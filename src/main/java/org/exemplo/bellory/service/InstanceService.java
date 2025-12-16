package org.exemplo.bellory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.instancia.InstanceCreateDTO;
import org.exemplo.bellory.model.dto.instancia.InstanceDTO;
import org.exemplo.bellory.model.dto.instancia.InstanceUpdateDTO;
import org.exemplo.bellory.model.dto.sendMessage.whatsapp.SendTextMessageDTO;
import org.exemplo.bellory.model.entity.instancia.Instance;
import org.exemplo.bellory.model.entity.instancia.InstanceStatus;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.instance.InstanceRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Service
@Slf4j
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String evolutionApiUrl = "https://wa.bellory.com.br";

    private String evolutionApiKey = "0626f19f09bd356cc21037164c7c3ca51752fef8";

    public InstanceService(
            InstanceRepository instanceRepository,
            OrganizacaoRepository organizacaoRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.instanceRepository = instanceRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Criar nova instância no Evolution API e salvar no banco
     */
    @Transactional
    public InstanceDTO createInstance(InstanceCreateDTO dto) {

        // Validar organização do contexto
        Long organizacaoId = getOrganizacaoIdFromContext();
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        // Verificar se já existe instância com esse nome
        if (instanceRepository.existsByInstanceName(dto.getInstanceName())) {
            throw new IllegalArgumentException("Já existe uma instância com este nome");
        }

        try {
            // 1. Criar instância no Evolution API
            String url = evolutionApiUrl + "/instance/create";
            HttpHeaders headers = createHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("instanceName", dto.getInstanceName());
            requestBody.put("qrcode", true);
            requestBody.put("integration", "WHATSAPP-BAILEYS");
            requestBody.put("number", dto.getInstanceNumber().toString());
            requestBody.put("alwaysOnline", false);
            requestBody.put("readMessages", false);
            requestBody.put("syncFullHistory", false);
            requestBody.put("readStatus", false);
            requestBody.put("groupsIgnore", false);
            requestBody.put("rejectCall", true);


            Map<String, Object> webhook = new HashMap<>();
            webhook.put("url", "https://auto.bellory.com.br/webhook/webhook/whatsapp"); // URL do seu endpoint
            webhook.put("byEvents", false);
            webhook.put("base64", false);

            Map<String, String> webhookHeaders = new HashMap<>();
            webhookHeaders.put("authorization", "Bearer 0626f19f09bd356cc21037164c7c3ca51752fef8");
            webhookHeaders.put("Content-Type", "application/json");
            webhook.put("headers", webhookHeaders);

            List<String> events = Arrays.asList(
                    "MESSAGES_UPSERT"
            );
            webhook.put("events", events);

            requestBody.put("webhook", webhook);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            // Parsear resposta
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            JsonNode instanceNode = jsonResponse.path("instance");

            // 2. Criar entidade no banco de dados
            Instance instance = new Instance();
            instance.setInstanceId(instanceNode.path("instanceId").asText());
            instance.setInstanceName(instanceNode.path("instanceName").asText());
            instance.setIntegration(instanceNode.path("integration").asText());
            instance.setOrganizacao(organizacao);
            instance.setDescription(dto.getDescription());
            instance.setPersonality(dto.getPersonality());

            Instance savedInstance = instanceRepository.save(instance);

            return new InstanceDTO(savedInstance);

        } catch (Exception e) {
            log.error("Erro ao criar instância: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar instância no Evolution API: " + e.getMessage());
        }
    }

    /**
     * Listar todas as instâncias da organização
     */
    /**
     * Listar todas as instâncias da organização com dados do Evolution API
     */
    @Transactional(readOnly = true)
    public List<InstanceDTO> getAllInstances() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        // Buscar instâncias com EAGER fetch para evitar lazy loading
        List<Instance> instances = instanceRepository.findByOrganizacaoIdWithRelations(organizacaoId);

        // Buscar TODAS as instâncias do Evolution API de uma vez
        Map<String, JsonNode> evolutionDataMap = fetchAllEvolutionInstances();

        return instances.stream()
                .map(instance -> {
                    InstanceDTO dto = new InstanceDTO(instance);

                    // Buscar dados do Evolution API pelo instanceName
                    JsonNode evolutionData = evolutionDataMap.get(instance.getInstanceName());
                    if (evolutionData != null) {
                        populateFromEvolutionData(dto, evolutionData);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Busca todas as instâncias do Evolution API de uma vez
     */
    private Map<String, JsonNode> fetchAllEvolutionInstances() {
        try {
            String url = evolutionApiUrl + "/instance/fetchInstances";
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class
            );

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            Map<String, JsonNode> dataMap = new HashMap<>();

            // Verificar se a resposta é um array
            if (jsonResponse.isArray()) {
                jsonResponse.forEach(node -> {
                    String instanceName = node.path("name").asText();
//                    String instanceName = instanceNode.path("instanceName").asText();

                    if (!instanceName.isEmpty()) {
                        dataMap.put(instanceName, node);
                    }
                });
            } else if (!jsonResponse.isMissingNode()) {
                // Se for um objeto único
                JsonNode instanceNode = jsonResponse.path("instance");
                String instanceName = instanceNode.path("instanceName").asText();

                if (!instanceName.isEmpty()) {
                    dataMap.put(instanceName, jsonResponse);
                }
            }

            log.info("Buscadas {} instâncias do Evolution API", dataMap.size());
            return dataMap;

        } catch (Exception e) {
            log.error("Erro ao buscar todas as instâncias do Evolution API: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private void populateFromEvolutionData(InstanceDTO dto, JsonNode evolutionData) {
        try {
            // Campos principais da instância
            dto.setInstanceId(evolutionData.path("id").asText(dto.getInstanceId()));
            dto.setInstanceName(evolutionData.path("name").asText(dto.getInstanceName()));
            dto.setIntegration(evolutionData.path("integration").asText(dto.getIntegration()));

            // Status da conexão
            String connectionStatus = evolutionData.path("connectionStatus").asText("close");
            dto.setStatus(mapConnectionStatus(connectionStatus));

            // Dados do perfil do WhatsApp
            String ownerJid = evolutionData.path("ownerJid").asText(null);
            if (ownerJid != null && !ownerJid.isEmpty()) {
                String phoneNumber = ownerJid.split("@")[0];
                dto.setPhoneNumber(phoneNumber);
            }

            // Número formatado (sobrescreve se existir)
            String number = evolutionData.path("number").asText(null);
            if (number != null && !number.isEmpty()) {
                dto.setPhoneNumber(number);
            }

            dto.setProfileName(evolutionData.path("profileName").asText(null));
            dto.setProfilePictureUrl(evolutionData.path("profilePicUrl").asText(null));

            // Campos adicionais
            dto.setToken(evolutionData.path("token").asText(null));
            dto.setClientName(evolutionData.path("clientName").asText(null));
            dto.setBusinessId(evolutionData.path("businessId").asText(null));

            // Parsing de datas ISO 8601
            dto.setDisconnectionAt(parseIsoDateTime(evolutionData.path("disconnectionAt").asText(null)));
            dto.setCreatedAt(parseIsoDateTime(evolutionData.path("createdAt").asText(null)));
            dto.setUpdatedAt(parseIsoDateTime(evolutionData.path("updatedAt").asText(null)));

            // Configurações (Setting object)
            JsonNode settings = evolutionData.path("Setting");
//            if (!settings.isMissingNode() && !settings.isNull()) {
//                dto.setRejectCall(settings.path("rejectCall").asBoolean(false));
//                dto.setMsgCall(settings.path("msgCall").asText(""));
//                dto.setGroupsIgnore(settings.path("groupsIgnore").asBoolean(false));
//                dto.setAlwaysOnline(settings.path("alwaysOnline").asBoolean(false));
//                dto.setReadMessages(settings.path("readMessages").asBoolean(false));
//                dto.setReadStatus(settings.path("readStatus").asBoolean(false));
//            }

            // Contadores (_count)
            JsonNode count = evolutionData.path("_count");
            if (!count.isMissingNode() && !count.isNull()) {
                dto.setMessageCount(count.path("Message").asInt(0));
                dto.setContactCount(count.path("Contact").asInt(0));
                dto.setChatCount(count.path("Chat").asInt(0));
            }

            // Determinar se está ativa baseado no status
            InstanceStatus currentStatus = dto.getStatus();
            dto.setIsActive(currentStatus == InstanceStatus.CONNECTED ||
                    currentStatus == InstanceStatus.OPEN);

            // QR Code será buscado APENAS quando solicitado individualmente
            // Não buscar aqui para evitar N+1 queries
            dto.setQrcode(null);

        } catch (Exception e) {
            log.error("Erro ao preencher dados da Evolution API para instância {}: {}",
                    dto.getInstanceName(), e.getMessage(), e);
        }
    }

    /**
     * Parse de datas ISO 8601 com timezone
     */
    private LocalDateTime parseIsoDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }

        try {
            // Remover timezone e milissegundos extras para parsing
            // De: "2025-12-05T17:12:24.681Z" para "2025-12-05T17:12:24"
            if (dateTimeStr.contains(".")) {
                dateTimeStr = dateTimeStr.substring(0, dateTimeStr.indexOf('.'));
            } else if (dateTimeStr.endsWith("Z")) {
                dateTimeStr = dateTimeStr.substring(0, dateTimeStr.length() - 1);
            }

            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            log.debug("Erro ao parsear data/hora: {} - {}", dateTimeStr, e.getMessage());
            return null;
        }
    }

    private InstanceStatus mapConnectionStatus(String connectionStatus) {
        if (connectionStatus == null) {
            return InstanceStatus.DISCONNECTED;
        }

        return switch (connectionStatus.toLowerCase()) {
            case "open" -> InstanceStatus.OPEN;
            case "connecting" -> InstanceStatus.CONNECTING;
            case "close", "closed" -> InstanceStatus.DISCONNECTED;
            default -> InstanceStatus.ERROR;
        };
    }
    /**
     * Buscar instância por ID
     */
    @Transactional(readOnly = true)
    public InstanceDTO getInstanceById(Long id) {
        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());
        return new InstanceDTO(instance);
    }

    /**
     * Atualizar instância
     */
    @Transactional
    public InstanceDTO updateInstance(Long id, InstanceUpdateDTO dto) {
        log.info("Atualizando instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            // Atualizar campos locais
//            if (dto.getWebhookUrl() != null) instance.setWebhookUrl(dto.getWebhookUrl());
//            if (dto.getWebhookEnabled() != null) instance.setWebhookEnabled(dto.getWebhookEnabled());
//            if (dto.getRejectCall() != null) instance.setRejectCall(dto.getRejectCall());
//            if (dto.getMsgCall() != null) instance.setMsgCall(dto.getMsgCall());
//            if (dto.getGroupsIgnore() != null) instance.setGroupsIgnore(dto.getGroupsIgnore());
//            if (dto.getAlwaysOnline() != null) instance.setAlwaysOnline(dto.getAlwaysOnline());
//            if (dto.getReadMessages() != null) instance.setReadMessages(dto.getReadMessages());
//            if (dto.getReadStatus() != null) instance.setReadStatus(dto.getReadStatus());
//            if (dto.getIsActive() != null) instance.setIsActive(dto.getIsActive());
            if (dto.getDescription() != null) instance.setDescription(dto.getDescription());

            if (dto.getWebhookEvents() != null) {
//                instance.setWebhookEvents(dto.getWebhookEvents().toString());
            }

            // Atualizar settings no Evolution API
            configureSettings(instance);

            // Atualizar webhook no Evolution API
            if (dto.getWebhookUrl() != null || dto.getWebhookEnabled() != null) {
                configureWebhook(instance);
            }

            Instance updatedInstance = instanceRepository.save(instance);
            log.info("Instância atualizada com sucesso: {}", updatedInstance.getInstanceName());

            return new InstanceDTO(updatedInstance);

        } catch (Exception e) {
            log.error("Erro ao atualizar instância: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao atualizar instância: " + e.getMessage());
        }
    }

    /**
     * Deletar instância
     */
    @Transactional
    public void deleteInstance(Long id) {
        log.info("Deletando instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            // Deletar do Evolution API
            String url = evolutionApiUrl + "/instance/delete/" + instance.getInstanceName();
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

            // Deletar do banco de dados
            instanceRepository.delete(instance);

            log.info("Instância deletada com sucesso: {}", instance.getInstanceName());

        } catch (Exception e) {
            log.error("Erro ao deletar instância: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao deletar instância: " + e.getMessage());
        }
    }

    /**
     * Obter QR Code para conectar WhatsApp
     */
    public Map<String, String> getQRCode(Long id) {
        log.info("Obtendo QR Code para instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            String url = evolutionApiUrl + "/instance/connect/" + instance.getInstanceName();
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            String base64 = jsonResponse.has("base64")
                    ? jsonResponse.get("base64").asText()
                    : null;



            Map<String, String> result = new HashMap<>();
            result.put("base64", base64);
            result.put("instanceName", instance.getInstanceName());

            return result;

        } catch (Exception e) {
            log.error("Erro ao obter QR Code: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao obter QR Code: " + e.getMessage());
        }
    }

    /**
     * Obter status de conexão
     */
    public Map<String, Object> getConnectionStatus(Long id) {
        log.info("Obtendo status de conexão para instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            String url = evolutionApiUrl + "/instance/connectionState/" + instance.getInstanceName();
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            // Atualizar status no banco
            String state = jsonResponse.has("state") ? jsonResponse.get("state").asText() : "disconnected";


            if (jsonResponse.has("instance")) {
                JsonNode instanceNode = jsonResponse.get("instance");
            }

            instanceRepository.save(instance);

            return objectMapper.convertValue(jsonResponse, Map.class);

        } catch (Exception e) {
            log.error("Erro ao obter status de conexão: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao obter status de conexão: " + e.getMessage());
        }
    }

    /**
     * Desconectar instância (logout)
     */
    @Transactional
    public void logout(Long id) {
        log.info("Desconectando instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            String url = evolutionApiUrl + "/instance/logout/" + instance.getInstanceName();
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

            // Atualizar status no banco
//            instance.setStatus(InstanceStatus.DISCONNECTED);
//            instance.setQrcode(null);
//            instance.setPhoneNumber(null);
//            instance.setProfileName(null);
//            instance.setProfilePictureUrl(null);

            instanceRepository.save(instance);

            log.info("Instância desconectada com sucesso: {}", instance.getInstanceName());

        } catch (Exception e) {
            log.error("Erro ao desconectar instância: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao desconectar instância: " + e.getMessage());
        }
    }

    /**
     * Reiniciar instância
     */
    public void restart(Long id) {
        log.info("Reiniciando instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            String url = evolutionApiUrl + "/instance/restart/" + instance.getInstanceName();
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

            log.info("Instância reiniciada com sucesso: {}", instance.getInstanceName());

        } catch (Exception e) {
            log.error("Erro ao reiniciar instância: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao reiniciar instância: " + e.getMessage());
        }
    }

    /**
     * Enviar mensagem de texto
     */
    public Map<String, Object> sendTextMessage(Long id, SendTextMessageDTO dto) {
        log.info("Enviando mensagem de texto pela instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

//        if (!instance.getStatus().equals(InstanceStatus.OPEN) &&
//                !instance.getStatus().equals(InstanceStatus.CONNECTED)) {
//            throw new IllegalStateException("A instância não está conectada");
//        }

        try {
            String url = evolutionApiUrl + "/message/sendText/" + instance.getInstanceName();
            HttpHeaders headers = createHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("number", dto.getNumber());
            requestBody.put("text", dto.getText());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return objectMapper.convertValue(jsonResponse, Map.class);

        } catch (Exception e) {
            log.error("Erro ao enviar mensagem: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Configurar settings da instância no Evolution API
     */
    private void configureSettings(Instance instance) {
        try {
            String url = evolutionApiUrl + "/instance/settings/" + instance.getInstanceName();
            HttpHeaders headers = createHeaders();

            Map<String, Object> settings = new HashMap<>();
//            settings.put("reject_call", instance.getRejectCall());
//            settings.put("msg_call", instance.getMsgCall());
//            settings.put("groups_ignore", instance.getGroupsIgnore());
//            settings.put("always_online", instance.getAlwaysOnline());
//            settings.put("read_messages", instance.getReadMessages());
//            settings.put("read_status", instance.getReadStatus());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(settings, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.debug("Settings configurados para instância: {}", instance.getInstanceName());

        } catch (Exception e) {
            log.error("Erro ao configurar settings: {}", e.getMessage());
        }
    }

    /**
     * Configurar webhook da instância no Evolution API
     */
    private void configureWebhook(Instance instance) {
//        if (instance.getWebhookUrl() == null || instance.getWebhookUrl().isEmpty()) {
//            return;
//        }

        try {
            String url = evolutionApiUrl + "/webhook/set/" + instance.getInstanceName();
            HttpHeaders headers = createHeaders();

            Map<String, Object> webhook = new HashMap<>();

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(webhook, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.debug("Webhook configurado para instância: {}", instance.getInstanceName());

        } catch (Exception e) {
            log.error("Erro ao configurar webhook: {}", e.getMessage());
        }
    }

    /**
     * Criar headers para requisição na Evolution API
     * IMPORTANTE: A Evolution API requer o header 'apikey' em todas as requisições
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", evolutionApiKey); // Header obrigatório para Evolution API

        log.debug("Headers criados com apikey: {}",
                evolutionApiKey != null ? "***" + evolutionApiKey.substring(Math.max(0, evolutionApiKey.length() - 4)) : "null");

        return headers;
    }

    /**
     * Mapear status retornado pela Evolution API para enum local
     */
    private InstanceStatus mapStatusFromEvolution(String status) {
        return switch (status.toLowerCase()) {
            case "open", "connected" -> InstanceStatus.CONNECTED;
            case "connecting" -> InstanceStatus.CONNECTING;
            default -> InstanceStatus.DISCONNECTED;
        };
    }

    /**
     * Buscar instância por ID com validação
     */
    private Instance findInstanceById(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada com ID: " + id));
    }

    /**
     * Validar se a entidade pertence à organização do usuário
     */
    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }

        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }
    }

    /**
     * Obter ID da organização do contexto
     */
    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }

        return organizacaoId;
    }
}
