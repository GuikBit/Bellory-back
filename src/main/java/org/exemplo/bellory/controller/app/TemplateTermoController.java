package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.questionario.TemplateTermoDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.questionario.enums.TipoTemplateTermo;
import org.exemplo.bellory.service.questionario.TemplateTermoCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questionarios/templates-termo")
@Tag(name = "Templates de Termo de Consentimento",
        description = "Catálogo de templates pré-definidos para perguntas do tipo TERMO_CONSENTIMENTO")
public class TemplateTermoController {

    private final TemplateTermoCatalog catalog;

    public TemplateTermoController(TemplateTermoCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Operation(summary = "Listar templates de termo disponíveis")
    public ResponseEntity<ResponseAPI<List<TemplateTermoDTO>>> listar() {
        List<TemplateTermoDTO> templates = catalog.listar();
        return ResponseEntity.ok(ResponseAPI.<List<TemplateTermoDTO>>builder()
                .success(true)
                .dados(templates)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar template específico pelo identificador")
    public ResponseEntity<ResponseAPI<TemplateTermoDTO>> buscarPorId(@PathVariable TipoTemplateTermo id) {
        return catalog.buscarPorId(id)
                .map(template -> ResponseEntity.ok(ResponseAPI.<TemplateTermoDTO>builder()
                        .success(true)
                        .dados(template)
                        .build()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseAPI.<TemplateTermoDTO>builder()
                                .success(false)
                                .message("Template não encontrado.")
                                .errorCode(404)
                                .build()));
    }
}
