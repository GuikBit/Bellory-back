package org.exemplo.bellory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.instancia.InstanceByNameDTO;
import org.exemplo.bellory.model.dto.instancia.InstanceCreateDTO;
import org.exemplo.bellory.model.dto.instancia.InstanceDTO;
import org.exemplo.bellory.model.dto.instancia.InstanceUpdateDTO;
import org.exemplo.bellory.model.dto.sendMessage.whatsapp.SendTextMessageDTO;
import org.exemplo.bellory.model.entity.instancia.*;
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

    @Value("${evolution.api.url:https://wa.bellory.com.br}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key:0626f19f09bd356cc21037164c7c3ca51752fef8}")
    private String evolutionApiKey;

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

    // ================================================================
    //  HEADERS — apikey como header (conforme Postman collection auth)
    // ================================================================

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", evolutionApiKey);
        return headers;
    }

    // ================================================================
    //  CREATE INSTANCE
    //  POST {{baseUrl}}/instance/create
    // ================================================================

    @Transactional
    public InstanceDTO createInstance(InstanceCreateDTO dto, boolean interno, long orgId) {

        Long organizacaoId = 0L;
        if (!interno) {
            organizacaoId = getOrganizacaoIdFromContext();
        }

        Organizacao organizacao = organizacaoRepository.findById(interno ? orgId : organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        if (instanceRepository.existsByInstanceName(dto.getInstanceName())) {
            throw new IllegalArgumentException("Já existe uma instância com este nome");
        }

        try {
            // ---- Montar body conforme Postman collection ----
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("instanceName", dto.getInstanceName());
            body.put("qrcode", true);
            body.put("integration", "WHATSAPP-BAILEYS");

            // number (opcional)
            if (dto.getInstanceNumber() != null && !dto.getInstanceNumber().isEmpty()) {
                body.put("number", dto.getInstanceNumber());
            }

            // settings inline no create (conforme Postman)
            body.put("rejectCall", dto.getRejectCall() != null ? dto.getRejectCall() : false);
            body.put("msgCall", dto.getMsgCall() != null ? dto.getMsgCall() : "");
            body.put("groupsIgnore", dto.getGroupsIgnore() != null ? dto.getGroupsIgnore() : false);
            body.put("alwaysOnline", dto.getAlwaysOnline() != null ? dto.getAlwaysOnline() : false);
            body.put("readMessages", dto.getReadMessages() != null ? dto.getReadMessages() : false);
            body.put("readStatus", dto.getReadStatus() != null ? dto.getReadStatus() : false);
            body.put("syncFullHistory", false);

            // webhook — objeto aninhado conforme Postman
            Map<String, Object> webhook = new LinkedHashMap<>();
            webhook.put("url", dto.getWebhookUrl() != null
                    ? dto.getWebhookUrl()
                    : "https://auto.bellory.com.br/webhook/whatsapp2");
            webhook.put("byEvents", false);
            webhook.put("base64", false);

            Map<String, String> webhookHeaders = new LinkedHashMap<>();
            webhookHeaders.put("autorization", "Bearer " + evolutionApiKey);  // "autorization" — typo da Evolution API
            webhookHeaders.put("Content-Type", "application/json");
            webhook.put("headers", webhookHeaders);

            List<String> events = dto.getWebhookEvents() != null && !dto.getWebhookEvents().isEmpty()
                    ? dto.getWebhookEvents()
                    : Arrays.asList("MESSAGES_UPSERT");
            webhook.put("events", events);

            body.put("webhook", webhook);

            // ---- Chamada à Evolution API ----
            String url = evolutionApiUrl + "/instance/create";
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            log.info("Evolution API /instance/create response: {}", response.getStatusCode());

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode instanceNode = jsonResponse.path("instance");

            // ---- Salvar no banco de dados ----
            Instance instance = new Instance();
            instance.setInstanceId(instanceNode.path("instanceId").asText());
            instance.setInstanceName(instanceNode.path("instanceName").asText());
            instance.setIntegration(instanceNode.path("integration").asText());
            instance.setOrganizacao(organizacao);
            instance.setDescription(dto.getDescription());
            instance.setPersonality(dto.getPersonality());


            // ✅ Status inicial = DISCONNECTED (aguardando QR code scan)
            instance.setStatus(InstanceStatus.DISCONNECTED);

            // Inicializar Settings com valores do DTO
            Settings settings = new Settings();
            settings.setRejectCall(dto.getRejectCall() != null ? dto.getRejectCall() : false);
            settings.setMsgCall(dto.getMsgCall());
            settings.setGroupsIgnore(dto.getGroupsIgnore() != null ? dto.getGroupsIgnore() : false);
            settings.setAlwaysOnline(dto.getAlwaysOnline() != null ? dto.getAlwaysOnline() : false);
            settings.setReadMessages(dto.getReadMessages() != null ? dto.getReadMessages() : false);
            settings.setReadStatus(dto.getReadStatus() != null ? dto.getReadStatus() : false);
            instance.setSettings(settings);

            // Inicializar WebhookConfig
            WebhookConfig webhookConfig = new WebhookConfig();
            webhookConfig.setUrl((String) webhook.get("url"));
            webhookConfig.setEnabled(dto.getWebhookEnabled() != null ? dto.getWebhookEnabled() : true);
            webhookConfig.setEvents(events);
            instance.setWebhookConfig(webhookConfig);

            Instance savedInstance = instanceRepository.save(instance);

            log.info("Instância criada com sucesso: {} (ID: {})", savedInstance.getInstanceName(), savedInstance.getId());

            return new InstanceDTO(savedInstance);

        } catch (Exception e) {
            log.error("Erro ao criar instância: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar instância no Evolution API: " + e.getMessage());
        }
    }

    // ================================================================
    //  GET ALL INSTANCES
    //  GET {{baseUrl}}/instance/fetchInstances
    // ================================================================

    @Transactional(readOnly = true)
    public List<InstanceDTO> getAllInstances() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        List<Instance> instances = instanceRepository.findByOrganizacaoIdWithRelations(organizacaoId);

        Map<String, JsonNode> evolutionDataMap = fetchAllEvolutionInstances();

        return instances.stream()
                .map(instance -> {
                    InstanceDTO dto = new InstanceDTO(instance);
                    JsonNode evolutionData = evolutionDataMap.get(instance.getInstanceName());
                    if (evolutionData != null) {
                        populateFromEvolutionData(dto, evolutionData);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Map<String, JsonNode> fetchAllEvolutionInstances() {
        try {
            String url = evolutionApiUrl + "/instance/fetchInstances";
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            Map<String, JsonNode> dataMap = new HashMap<>();
            if (jsonResponse.isArray()) {
                jsonResponse.forEach(node -> {
                    String name = node.path("name").asText("");
                    if (!name.isEmpty()) {
                        dataMap.put(name, node);
                    }
                });
            }

            log.info("Buscadas {} instâncias do Evolution API", dataMap.size());
            return dataMap;

        } catch (Exception e) {
            log.error("Erro ao buscar instâncias do Evolution API: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private void populateFromEvolutionData(InstanceDTO dto, JsonNode evolutionData) {
        try {
            dto.setInstanceId(evolutionData.path("id").asText(dto.getInstanceId()));
            dto.setInstanceName(evolutionData.path("name").asText(dto.getInstanceName()));
            dto.setIntegration(evolutionData.path("integration").asText(dto.getIntegration()));

            String connectionStatus = evolutionData.path("connectionStatus").asText("close");
            dto.setStatus(mapConnectionStatus(connectionStatus));

            // Phone number
            String ownerJid = evolutionData.path("ownerJid").asText(null);
            if (ownerJid != null && !ownerJid.isEmpty()) {
                dto.setPhoneNumber(ownerJid.split("@")[0]);
            }
            String number = evolutionData.path("number").asText(null);
            if (number != null && !number.isEmpty()) {
                dto.setPhoneNumber(number);
            }

            dto.setProfileName(evolutionData.path("profileName").asText(null));
            dto.setProfilePictureUrl(evolutionData.path("profilePicUrl").asText(null));
            dto.setToken(evolutionData.path("token").asText(null));
            dto.setClientName(evolutionData.path("clientName").asText(null));
            dto.setBusinessId(evolutionData.path("businessId").asText(null));

            dto.setDisconnectionAt(parseIsoDateTime(evolutionData.path("disconnectionAt").asText(null)));
            dto.setCreatedAt(parseIsoDateTime(evolutionData.path("createdAt").asText(null)));
            dto.setUpdatedAt(parseIsoDateTime(evolutionData.path("updatedAt").asText(null)));

            // Settings do Evolution (complementar ao banco)
            JsonNode settingsNode = evolutionData.path("Setting");
            if (!settingsNode.isMissingNode() && !settingsNode.isNull()) {
                InstanceDTO.Settings s = dto.getSettings() != null ? dto.getSettings() : new InstanceDTO.Settings();
                if (s.getRejectCall() == null) s.setRejectCall(settingsNode.path("rejectCall").asBoolean(false));
                if (s.getMsgCall() == null) s.setMsgCall(settingsNode.path("msgCall").asText(""));
                if (s.getGroupsIgnore() == null) s.setGroupsIgnore(settingsNode.path("groupsIgnore").asBoolean(false));
                if (s.getAlwaysOnline() == null) s.setAlwaysOnline(settingsNode.path("alwaysOnline").asBoolean(false));
                if (s.getReadMessages() == null) s.setReadMessages(settingsNode.path("readMessages").asBoolean(false));
                if (s.getReadStatus() == null) s.setReadStatus(settingsNode.path("readStatus").asBoolean(false));
                dto.setSettings(s);
            }

            // Counts
            JsonNode count = evolutionData.path("_count");
            if (!count.isMissingNode()) {
                dto.setMessageCount(count.path("Message").asInt(0));
                dto.setContactCount(count.path("Contact").asInt(0));
                dto.setChatCount(count.path("Chat").asInt(0));
            }

            InstanceStatus currentStatus = dto.getStatus();
            dto.setIsActive(currentStatus == InstanceStatus.CONNECTED || currentStatus == InstanceStatus.OPEN);
            dto.setQrcode(null);

        } catch (Exception e) {
            log.error("Erro ao preencher dados da Evolution API para {}: {}", dto.getInstanceName(), e.getMessage());
        }
    }

    // ================================================================
    //  GET INSTANCE BY ID
    // ================================================================

    @Transactional(readOnly = true)
    public InstanceDTO getInstanceById(Long id) {
        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());
        return new InstanceDTO(instance);
    }

    // ================================================================
    //  UPDATE INSTANCE
    //  Persiste no banco + sincroniza Settings e Webhook com Evolution
    // ================================================================

    @Transactional
    public InstanceDTO updateInstance(Long id, InstanceUpdateDTO dto) {
        log.info("Atualizando instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            // ---- Description / Personality ----
            if (dto.getDescription() != null) {
                instance.setDescription(dto.getDescription());
            }
            if (dto.getPersonality() != null) {
                instance.setPersonality(dto.getPersonality());
            }
            if(dto.getAtivo() != null){
                instance.setAtivo(dto.getAtivo());
            }

            // ---- Settings (banco) ----
            boolean settingsChanged = false;
            Settings settings = instance.getSettings();
            if (settings == null) {
                settings = new Settings();
                instance.setSettings(settings);
            }
            if (dto.getRejectCall() != null)   { settings.setRejectCall(dto.getRejectCall());     settingsChanged = true; }
            if (dto.getMsgCall() != null)       { settings.setMsgCall(dto.getMsgCall());           settingsChanged = true; }
            if (dto.getGroupsIgnore() != null)  { settings.setGroupsIgnore(dto.getGroupsIgnore());settingsChanged = true; }
            if (dto.getAlwaysOnline() != null)  { settings.setAlwaysOnline(dto.getAlwaysOnline());settingsChanged = true; }
            if (dto.getReadMessages() != null)  { settings.setReadMessages(dto.getReadMessages());settingsChanged = true; }
            if (dto.getReadStatus() != null)    { settings.setReadStatus(dto.getReadStatus());    settingsChanged = true; }

            // ---- Tools (banco apenas — não tem equivalente na Evolution API) ----
            if (dto.getTools() != null) {
                Tools tools = instance.getTools();
                if (tools == null) {
                    tools = new Tools();
                    instance.setTools(tools);
                }
                InstanceUpdateDTO.ToolsDTO t = dto.getTools();
                if (t.getGetServices() != null)          tools.setGetServices(t.getGetServices());
                if (t.getGetProfessional() != null)      tools.setGetProfessional(t.getGetProfessional());
                if (t.getGetProducts() != null)          tools.setGetProducts(t.getGetProducts());
                if (t.getGetAvaliableSchedules() != null)tools.setGetAvaliableSchedules(t.getGetAvaliableSchedules());
                if (t.getPostScheduling() != null)       tools.setPostScheduling(t.getPostScheduling());
                if (t.getSendTextMessage() != null)      tools.setSendTextMessage(t.getSendTextMessage());
                if (t.getSendMediaMessage() != null)     tools.setSendMediaMessage(t.getSendMediaMessage());
                if (t.getPostConfirmations() != null)    tools.setPostConfirmations(t.getPostConfirmations());
                if (t.getPostCancellations() != null)    tools.setPostCancellations(t.getPostCancellations());

            }

            // ---- Webhook (banco) ----
            boolean webhookChanged = false;
            if (dto.getWebhookUrl() != null || dto.getWebhookEnabled() != null || dto.getWebhookEvents() != null) {
                WebhookConfig wc = instance.getWebhookConfig();
                if (wc == null) {
                    wc = new WebhookConfig();
                    instance.setWebhookConfig(wc);
                }
                if (dto.getWebhookUrl() != null)     { wc.setUrl(dto.getWebhookUrl());         webhookChanged = true; }
                if (dto.getWebhookEnabled() != null)  { wc.setEnabled(dto.getWebhookEnabled()); webhookChanged = true; }
                if (dto.getWebhookEvents() != null)   { wc.setEvents(dto.getWebhookEvents());   webhookChanged = true; }
            }

            // ---- Salvar no banco ----
            Instance saved = instanceRepository.save(instance);

            // ---- Sincronizar com Evolution API ----
            if (settingsChanged) {
                syncSettingsToEvolution(saved);
            }
            if (webhookChanged) {
                syncWebhookToEvolution(saved);
            }

            log.info("Instância atualizada com sucesso: {}", saved.getInstanceName());
            return new InstanceDTO(saved);

        } catch (Exception e) {
            log.error("Erro ao atualizar instância: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao atualizar instância: " + e.getMessage());
        }
    }

    // ================================================================
    //  SYNC SETTINGS → Evolution API
    //  POST {{baseUrl}}/settings/set/{{instance}}
    //
    //  Body (FLAT — conforme Postman collection):
    //  {
    //      "rejectCall": true,
    //      "msgCall": "I do not accept calls",
    //      "groupsIgnore": false,
    //      "alwaysOnline": true,
    //      "readMessages": false,
    //      "syncFullHistory": false,
    //      "readStatus": false
    //  }
    // ================================================================

    private void syncSettingsToEvolution(Instance instance) {
        try {
            String url = evolutionApiUrl + "/settings/set/" + instance.getInstanceName();

            Settings s = instance.getSettings();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("rejectCall",      s.getRejectCall()   != null ? s.getRejectCall()   : false);
            body.put("msgCall",         s.getMsgCall()      != null ? s.getMsgCall()       : "");
            body.put("groupsIgnore",    s.getGroupsIgnore() != null ? s.getGroupsIgnore() : false);
            body.put("alwaysOnline",    s.getAlwaysOnline() != null ? s.getAlwaysOnline() : false);
            body.put("readMessages",    s.getReadMessages() != null ? s.getReadMessages() : false);
            body.put("syncFullHistory", false);
            body.put("readStatus",      s.getReadStatus()   != null ? s.getReadStatus()   : false);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            log.info("Evolution /settings/set/{} → {} | Body: {}",
                    instance.getInstanceName(), response.getStatusCode(), body);

        } catch (Exception e) {
            log.error("Falha ao sincronizar settings com Evolution para {}: {}",
                    instance.getInstanceName(), e.getMessage());
        }
    }

    // ================================================================
    //  SYNC WEBHOOK → Evolution API
    //  POST {{baseUrl}}/webhook/set/{{instance}}
    //
    //  Body (ANINHADO em "webhook" — conforme Postman collection):
    //  {
    //      "webhook": {
    //          "enabled": true,
    //          "url": "https://webhook.site",
    //          "headers": {
    //              "autorization": "Bearer TOKEN",   ← typo da Evolution API
    //              "Content-Type": "application/json"
    //          },
    //          "byEvents": false,
    //          "base64": false,
    //          "events": ["MESSAGES_UPSERT", ...]
    //      }
    //  }
    // ================================================================

    private void syncWebhookToEvolution(Instance instance) {
        WebhookConfig wc = instance.getWebhookConfig();
        if (wc == null || wc.getUrl() == null || wc.getUrl().isEmpty()) {
            log.debug("Webhook não configurado para {}, pulando sincronização", instance.getInstanceName());
            return;
        }

        try {
            String url = evolutionApiUrl + "/webhook/set/" + instance.getInstanceName();

            // Objeto interno "webhook" — conforme Postman collection
            Map<String, Object> webhookInner = new LinkedHashMap<>();
            webhookInner.put("enabled", wc.getEnabled() != null ? wc.getEnabled() : true);
            webhookInner.put("url", wc.getUrl());

            Map<String, String> hdrs = new LinkedHashMap<>();
            hdrs.put("autorization", "Bearer " + evolutionApiKey);  // "autorization" — typo da Evolution API
            hdrs.put("Content-Type", "application/json");
            webhookInner.put("headers", hdrs);

            webhookInner.put("byEvents", false);
            webhookInner.put("base64", false);

            if (wc.getEvents() != null && !wc.getEvents().isEmpty()) {
                webhookInner.put("events", wc.getEvents());
            } else {
                webhookInner.put("events", Arrays.asList("MESSAGES_UPSERT"));
            }

            // Body raiz com chave "webhook"
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("webhook", webhookInner);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            log.info("Evolution /webhook/set/{} → {} | Events: {}",
                    instance.getInstanceName(), response.getStatusCode(),
                    wc.getEvents() != null ? wc.getEvents().size() : 0);

        } catch (Exception e) {
            log.error("Falha ao sincronizar webhook com Evolution para {}: {}",
                    instance.getInstanceName(), e.getMessage());
        }
    }

    // ================================================================
    //  DELETE INSTANCE
    //  DELETE {{baseUrl}}/instance/delete/{{instance}}
    // ================================================================

    @Transactional
    public void deleteInstance(Long id) {
        log.info("Deletando instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        instance.setAtivo(false);
        instance.setDeletado(true);
        instance.setStatus(InstanceStatus.DISCONNECTED);

        instanceRepository.save(instance);

        try {
            String url = evolutionApiUrl + "/instance/delete/" + instance.getInstanceName();
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

            log.info("Evolution /instance/delete/{} → OK", instance.getInstanceName());

        } catch (Exception e) {
            log.warn("Falha ao deletar na Evolution API (prosseguindo com remoção local): {}", e.getMessage());
        }

        instanceRepository.delete(instance);
        log.info("Instância deletada do banco: {}", instance.getInstanceName());
    }

    // ================================================================
    //  QR CODE
    //  GET {{baseUrl}}/instance/connect/{{instance}}
    // ================================================================

    public Map<String, String> getQRCode(Long id) {
        log.info("Obtendo QR Code para instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            String url = evolutionApiUrl + "/instance/connect/" + instance.getInstanceName();

            HttpHeaders headers = createHeaders();
            headers.set("Accept", "application/json");  // conforme Postman collection

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            String base64 = jsonResponse.has("base64") ? jsonResponse.get("base64").asText() : null;

            Map<String, String> result = new HashMap<>();
            result.put("base64", base64);
            result.put("instanceName", instance.getInstanceName());
            return result;

        } catch (Exception e) {
            log.error("Erro ao obter QR Code: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao obter QR Code: " + e.getMessage());
        }
    }

    // ================================================================
    //  CONNECTION STATUS
    //  GET {{baseUrl}}/instance/connectionState/{{instance}}
    //
    //  Retorna: { "instance": {...}, "state": "open" | "close" | "connecting" }
    //  ✅ Persiste o status no banco de dados
    // ================================================================

    @Transactional
    public Map<String, Object> getConnectionStatus(Long id) {
        log.info("Obtendo status de conexão para instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            String url = evolutionApiUrl + "/instance/connectionState/" + instance.getInstanceName();
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            String state = jsonResponse.has("state") ? jsonResponse.get("state").asText() : "close";
            InstanceStatus newStatus = mapConnectionStatus(state);

            // Atualizar no banco se mudou
            if (instance.getStatus() != newStatus) {
                log.info("Status mudou para {}: {} → {}", instance.getInstanceName(), instance.getStatus(), newStatus);
                instance.setStatus(newStatus);
                instanceRepository.save(instance);
            }

            return objectMapper.convertValue(jsonResponse, Map.class);

        } catch (Exception e) {
            log.error("Erro ao obter status de conexão: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao obter status de conexão: " + e.getMessage());
        }
    }

    // ================================================================
    //  LOGOUT
    //  DELETE {{baseUrl}}/instance/logout/{{instance}}
    // ================================================================

    @Transactional
    public void logout(Long id) {
        log.info("Desconectando instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            String url = evolutionApiUrl + "/instance/logout/" + instance.getInstanceName();
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

            log.info("Evolution /instance/logout/{} → OK", instance.getInstanceName());

        } catch (Exception e) {
            log.warn("Falha ao deslogar na Evolution API: {}", e.getMessage());
        }

        instance.setStatus(InstanceStatus.DISCONNECTED);
        instanceRepository.save(instance);
    }

    // ================================================================
    //  RESTART
    //  POST {{baseUrl}}/instance/restart/{{instance}}   ← POST, não PUT!
    // ================================================================

    @Transactional
    public void restart(Long id) {
        log.info("Reiniciando instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        try {
            String url = evolutionApiUrl + "/instance/restart/" + instance.getInstanceName();
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());

            // POST (não PUT) — conforme Postman collection
            restTemplate.postForEntity(url, request, String.class);

            log.info("Evolution /instance/restart/{} → OK", instance.getInstanceName());

            instance.setStatus(InstanceStatus.CONNECTING);
            instanceRepository.save(instance);

        } catch (Exception e) {
            log.error("Erro ao reiniciar instância: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao reiniciar instância: " + e.getMessage());
        }
    }

    // ================================================================
    //  SEND TEXT MESSAGE
    //  POST {{baseUrl}}/message/sendText/{{instance}}
    // ================================================================

    public Map<String, Object> sendTextMessage(Long id, SendTextMessageDTO dto) {
        log.info("Enviando mensagem pela instância ID: {}", id);

        Instance instance = findInstanceById(id);
        validarOrganizacao(instance.getOrganizacao().getId());

        if (instance.getStatus() != null
                && instance.getStatus() != InstanceStatus.OPEN
                && instance.getStatus() != InstanceStatus.CONNECTED) {
            throw new IllegalStateException("Instância não conectada. Status: " + instance.getStatus());
        }

        try {
            String url = evolutionApiUrl + "/message/sendText/" + instance.getInstanceName();

            Map<String, Object> body = new HashMap<>();
            body.put("number", dto.getNumber());
            body.put("text", dto.getText());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, createHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            return objectMapper.convertValue(objectMapper.readTree(response.getBody()), Map.class);

        } catch (Exception e) {
            log.error("Erro ao enviar mensagem: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    // ================================================================
    //  GET INSTANCE BY NAME (custom endpoint)
    // ================================================================

    @Transactional(readOnly = true)
    public InstanceByNameDTO getInstanceByNameCustom(String instanceName) {
        log.info("Buscando instância pelo nome: {}", instanceName);

        Instance instance = instanceRepository.findByInstanceNameWithRelations(instanceName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Instância não encontrada com o nome: " + instanceName));

        InstanceByNameDTO dto = new InstanceByNameDTO(instance);

        try {
            String url = evolutionApiUrl + "/instance/fetchInstances?instanceName=" + instanceName;
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            String phoneNumber = null, profileName = null, profilePictureUrl = null;

            if (jsonResponse.isArray() && jsonResponse.size() > 0) {
                JsonNode instNode = jsonResponse.get(0).path("instance");
                phoneNumber = instNode.path("owner").asText(null);
                profileName = instNode.path("profileName").asText(null);
                profilePictureUrl = instNode.path("profilePictureUrl").asText(null);
            }

            dto.updateFromEvolutionData(phoneNumber, profileName, profilePictureUrl);

        } catch (Exception e) {
            log.warn("Não foi possível buscar dados Evolution para {}: {}", instanceName, e.getMessage());
        }

        return dto;
    }

    // ================================================================
    //  UTILITÁRIOS
    // ================================================================

    private InstanceStatus mapConnectionStatus(String status) {
        if (status == null) return InstanceStatus.DISCONNECTED;
        return switch (status.toLowerCase()) {
            case "open" -> InstanceStatus.CONNECTED;
            case "connecting" -> InstanceStatus.CONNECTING;
            case "close", "closed" -> InstanceStatus.DISCONNECTED;
            default -> InstanceStatus.DISCONNECTED;
        };
    }

    private LocalDateTime parseIsoDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            if (dateTimeStr.contains(".")) {
                dateTimeStr = dateTimeStr.substring(0, dateTimeStr.indexOf('.'));
            } else if (dateTimeStr.endsWith("Z")) {
                dateTimeStr = dateTimeStr.substring(0, dateTimeStr.length() - 1);
            }
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            return null;
        }
    }

    private Instance findInstanceById(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada com ID: " + id));
    }

    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }
        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }
        return organizacaoId;
    }
}
