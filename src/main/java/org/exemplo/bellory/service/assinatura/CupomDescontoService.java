package org.exemplo.bellory.service.assinatura;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.bellory.model.dto.cupom.CupomValidacaoResult;
import org.exemplo.bellory.model.entity.assinatura.CupomDesconto;
import org.exemplo.bellory.model.entity.assinatura.CupomUtilizacao;
import org.exemplo.bellory.model.entity.assinatura.TipoDesconto;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.assinatura.CupomDescontoRepository;
import org.exemplo.bellory.model.repository.assinatura.CupomUtilizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CupomDescontoService {

    private final CupomDescontoRepository cupomDescontoRepository;
    private final CupomUtilizacaoRepository cupomUtilizacaoRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CupomValidacaoResult validarCupom(String codigo, Organizacao organizacao, String planoCodigo, String cicloCobranca, BigDecimal valorOriginal) {
        // Buscar cupom ativo
        CupomDesconto cupom = cupomDescontoRepository.findByCodigoAtivo(codigo.toUpperCase().trim())
                .orElse(null);

        if (cupom == null) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom não encontrado ou inativo")
                    .build();
        }

        // Verificar vigencia
        if (!cupom.isVigente()) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom fora do período de validade")
                    .build();
        }

        // Verificar limite global
        if (cupom.atingiuLimiteGlobal()) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom atingiu o limite máximo de utilizações")
                    .build();
        }

        // Verificar limite por organizacao
        if (cupom.getMaxUtilizacoesPorOrg() != null) {
            long utilizacoesOrg = cupomUtilizacaoRepository.countByCupomIdAndOrganizacaoId(cupom.getId(), organizacao.getId());
            if (utilizacoesOrg >= cupom.getMaxUtilizacoesPorOrg()) {
                return CupomValidacaoResult.builder()
                        .valido(false)
                        .mensagem("Cupom já utilizado o máximo de vezes para esta organização")
                        .build();
            }
        }

        // Verificar plano permitido
        List<String> planos = parseJsonList(cupom.getPlanosPermitidos(), new TypeReference<List<String>>() {});
        if (planos != null && !planos.isEmpty() && !planos.contains(planoCodigo)) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom não é válido para o plano selecionado")
                    .build();
        }

        // Verificar segmento (publicoAlvo) da organizacao
        List<String> segmentos = parseJsonList(cupom.getSegmentosPermitidos(), new TypeReference<List<String>>() {});
        if (segmentos != null && !segmentos.isEmpty()) {
            String publicoAlvo = organizacao.getPublicoAlvo();
            if (publicoAlvo == null || !segmentos.contains(publicoAlvo)) {
                return CupomValidacaoResult.builder()
                        .valido(false)
                        .mensagem("Cupom não é válido para o segmento da sua organização")
                        .build();
            }
        }

        // Verificar organizacao permitida
        List<Long> orgsPermitidas = parseJsonList(cupom.getOrganizacoesPermitidas(), new TypeReference<List<Long>>() {});
        if (orgsPermitidas != null && !orgsPermitidas.isEmpty() && !orgsPermitidas.contains(organizacao.getId())) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom não é válido para esta organização")
                    .build();
        }

        // Verificar ciclo de cobranca
        if (cupom.getCicloCobranca() != null && !cupom.getCicloCobranca().isEmpty()
                && !cupom.getCicloCobranca().equalsIgnoreCase(cicloCobranca)) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom não é válido para o ciclo de cobrança selecionado")
                    .build();
        }

        // Calcular desconto
        BigDecimal desconto = calcularDesconto(cupom, valorOriginal);
        BigDecimal valorComDesconto = valorOriginal.subtract(desconto);
        if (valorComDesconto.compareTo(BigDecimal.ZERO) < 0) {
            valorComDesconto = BigDecimal.ZERO;
        }

        return CupomValidacaoResult.builder()
                .valido(true)
                .mensagem("Cupom valido")
                .cupom(cupom)
                .valorOriginal(valorOriginal)
                .valorDesconto(desconto)
                .valorComDesconto(valorComDesconto)
                .build();
    }

    @Transactional(readOnly = true)
    public CupomValidacaoResult validarCupomPublico(String codigo, String planoCodigo, String cicloCobranca, BigDecimal valorOriginal) {
        CupomDesconto cupom = cupomDescontoRepository.findByCodigoAtivo(codigo.toUpperCase().trim())
                .orElse(null);

        if (cupom == null) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom não encontrado ou inativo")
                    .build();
        }

        if (!cupom.isVigente()) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom fora do período de validade")
                    .build();
        }

        if (cupom.atingiuLimiteGlobal()) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom atingiu o limite máximo de utilizações")
                    .build();
        }

        List<String> planos = parseJsonList(cupom.getPlanosPermitidos(), new TypeReference<List<String>>() {});
        if (planos != null && !planos.isEmpty() && !planos.contains(planoCodigo)) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom não é válido para o plano selecionado")
                    .build();
        }

        if (cupom.getCicloCobranca() != null && !cupom.getCicloCobranca().isEmpty()
                && !cupom.getCicloCobranca().equalsIgnoreCase(cicloCobranca)) {
            return CupomValidacaoResult.builder()
                    .valido(false)
                    .mensagem("Cupom não é válido para o ciclo de cobrança selecionado")
                    .build();
        }

        BigDecimal desconto = calcularDesconto(cupom, valorOriginal);
        BigDecimal valorComDesconto = valorOriginal.subtract(desconto);
        if (valorComDesconto.compareTo(BigDecimal.ZERO) < 0) {
            valorComDesconto = BigDecimal.ZERO;
        }

        return CupomValidacaoResult.builder()
                .valido(true)
                .mensagem("Cupom valido")
                .cupom(cupom)
                .valorOriginal(valorOriginal)
                .valorDesconto(desconto)
                .valorComDesconto(valorComDesconto)
                .build();
    }

    public BigDecimal calcularDesconto(CupomDesconto cupom, BigDecimal valorOriginal) {
        if (cupom.getTipoDesconto() == TipoDesconto.PERCENTUAL) {
            return valorOriginal.multiply(cupom.getValorDesconto())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // VALOR_FIXO: min(desconto, valor) para nao ficar negativo
            return cupom.getValorDesconto().min(valorOriginal);
        }
    }

    @Transactional
    public CupomUtilizacao registrarUtilizacao(CupomDesconto cupom, Long organizacaoId,
                                                Long assinaturaId, Long cobrancaId,
                                                BigDecimal valorOriginal, BigDecimal valorDesconto,
                                                BigDecimal valorFinal, String planoCodigo, String cicloCobranca) {
        CupomUtilizacao utilizacao = CupomUtilizacao.builder()
                .cupom(cupom)
                .organizacaoId(organizacaoId)
                .assinaturaId(assinaturaId)
                .cobrancaId(cobrancaId)
                .valorOriginal(valorOriginal)
                .valorDesconto(valorDesconto)
                .valorFinal(valorFinal)
                .planoCodigo(planoCodigo)
                .cicloCobranca(cicloCobranca)
                .build();

        CupomUtilizacao salva = cupomUtilizacaoRepository.save(utilizacao);

        // Incrementar total utilizado
        cupom.setTotalUtilizado(cupom.getTotalUtilizado() + 1);
        cupomDescontoRepository.save(cupom);

        log.info("Cupom {} utilizado pela organizacao {} - desconto: {}", cupom.getCodigo(), organizacaoId, valorDesconto);
        return salva;
    }

    private <T> T parseJsonList(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("Erro ao parsear JSON: {}", e.getMessage());
            return null;
        }
    }
}
