package org.exemplo.bellory.service;

import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;

@Service
public class OrganizacaoService {


    OrganizacaoRepository organizacaoRepository;

    public OrganizacaoService(OrganizacaoRepository organizacaoRepository) {
        this.organizacaoRepository = organizacaoRepository;
    }


    public Organizacao getOrganizacaoPadrao() {
            return null;
    }
}
