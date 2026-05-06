package org.exemplo.bellory.model.mapper;

import org.exemplo.bellory.model.dto.organizacao.CreateOrganizacaoDTO;
import org.exemplo.bellory.model.dto.organizacao.OrganizacaoResponseDTO;
import org.exemplo.bellory.model.dto.organizacao.ResponsavelDTO;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.endereco.Coordenadas;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.organizacao.*;
import org.exemplo.bellory.model.entity.tema.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper manual para conversão entre DTOs e Entities
 * Lida com as diferenças de nomenclatura entre JSON e entidades
 */
@Component
public class OrganizacaoMapper {

    /**
     * Converte CreateOrganizacaoDTO para Organizacao Entity
     */
    public Organizacao toEntity(CreateOrganizacaoDTO dto) {
        if (dto == null) return null;

        Organizacao org = new Organizacao();

        // Campos básicos
        org.setCnpj(dto.getCnpj());
        org.setRazaoSocial(dto.getRazaoSocial());
        org.setNomeFantasia(dto.getNomeFantasia());
        org.setInscricaoEstadual(normalizarString(dto.getInscricaoEstadual()));
        org.setEmailPrincipal(dto.getEmail());
        org.setTelefone1(dto.getTelefone1());
        org.setTelefone2(dto.getTelefone2());
        org.setWhatsapp(dto.getWhatsapp());
        org.setPublicoAlvo(normalizarPublicoAlvo(dto.getPublicoAlvo()));
        org.setSegmento(normalizarString(dto.getSegmento()));

        // Responsavel (Embeddable)
        if (dto.getResponsavel() != null) {
            Responsavel responsavel = new Responsavel();
            responsavel.setNome(dto.getResponsavel().getNome());
            responsavel.setEmail(dto.getResponsavel().getEmail());
            responsavel.setTelefone(dto.getResponsavel().getTelefone());
            org.setResponsavel(responsavel);
        }

        // Endereco (OneToOne Entity)
        if (dto.getEndereco() != null) {
            Endereco endereco = new Endereco();
            endereco.setCep(dto.getEndereco().getCep());
            endereco.setLogradouro(dto.getEndereco().getLogradouro());
            endereco.setNumero(dto.getEndereco().getNumero());
            endereco.setComplemento(dto.getEndereco().getComplemento());
            endereco.setBairro(dto.getEndereco().getBairro());
            endereco.setCidade(dto.getEndereco().getCidade());
            endereco.setUf(dto.getEndereco().getUf());

            Coordenadas cood = applyCoordenadas(
                    null,
                    dto.getEndereco().getLatitude(),
                    dto.getEndereco().getLongitude());
            if (cood != null) {
                endereco.setCoordenadas(cood);
            }

            org.setEnderecoPrincipal(endereco);

        }

        // AcessoAdm (Embeddable)
        if (dto.getAcessoAdm() != null) {
            AcessoAdm acessoAdm = new AcessoAdm();
            acessoAdm.setLogin(dto.getAcessoAdm().getLogin());
            acessoAdm.setSenha(dto.getAcessoAdm().getSenha()); // Será criptografada no service
            acessoAdm.setRole(dto.getAcessoAdm().getRole() != null ?
                    dto.getAcessoAdm().getRole() : "ROLE_ADMIN");
//            org.setAcessoAdm(acessoAdm);
        }

        // Tema: hoje recebemos apenas o identificador do tema (string) e gravamos em tema_nome.
        // Cores/fonts/borderRadius/shadows ficam null no banco — o front aplica o preset pelo nome.
        // tema_tipo é inferido por convenção do prefixo do nome (masculino*/feminino*/unissex*).
        if (dto.getTema() != null && !dto.getTema().isBlank()) {
            Tema tema = new Tema();
            tema.setNome(dto.getTema().trim());
            tema.setTipo(inferirTipoTema(dto.getTema()));
            org.setTema(tema);
        }

        // Plano - NÃO SETAR AQUI, será setado no Service após buscar do banco
        // O DTO do plano contém informações de pagamento, não uma referência ao plano

        // Campos de controle
        org.setAtivo(true);

        return org;
    }

    /**
     * Converte Organizacao Entity para OrganizacaoResponseDTO
     */
    public OrganizacaoResponseDTO toResponseDTO(Organizacao org) {
        if (org == null) return null;

        OrganizacaoResponseDTO dto = new OrganizacaoResponseDTO();

        // Campos básicos
        dto.setId(org.getId());
        dto.setCnpj(org.getCnpj());
        dto.setRazaoSocial(org.getRazaoSocial());
        dto.setNomeFantasia(org.getNomeFantasia());
        dto.setInscricaoEstadual(org.getInscricaoEstadual());
        dto.setEmailPrincipal(org.getEmailPrincipal());
        dto.setTelefone1(org.getTelefone1());
        dto.setTelefone2(org.getTelefone2());
        dto.setWhatsapp(org.getWhatsapp());
        dto.setSlug(org.getSlug());
        dto.setPublicoAlvo(org.getPublicoAlvo());
        dto.setSegmento(org.getSegmento());

        // Responsavel
        if (org.getResponsavel() != null) {
            Responsavel respDTO = new Responsavel();
            respDTO.setNome(org.getResponsavel().getNome());
            respDTO.setEmail(org.getResponsavel().getEmail());
            respDTO.setTelefone(org.getResponsavel().getTelefone());
            dto.setResponsavel(respDTO);
        }

        if(org.getConfigSistema() != null) {
            ConfigSistema config = new ConfigSistema();
            config.setId(org.getConfigSistema().getId());
            config.setTenantId(org.getConfigSistema().getTenantId());
            config.setUrlAcesso(org.getConfigSistema().getUrlAcesso());

            dto.setConfigSistema(config);
        }

        if(org.getRedesSociais() != null) {
            RedesSociais redes = new RedesSociais();

            redes.setFacebook(org.getRedesSociais().getFacebook());
            redes.setSite(org.getRedesSociais().getSite());
            redes.setLinkedin(org.getRedesSociais().getLinkedin());
            redes.setWhatsapp(org.getRedesSociais().getWhatsapp());
            redes.setYoutube(org.getRedesSociais().getYoutube());
            redes.setMessenger(org.getRedesSociais().getMessenger());

            dto.setRedesSociais(redes);
        }

        // AcessoAdm (SEM SENHA!)
//        if (org.getAcessoAdm() != null) {
//            AcessoAdm acessoDTO = new AcessoAdm();
//            acessoDTO.setLogin(org.getAcessoAdm().getLogin());
//            acessoDTO.setRole(org.getAcessoAdm().getRole());
//            // ⚠️ NÃO incluir senha!
//            dto.setAcessoAdm(acessoDTO);
//        }

        // Tema: devolve só o identificador (tema_nome). Front aplica o preset pelo nome.
        if (org.getTema() != null) {
            dto.setTema(org.getTema().getNome());
        }

        // Plano/limites agora vem da Payment API (consumido no frontend via /auth/me + /assinatura/refresh-cache).

        // Endereco
        if (org.getEnderecoPrincipal() != null) {
            Endereco endDTO = new Endereco();
            endDTO.setId(org.getEnderecoPrincipal().getId());
            endDTO.setCep(org.getEnderecoPrincipal().getCep());
            endDTO.setLogradouro(org.getEnderecoPrincipal().getLogradouro());
            endDTO.setNumero(org.getEnderecoPrincipal().getNumero());
            endDTO.setComplemento(org.getEnderecoPrincipal().getComplemento());
            endDTO.setBairro(org.getEnderecoPrincipal().getBairro());
            endDTO.setCidade(org.getEnderecoPrincipal().getCidade());
            endDTO.setUf(org.getEnderecoPrincipal().getUf());
            endDTO.setReferencia(org.getEnderecoPrincipal().getReferencia());
            endDTO.setCoordenadas(org.getEnderecoPrincipal().getCoordenadas());
            dto.setEnderecoPrincipal(endDTO);
        }

        // Imagens
        dto.setLogoUrl(org.getLogoUrl());
        dto.setBannerUrl(org.getBannerUrl());

        // Controle
        dto.setAtivo(org.getAtivo());
        dto.setDtCadastro(org.getDtCadastro());
        dto.setDtAtualizacao(org.getDtAtualizacao());

        return dto;
    }

    /**
     * Converte lista de Organizacao para lista de OrganizacaoResponseDTO
     */
    public java.util.List<OrganizacaoResponseDTO> toResponseDTOList(
            java.util.List<Organizacao> organizacoes) {
        if (organizacoes == null) return null;

        return organizacoes.stream()
                .map(this::toResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Normaliza string opcional: trim + null se ficar vazia. Útil para campos como
     * inscricaoEstadual que chegam como "" do front e a gente prefere persistir como NULL.
     */
    public String normalizarString(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Normaliza publicoAlvo aceitando M/F/U ou Masculino/Feminino/Unissex (case-insensitive)
     * e devolve a forma canônica curta (M/F/U). Retorna null se a entrada for vazia.
     * O Bean Validation (@Pattern) já barra valores fora do whitelist antes de chegar aqui;
     * o switch default é defesa em profundidade.
     */
    public String normalizarPublicoAlvo(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return switch (valor.trim().toUpperCase()) {
            case "M", "MASCULINO" -> "M";
            case "F", "FEMININO" -> "F";
            case "U", "UNISSEX" -> "U";
            default -> throw new IllegalArgumentException(
                    "Publico alvo invalido: aceita M, F, U, Masculino, Feminino ou Unissex");
        };
    }

    /**
     * Infere o tipo do tema a partir do prefixo do nome (convenção do front: "masculinoClassico",
     * "femininoModerno", "unissexPadrao"). Retorna null se nenhum prefixo conhecido casar.
     */
    public String inferirTipoTema(String nome) {
        if (nome == null) return null;
        String n = nome.trim().toLowerCase();
        if (n.startsWith("masculino")) return "masculino";
        if (n.startsWith("feminino")) return "feminino";
        if (n.startsWith("unissex") || n.startsWith("neutro")) return "unissex";
        return null;
    }

    /**
     * Aplica latitude/longitude (BigDecimal) sobre um {@link Coordenadas}, criando o objeto
     * se ainda não existir. Mantém o valor previamente persistido se um dos lados for null.
     * Retorna null somente quando lat e lng são ambos null e não havia coordenadas existentes.
     */
    public Coordenadas applyCoordenadas(Coordenadas existente, BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null && longitude == null) {
            return existente;
        }
        Coordenadas alvo = existente != null ? existente : new Coordenadas();
        if (latitude != null) {
            alvo.setLatitude(latitude.toPlainString());
        }
        if (longitude != null) {
            alvo.setLongitude(longitude.toPlainString());
        }
        return alvo;
    }
}
