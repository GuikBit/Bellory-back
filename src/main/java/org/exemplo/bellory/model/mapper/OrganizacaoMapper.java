package org.exemplo.bellory.model.mapper;

import org.exemplo.bellory.model.dto.organizacao.CreateOrganizacaoDTO;
import org.exemplo.bellory.model.dto.organizacao.OrganizacaoResponseDTO;
import org.exemplo.bellory.model.dto.organizacao.ResponsavelDTO;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.exemplo.bellory.model.entity.endereco.Endereco;
import org.exemplo.bellory.model.entity.organizacao.*;
import org.exemplo.bellory.model.entity.tema.*;
import org.springframework.stereotype.Component;

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
        org.setInscricaoEstadual(dto.getInscricaoEstadual());
        org.setEmailPrincipal(dto.getEmail());
        org.setTelefone1(dto.getTelefone1());
        org.setTelefone2(dto.getTelefone2());
        org.setWhatsapp(dto.getWhatsapp());
        org.setPublicoAlvo(dto.getPublicoAlvo());

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

        // Tema (Embeddable complexo)
        if (dto.getTema() != null) {
            Tema tema = new Tema();
            tema.setNome(dto.getTema().getNome());
            tema.setTipo(dto.getTema().getTipo());

            // Cores
            if (dto.getTema().getCores() != null) {
                Cores cores = new Cores();
                var coresDTO = dto.getTema().getCores();

                cores.setPrimary(coresDTO.getPrimary());
                cores.setSecondary(coresDTO.getSecondary());
                cores.setAccent(coresDTO.getAccent());
                cores.setBackground(coresDTO.getBackground());
                cores.setText(coresDTO.getText());
                cores.setTextSecondary(coresDTO.getTextSecondary());
                cores.setCardBackground(coresDTO.getCardBackground());
                cores.setCardBackgroundSecondary(coresDTO.getCardBackgroundSecondary());
                cores.setButtonText(coresDTO.getButtonText());
                cores.setBackgroundLinear(coresDTO.getBackgroundLinear());
                cores.setSuccess(coresDTO.getSuccess());
                cores.setWarning(coresDTO.getWarning());
                cores.setError(coresDTO.getError());
                cores.setInfo(coresDTO.getInfo());
                cores.setBorder(coresDTO.getBorder());
                cores.setBorderLight(coresDTO.getBorderLight());
                cores.setDivider(coresDTO.getDivider());
                cores.setOverlay(coresDTO.getOverlay());
                cores.setModalBackground(coresDTO.getModalBackground());
                cores.setInputBackground(coresDTO.getInputBackground());
                cores.setInputBorder(coresDTO.getInputBorder());
                cores.setInputFocus(coresDTO.getInputFocus());
                cores.setPlaceholder(coresDTO.getPlaceholder());
                cores.setNavBackground(coresDTO.getNavBackground());
                cores.setNavHover(coresDTO.getNavHover());
                cores.setNavActive(coresDTO.getNavActive());
                cores.setOnline(coresDTO.getOnline());
                cores.setOffline(coresDTO.getOffline());
                cores.setAway(coresDTO.getAway());
                cores.setBusy(coresDTO.getBusy());

                tema.setCores(cores);
            }

            // Fonts (com valores padrão)
            if (dto.getTema().getFonts() != null) {
                Fonts fonts = new Fonts();
                fonts.setHeading(dto.getTema().getFonts().getHeading());
                fonts.setBody(dto.getTema().getFonts().getBody());
                fonts.setMono(dto.getTema().getFonts().getMono());
                tema.setFonts(fonts);
            } else {
                // Valores padrão se não vier no JSON
                Fonts fonts = new Fonts();
                fonts.setHeading("Poppins, sans-serif");
                fonts.setBody("Inter, sans-serif");
                fonts.setMono("JetBrains Mono, monospace");
                tema.setFonts(fonts);
            }

            // BorderRadius (com valores padrão)
            if (dto.getTema().getBorderRadius() != null) {
                BorderRadius br = new BorderRadius();
                br.setSmall(dto.getTema().getBorderRadius().getSmall());
                br.setMedium(dto.getTema().getBorderRadius().getMedium());
                br.setLarge(dto.getTema().getBorderRadius().getLarge());
                br.setXl(dto.getTema().getBorderRadius().getXl());
                br.setFull(dto.getTema().getBorderRadius().getFull());
                tema.setBorderRadius(br);
            } else {
                BorderRadius br = new BorderRadius();
                br.setSmall("4px");
                br.setMedium("8px");
                br.setLarge("12px");
                br.setXl("16px");
                br.setFull("9999px");
                tema.setBorderRadius(br);
            }

            // Shadows (com valores padrão)
            if (dto.getTema().getShadows() != null) {
                Shadows shadows = new Shadows();
                shadows.setBase(dto.getTema().getShadows().getBase());
                shadows.setMd(dto.getTema().getShadows().getMd());
                shadows.setLg(dto.getTema().getShadows().getLg());
                shadows.setPrimaryGlow(dto.getTema().getShadows().getPrimaryGlow());
                shadows.setAccentGlow(dto.getTema().getShadows().getAccentGlow());
                tema.setShadows(shadows);
            } else {
                Shadows shadows = new Shadows();
                shadows.setBase("0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24)");
                shadows.setMd("0 4px 6px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.06)");
                shadows.setLg("0 10px 15px rgba(0, 0, 0, 0.1), 0 4px 6px rgba(0, 0, 0, 0.05)");

                // Gerar glows baseado nas cores do tema
                String primaryColor = dto.getTema().getCores() != null ?
                        dto.getTema().getCores().getPrimary() : "#3B82F6";
                shadows.setPrimaryGlow("0 0 20px " + primaryColor + "80");
                shadows.setAccentGlow("0 0 20px " + primaryColor + "60");
                tema.setShadows(shadows);
            }

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

        // Tema (mapeamento completo de todos os embeddables)
        if (org.getTema() != null) {
            Tema temaDTO = new Tema();
            temaDTO.setNome(org.getTema().getNome());
            temaDTO.setTipo(org.getTema().getTipo());

            // Cores
            if (org.getTema().getCores() != null) {
                Cores coresDTO = new Cores();
                var cores = org.getTema().getCores();

                coresDTO.setPrimary(cores.getPrimary());
                coresDTO.setSecondary(cores.getSecondary());
                coresDTO.setAccent(cores.getAccent());
                coresDTO.setBackground(cores.getBackground());
                coresDTO.setText(cores.getText());
                coresDTO.setTextSecondary(cores.getTextSecondary());
                coresDTO.setCardBackground(cores.getCardBackground());
                coresDTO.setCardBackgroundSecondary(cores.getCardBackgroundSecondary());
                coresDTO.setButtonText(cores.getButtonText());
                coresDTO.setBackgroundLinear(cores.getBackgroundLinear());
                coresDTO.setSuccess(cores.getSuccess());
                coresDTO.setWarning(cores.getWarning());
                coresDTO.setError(cores.getError());
                coresDTO.setInfo(cores.getInfo());
                coresDTO.setBorder(cores.getBorder());
                coresDTO.setBorderLight(cores.getBorderLight());
                coresDTO.setDivider(cores.getDivider());
                coresDTO.setOverlay(cores.getOverlay());
                coresDTO.setModalBackground(cores.getModalBackground());
                coresDTO.setInputBackground(cores.getInputBackground());
                coresDTO.setInputBorder(cores.getInputBorder());
                coresDTO.setInputFocus(cores.getInputFocus());
                coresDTO.setPlaceholder(cores.getPlaceholder());
                coresDTO.setNavBackground(cores.getNavBackground());
                coresDTO.setNavHover(cores.getNavHover());
                coresDTO.setNavActive(cores.getNavActive());
                coresDTO.setOnline(cores.getOnline());
                coresDTO.setOffline(cores.getOffline());
                coresDTO.setAway(cores.getAway());
                coresDTO.setBusy(cores.getBusy());

                temaDTO.setCores(coresDTO);
            }

            // Fonts
            if (org.getTema().getFonts() != null) {
                Fonts fontsDTO = new Fonts();
                fontsDTO.setHeading(org.getTema().getFonts().getHeading());
                fontsDTO.setBody(org.getTema().getFonts().getBody());
                fontsDTO.setMono(org.getTema().getFonts().getMono());
                temaDTO.setFonts(fontsDTO);
            }

            // BorderRadius
            if (org.getTema().getBorderRadius() != null) {
                BorderRadius brDTO = new BorderRadius();
                brDTO.setSmall(org.getTema().getBorderRadius().getSmall());
                brDTO.setMedium(org.getTema().getBorderRadius().getMedium());
                brDTO.setLarge(org.getTema().getBorderRadius().getLarge());
                brDTO.setXl(org.getTema().getBorderRadius().getXl());
                brDTO.setFull(org.getTema().getBorderRadius().getFull());
                temaDTO.setBorderRadius(brDTO);
            }

            // Shadows
            if (org.getTema().getShadows() != null) {
                Shadows shadowsDTO = new Shadows();
                shadowsDTO.setBase(org.getTema().getShadows().getBase());
                shadowsDTO.setMd(org.getTema().getShadows().getMd());
                shadowsDTO.setLg(org.getTema().getShadows().getLg());
                shadowsDTO.setPrimaryGlow(org.getTema().getShadows().getPrimaryGlow());
                shadowsDTO.setAccentGlow(org.getTema().getShadows().getAccentGlow());
                temaDTO.setShadows(shadowsDTO);
            }

            dto.setTema(temaDTO);
        }

        // Plano
        if (org.getPlano() != null) {
            dto.setPlanoId(org.getPlano().getId());
            dto.setPlanoNome(org.getPlano().getNome());
        }

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
            dto.setEnderecoPrincipal(endDTO);
        }

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
}
