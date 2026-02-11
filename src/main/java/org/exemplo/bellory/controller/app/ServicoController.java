package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exemplo.bellory.model.dto.ServicoAgendamento;
import org.exemplo.bellory.model.dto.ServicoCreateDTO;
import org.exemplo.bellory.model.dto.ServicoDTO;
import org.exemplo.bellory.model.entity.enums.TipoCategoria;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.entity.servico.Servico;
import org.exemplo.bellory.service.CategoriaService;
import org.exemplo.bellory.service.ServicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servico")
@Tag(name = "Serviços", description = "Gerenciamento de serviços e categorias de serviço")
public class ServicoController {

    private final ServicoService servicoService;
    private final CategoriaService categoriaService;

    public ServicoController(ServicoService servicoService, CategoriaService categoriaService) {
        this.servicoService = servicoService;
        this.categoriaService = categoriaService;
    }


    @Operation(summary = "Listar todos os serviços")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<ServicoDTO>>> getServicoList() {
        List<Servico> servicos = servicoService.getListAllServicos();
        List<ServicoDTO> servicosDTO = servicos.stream().map(ServicoDTO::new).collect(Collectors.toList());

        return ResponseEntity.ok(ResponseAPI.<List<ServicoDTO>>builder()
                .success(true)
                .message("Lista de serviços recuperada com sucesso.")
                .dados(servicosDTO)
                .build());
    }

    @Operation(summary = "Buscar serviço por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<ServicoDTO>> getServicoById(@PathVariable Long id) {
        try {
            Servico servico = servicoService.getServicoById(id);
            return ResponseEntity.ok(ResponseAPI.<ServicoDTO>builder()
                    .success(true)
                    .dados(new ServicoDTO(servico))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ServicoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @Operation(summary = "Listar serviços para agendamento")
    @GetMapping("/agendamento")
    public ResponseEntity<ResponseAPI<List<ServicoAgendamento>>> getServicoAgendamentoList() {
        List<ServicoAgendamento> servicos = servicoService.getListAgendamentoServicos();
        return ResponseEntity.ok(ResponseAPI.<List<ServicoAgendamento>>builder()
                .success(true)
                .message("Lista de serviços para agendamento recuperada com sucesso.")
                .dados(servicos)
                .build());
    }

    @Operation(summary = "Criar novo serviço")
    @PostMapping
    public ResponseEntity<ResponseAPI<ServicoDTO>> postServico(@RequestBody ServicoCreateDTO servicoDTO) {
        try {
            Servico novoServico = servicoService.createServico(servicoDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<ServicoDTO>builder()
                            .success(true)
                            .message("Serviço criado com sucesso!")
                            .dados(new ServicoDTO(novoServico))
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ServicoDTO>builder()
                            .success(false)
                            .message("Erro ao criar serviço: " + e.getMessage())
                            .errorCode(400)
                            .build());
        }
    }

    @Operation(summary = "Atualizar serviço")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<ServicoDTO>> updateServico(@PathVariable Long id, @RequestBody ServicoCreateDTO servicoDTO) {
        try {
            Servico servicoAtualizado = servicoService.updateServico(id, servicoDTO);
            return ResponseEntity.ok(ResponseAPI.<ServicoDTO>builder()
                    .success(true)
                    .message("Serviço atualizado com sucesso!")
                    .dados(new ServicoDTO(servicoAtualizado))
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<ServicoDTO>builder()
                            .success(false)
                            .message("Erro ao atualizar serviço: " + e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @Operation(summary = "Desativar serviço")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> deleteServico(@PathVariable Long id) {
        try {
            servicoService.deleteServico(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Serviço desativado com sucesso.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @Operation(summary = "Listar categorias de serviço")
    @GetMapping("/categorias")
    public ResponseEntity<ResponseAPI<List<Categoria>>> getServiceCategorias() {
        List<Categoria> categorias = categoriaService.findByTipo(TipoCategoria.SERVICO);
        return ResponseEntity.ok(ResponseAPI.<List<Categoria>>builder()
                .success(true)
                .message("Lista de categorias de serviço recuperada com sucesso.")
                .dados(categorias)
                .build());
    }

    @Operation(summary = "Criar categoria de serviço")
    @PostMapping("/categoria")
    public ResponseEntity<ResponseAPI<Categoria>> createServiceCategoria(@RequestBody Categoria categoria) {
        try {
            categoria.setTipo(TipoCategoria.SERVICO);
            Categoria novaCategoria = categoriaService.createCategoria(categoria);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<Categoria>builder()
                            .success(true)
                            .message("Categoria de serviço criada com sucesso!")
                            .dados(novaCategoria)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Categoria>builder()
                            .success(false)
                            .message("Erro ao criar categoria: " + e.getMessage())
                            .errorCode(400)
                            .build());
        }
    }

    @Operation(summary = "Atualizar categoria de serviço")
    @PutMapping("/categoria/{id}")
    public ResponseEntity<ResponseAPI<Categoria>> updateServiceCategoria(@PathVariable Long id, @RequestBody Categoria categoria) {
        try {
            categoria.setTipo(TipoCategoria.SERVICO);
            Categoria categoriaAtualizada = categoriaService.updateCategoria(id, categoria);
            return ResponseEntity.ok(ResponseAPI.<Categoria>builder()
                    .success(true)
                    .message("Categoria de serviço atualizada com sucesso!")
                    .dados(categoriaAtualizada)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Categoria>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }

    @Operation(summary = "Desativar categoria de serviço")
    @DeleteMapping("/categoria/{id}")
    public ResponseEntity<ResponseAPI<Void>> deleteServiceCategoria(@PathVariable Long id) {
        try {
            categoriaService.deleteCategoria(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Categoria de serviço desativada com sucesso.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .errorCode(404)
                            .build());
        }
    }
}
