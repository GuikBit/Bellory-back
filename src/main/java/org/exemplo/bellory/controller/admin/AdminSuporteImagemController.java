package org.exemplo.bellory.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.admin.SuporteImagemDTO;
import org.exemplo.bellory.model.dto.admin.SuportePastaDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.admin.AdminSuporteImagemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/suporte-imagens")
@RequiredArgsConstructor
@Tag(name = "Admin - Suporte Imagens", description = "Upload e gestao de imagens para base de conhecimento do agente de IA")
public class AdminSuporteImagemController {

    private final AdminSuporteImagemService adminSuporteImagemService;

    // ==================== Imagens ====================

    @Operation(summary = "Upload de imagem", description = "Faz upload de uma imagem para o diretorio de suporte. Opcionalmente pode especificar uma pasta.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseAPI<SuporteImagemDTO>> uploadImagem(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Nome da pasta (opcional). Se nao informado, salva na raiz do diretorio de suporte.")
            @RequestParam(value = "pasta", required = false) String pasta,
            @Parameter(description = "Nome personalizado para a imagem (opcional). Se nao informado, usa o nome original do arquivo.")
            @RequestParam(value = "nome", required = false) String nome) {
        try {
            SuporteImagemDTO resultado = adminSuporteImagemService.uploadImagem(file, pasta, nome);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<SuporteImagemDTO>builder()
                            .success(true)
                            .message("Imagem enviada com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<SuporteImagemDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Listar imagens", description = "Lista todas as imagens do diretorio de suporte. Opcionalmente filtra por pasta.")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<SuporteImagemDTO>>> listarImagens(
            @Parameter(description = "Nome da pasta para filtrar (opcional)")
            @RequestParam(value = "pasta", required = false) String pasta) {
        List<SuporteImagemDTO> imagens = adminSuporteImagemService.listarImagens(pasta);
        return ResponseEntity.ok(
                ResponseAPI.<List<SuporteImagemDTO>>builder()
                        .success(true)
                        .message("Imagens listadas com sucesso.")
                        .dados(imagens)
                        .build());
    }

    @Operation(summary = "Deletar imagem", description = "Deleta uma imagem pelo path relativo")
    @DeleteMapping
    public ResponseEntity<ResponseAPI<Void>> deletarImagem(
            @Parameter(description = "Path relativo da imagem (ex: admin/suporte/pasta/arquivo.png)")
            @RequestParam("relativePath") String relativePath) {
        try {
            adminSuporteImagemService.deletarImagem(relativePath);
            return ResponseEntity.ok(
                    ResponseAPI.<Void>builder()
                            .success(true)
                            .message("Imagem deletada com sucesso.")
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // ==================== Pastas ====================

    @Operation(summary = "Criar pasta", description = "Cria uma subpasta dentro do diretorio de suporte para organizar imagens")
    @PostMapping("/pastas")
    public ResponseEntity<ResponseAPI<SuportePastaDTO>> criarPasta(
            @RequestBody Map<String, String> body) {
        try {
            String nome = body.get("nome");
            SuportePastaDTO resultado = adminSuporteImagemService.criarPasta(nome);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<SuportePastaDTO>builder()
                            .success(true)
                            .message("Pasta criada com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<SuportePastaDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Listar pastas", description = "Lista todas as subpastas dentro do diretorio de suporte")
    @GetMapping("/pastas")
    public ResponseEntity<ResponseAPI<List<SuportePastaDTO>>> listarPastas() {
        List<SuportePastaDTO> pastas = adminSuporteImagemService.listarPastas();
        return ResponseEntity.ok(
                ResponseAPI.<List<SuportePastaDTO>>builder()
                        .success(true)
                        .message("Pastas listadas com sucesso.")
                        .dados(pastas)
                        .build());
    }

    @Operation(summary = "Deletar pasta", description = "Deleta uma pasta e todas as imagens dentro dela")
    @DeleteMapping("/pastas")
    public ResponseEntity<ResponseAPI<Void>> deletarPasta(
            @Parameter(description = "Nome da pasta a ser deletada")
            @RequestParam("nome") String nome) {
        try {
            adminSuporteImagemService.deletarPasta(nome);
            return ResponseEntity.ok(
                    ResponseAPI.<Void>builder()
                            .success(true)
                            .message("Pasta deletada com sucesso.")
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
