package org.exemplo.bellory.service.push;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.push.NotificacaoPushDTO;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.push.CategoriaNotificacao;
import org.exemplo.bellory.model.entity.push.NotificacaoPush;
import org.exemplo.bellory.model.entity.push.PrioridadeNotificacao;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.push.NotificacaoPushRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificacaoPushService {

    private final NotificacaoPushRepository notificacaoPushRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PushNotificationService pushNotificationService;

    public NotificacaoPushService(NotificacaoPushRepository notificacaoPushRepository,
                                  OrganizacaoRepository organizacaoRepository,
                                  PushNotificationService pushNotificationService) {
        this.notificacaoPushRepository = notificacaoPushRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.pushNotificationService = pushNotificationService;
    }

    public Page<NotificacaoPushDTO> listarNotificacoes(Pageable pageable) {
        Long userId = TenantContext.getCurrentUserId();
        String userRole = TenantContext.getCurrentRole();
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        return notificacaoPushRepository
                .findAllByUserIdAndUserRoleAndOrganizacao_IdOrderByDtCadastroDesc(
                        userId, userRole, organizacaoId, pageable)
                .map(NotificacaoPushDTO::new);
    }

    @Transactional
    public NotificacaoPushDTO marcarComoLida(Long id) {
        NotificacaoPush notificacao = notificacaoPushRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacao nao encontrada"));

        validarAcesso(notificacao);

        notificacao.setLido(true);
        notificacao.setDtRead(LocalDateTime.now());
        notificacaoPushRepository.save(notificacao);

        return new NotificacaoPushDTO(notificacao);
    }

    @Transactional
    public void deletar(Long id) {
        NotificacaoPush notificacao = notificacaoPushRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacao nao encontrada"));

        validarAcesso(notificacao);

        notificacaoPushRepository.delete(notificacao);
    }

    public long contarNaoLidas() {
        Long userId = TenantContext.getCurrentUserId();
        String userRole = TenantContext.getCurrentRole();
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        return notificacaoPushRepository
                .countByUserIdAndUserRoleAndOrganizacao_IdAndLidoFalse(userId, userRole, organizacaoId);
    }

    @Transactional
    public NotificacaoPush criarEEnviar(Long userId, String userRole, Long organizacaoId,
                                         String titulo, String descricao, String origem,
                                         CategoriaNotificacao categoria, PrioridadeNotificacao prioridade,
                                         String icone, String urlAcao) {

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organizacao nao encontrada"));

        NotificacaoPush notificacao = new NotificacaoPush();
        notificacao.setUserId(userId);
        notificacao.setUserRole(userRole);
        notificacao.setOrganizacao(organizacao);
        notificacao.setTitulo(titulo);
        notificacao.setDescricao(descricao);
        notificacao.setOrigem(origem);
        notificacao.setCategoria(categoria);
        notificacao.setPrioridade(prioridade);
        notificacao.setIcone(icone);
        notificacao.setUrlAcao(urlAcao);

        NotificacaoPush saved = notificacaoPushRepository.save(notificacao);

        // Dispara push assincrono
        pushNotificationService.sendToUser(userId, userRole, organizacaoId, saved);

        return saved;
    }

    @Transactional
    public void criarEEnviarParaRole(String role, Long organizacaoId,
                                      String titulo, String descricao, String origem,
                                      CategoriaNotificacao categoria, PrioridadeNotificacao prioridade,
                                      String icone, String urlAcao) {

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organizacao nao encontrada"));

        // Cria a notificacao-modelo para o push payload
        NotificacaoPush notificacaoModelo = new NotificacaoPush();
        notificacaoModelo.setOrganizacao(organizacao);
        notificacaoModelo.setTitulo(titulo);
        notificacaoModelo.setDescricao(descricao);
        notificacaoModelo.setOrigem(origem);
        notificacaoModelo.setCategoria(categoria);
        notificacaoModelo.setPrioridade(prioridade);
        notificacaoModelo.setIcone(icone);
        notificacaoModelo.setUrlAcao(urlAcao);

        // Dispara push assincrono para todos com a role
        pushNotificationService.sendToRole(role, organizacaoId, notificacaoModelo);
    }

    private void validarAcesso(NotificacaoPush notificacao) {
        Long userId = TenantContext.getCurrentUserId();
        String userRole = TenantContext.getCurrentRole();
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (!notificacao.getUserId().equals(userId)
                || !notificacao.getUserRole().equals(userRole)
                || !notificacao.getOrganizacao().getId().equals(organizacaoId)) {
            throw new SecurityException("Acesso negado a esta notificacao");
        }
    }
}
