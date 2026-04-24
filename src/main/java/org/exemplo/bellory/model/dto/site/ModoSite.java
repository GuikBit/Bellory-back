package org.exemplo.bellory.model.dto.site;

/**
 * Indica o nível de personalização disponível para o site público,
 * derivado do plano da organização.
 *
 *  - COMPLETO: planos PLUS/PREMIUM — site totalmente configurável (hero, about, services, products, team, footer, header customizados).
 *  - BASICO: plano BASICO — apenas booking + logo + banner.
 */
public enum ModoSite {
    COMPLETO,
    BASICO
}
