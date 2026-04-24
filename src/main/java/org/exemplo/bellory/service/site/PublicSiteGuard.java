package org.exemplo.bellory.service.site;

import org.exemplo.bellory.model.dto.site.ModoSite;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.entity.site.SitePublicoConfig;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.exemplo.bellory.model.repository.site.SitePublicoConfigRepository;
import org.exemplo.bellory.service.plano.LimiteValidatorService;
import org.exemplo.bellory.service.plano.LimiteValidatorService.TipoLimite;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Centraliza a regra de acesso ao site público:
 *   - NOT_FOUND   → organizacao inexistente ou desativada (404)
 *   - INACTIVE    → organizacao ok, mas SitePublicoConfig.active = false (200 + siteAtivo=false)
 *   - ACTIVE      → libera o fluxo, com ModoSite COMPLETO ou BASICO derivado do plano
 */
@Component
public class PublicSiteGuard {

    private final OrganizacaoRepository organizacaoRepository;
    private final SitePublicoConfigRepository siteConfigRepository;
    private final LimiteValidatorService limiteValidator;

    public PublicSiteGuard(OrganizacaoRepository organizacaoRepository,
                           SitePublicoConfigRepository siteConfigRepository,
                           LimiteValidatorService limiteValidator) {
        this.organizacaoRepository = organizacaoRepository;
        this.siteConfigRepository = siteConfigRepository;
        this.limiteValidator = limiteValidator;
    }

    public enum Status {
        NOT_FOUND,
        INACTIVE,
        ACTIVE
    }

    @Transactional(readOnly = true)
    public PublicSiteAccess check(String slug) {
        Optional<Organizacao> orgOpt = organizacaoRepository.findBySlugAndAtivoTrue(slug);
        if (orgOpt.isEmpty()) {
            return PublicSiteAccess.notFound();
        }

        Organizacao org = orgOpt.get();
        Optional<SitePublicoConfig> configOpt = siteConfigRepository.findByOrganizacaoId(org.getId());

        boolean active = configOpt
                .map(c -> c.getActive() != null && c.getActive())
                .orElse(false);

        if (!active) {
            return PublicSiteAccess.inactive(org.getId());
        }

        ModoSite modo = limiteValidator.podeUsar(org.getId(), TipoLimite.SITE_PERSONALIZACAO_COMPLETA)
                ? ModoSite.COMPLETO
                : ModoSite.BASICO;

        return PublicSiteAccess.active(org.getId(), modo);
    }

    public static class PublicSiteAccess {
        private final Status status;
        private final ModoSite modo;
        private final Long organizacaoId;

        private PublicSiteAccess(Status status, ModoSite modo, Long organizacaoId) {
            this.status = status;
            this.modo = modo;
            this.organizacaoId = organizacaoId;
        }

        public static PublicSiteAccess notFound() {
            return new PublicSiteAccess(Status.NOT_FOUND, null, null);
        }

        public static PublicSiteAccess inactive(Long organizacaoId) {
            return new PublicSiteAccess(Status.INACTIVE, null, organizacaoId);
        }

        public static PublicSiteAccess active(Long organizacaoId, ModoSite modo) {
            return new PublicSiteAccess(Status.ACTIVE, modo, organizacaoId);
        }

        public Status getStatus() { return status; }
        public ModoSite getModo() { return modo; }
        public Long getOrganizacaoId() { return organizacaoId; }

        public boolean isActive() { return status == Status.ACTIVE; }
        public boolean isBasico() { return modo == ModoSite.BASICO; }
        public boolean isCompleto() { return modo == ModoSite.COMPLETO; }
    }
}
