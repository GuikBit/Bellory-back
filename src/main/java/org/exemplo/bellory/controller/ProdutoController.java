package org.exemplo.bellory.controller;

import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.produto.Produto;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.service.ProdutoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/produto")
public class ProdutoController {

    ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public ResponseEntity<ResponseAPI<List<Produto>>> getListAllProdutos() {
        List<Produto> produtos = produtoService.getListAllProdutos();

        if (produtos.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT) // Ou HttpStatus.NO_CONTENT, dependendo da sua regra
                    .body(ResponseAPI.<List<Produto>>builder()
                            .success(true)
                            .message("Nenhum serviço encontrado.")
                            .dados(produtos) // Ainda envia a lista vazia
                            .build());
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseAPI.<List<Produto>>builder()
                        .success(true)
                        .message("Lista de serviços recuperada com sucesso.")
                        .dados(produtos)
                        .build());
    }
}
