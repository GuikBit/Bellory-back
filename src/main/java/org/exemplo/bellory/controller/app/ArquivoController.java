package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.bellory.model.dto.arquivo.ArquivoDTO;
import org.exemplo.bellory.model.dto.arquivo.PastaArquivoDTO;
import org.exemplo.bellory.model.dto.arquivo.StorageUsageDTO;
import org.exemplo.bellory.model.entity.error.ResponseAPI;
import org.exemplo.bellory.service.ArquivoStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/arquivos")
@RequiredArgsConstructor
@Tag(name = "Arquivos", description = "Gerenciamento de arquivos e pastas da organizacao")
public class ArquivoController {

    private final ArquivoStorageService arquivoStorageService;

    // ==================== UPLOAD ====================

    @Operation(summary = "Upload de arquivos", description = "Faz upload de um ou mais arquivos. Opcionalmente pode especificar uma pasta destino.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseAPI<List<ArquivoDTO>>> uploadArquivos(
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "ID da pasta destino (opcional). Se não informado, salva na raiz.")
            @RequestParam(value = "pastaId", required = false) Long pastaId) {
        try {
            List<ArquivoDTO> resultados = arquivoStorageService.uploadArquivos(files, pastaId);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<List<ArquivoDTO>>builder()
                            .success(true)
                            .message(resultados.size() + " arquivo(s) enviado(s) com sucesso.")
                            .dados(resultados)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<List<ArquivoDTO>>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // ==================== LISTAR ARQUIVOS ====================

    @Operation(summary = "Listar arquivos", description = "Lista arquivos de uma pasta. Se pastaId nao informado, lista arquivos da raiz.")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<ArquivoDTO>>> listarArquivos(
            @Parameter(description = "ID da pasta (opcional)")
            @RequestParam(value = "pastaId", required = false) Long pastaId) {
        List<ArquivoDTO> arquivos = arquivoStorageService.listarArquivos(pastaId);
        return ResponseEntity.ok(
                ResponseAPI.<List<ArquivoDTO>>builder()
                        .success(true)
                        .message("Arquivos listados com sucesso.")
                        .dados(arquivos)
                        .build());
    }

    @Operation(summary = "Listar todos os arquivos", description = "Lista todos os arquivos da organizacao independente da pasta.")
    @GetMapping("/todos")
    public ResponseEntity<ResponseAPI<List<ArquivoDTO>>> listarTodosArquivos() {
        List<ArquivoDTO> arquivos = arquivoStorageService.listarTodosArquivos();
        return ResponseEntity.ok(
                ResponseAPI.<List<ArquivoDTO>>builder()
                        .success(true)
                        .message("Todos os arquivos listados com sucesso.")
                        .dados(arquivos)
                        .build());
    }

    // ==================== DELETAR ARQUIVO ====================

    @Operation(summary = "Deletar arquivo", description = "Deleta um arquivo pelo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> deletarArquivo(@PathVariable Long id) {
        try {
            arquivoStorageService.deletarArquivo(id);
            return ResponseEntity.ok(
                    ResponseAPI.<Void>builder()
                            .success(true)
                            .message("Arquivo deletado com sucesso.")
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // ==================== MOVER ARQUIVO ====================

    @Operation(summary = "Mover arquivo", description = "Move um arquivo para outra pasta. Envie pastaId=null para mover para a raiz.")
    @PutMapping("/{id}/mover")
    public ResponseEntity<ResponseAPI<ArquivoDTO>> moverArquivo(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        try {
            Long novaPastaId = body.get("pastaId");
            ArquivoDTO resultado = arquivoStorageService.moverArquivo(id, novaPastaId);
            return ResponseEntity.ok(
                    ResponseAPI.<ArquivoDTO>builder()
                            .success(true)
                            .message("Arquivo movido com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ArquivoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // ==================== RENOMEAR ARQUIVO ====================

    @Operation(summary = "Renomear arquivo", description = "Renomeia um arquivo (mantem a extensao original)")
    @PutMapping("/{id}/renomear")
    public ResponseEntity<ResponseAPI<ArquivoDTO>> renomearArquivo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String novoNome = body.get("nome");
            ArquivoDTO resultado = arquivoStorageService.renomearArquivo(id, novoNome);
            return ResponseEntity.ok(
                    ResponseAPI.<ArquivoDTO>builder()
                            .success(true)
                            .message("Arquivo renomeado com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<ArquivoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // ==================== PASTAS ====================

    @Operation(summary = "Criar pasta", description = "Cria uma nova pasta. Pode ser na raiz ou dentro de outra pasta.")
    @PostMapping("/pastas")
    public ResponseEntity<ResponseAPI<PastaArquivoDTO>> criarPasta(@RequestBody Map<String, Object> body) {
        try {
            String nome = (String) body.get("nome");
            Long pastaPaiId = body.get("pastaPaiId") != null ? Long.valueOf(body.get("pastaPaiId").toString()) : null;
            PastaArquivoDTO resultado = arquivoStorageService.criarPasta(nome, pastaPaiId);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseAPI.<PastaArquivoDTO>builder()
                            .success(true)
                            .message("Pasta criada com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<PastaArquivoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Listar pastas", description = "Lista subpastas. Se pastaPaiId nao informado, lista pastas da raiz.")
    @GetMapping("/pastas")
    public ResponseEntity<ResponseAPI<List<PastaArquivoDTO>>> listarPastas(
            @Parameter(description = "ID da pasta pai (opcional)")
            @RequestParam(value = "pastaPaiId", required = false) Long pastaPaiId) {
        List<PastaArquivoDTO> pastas = arquivoStorageService.listarPastas(pastaPaiId);
        return ResponseEntity.ok(
                ResponseAPI.<List<PastaArquivoDTO>>builder()
                        .success(true)
                        .message("Pastas listadas com sucesso.")
                        .dados(pastas)
                        .build());
    }

    @Operation(summary = "Obter pasta", description = "Retorna detalhes de uma pasta com suas subpastas e arquivos")
    @GetMapping("/pastas/{id}")
    public ResponseEntity<ResponseAPI<PastaArquivoDTO>> obterPasta(@PathVariable Long id) {
        try {
            PastaArquivoDTO pasta = arquivoStorageService.obterPasta(id);
            return ResponseEntity.ok(
                    ResponseAPI.<PastaArquivoDTO>builder()
                            .success(true)
                            .message("Pasta encontrada.")
                            .dados(pasta)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<PastaArquivoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Renomear pasta", description = "Renomeia uma pasta existente")
    @PutMapping("/pastas/{id}/renomear")
    public ResponseEntity<ResponseAPI<PastaArquivoDTO>> renomearPasta(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String novoNome = body.get("nome");
            PastaArquivoDTO resultado = arquivoStorageService.renomearPasta(id, novoNome);
            return ResponseEntity.ok(
                    ResponseAPI.<PastaArquivoDTO>builder()
                            .success(true)
                            .message("Pasta renomeada com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<PastaArquivoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Deletar pasta", description = "Deleta uma pasta e todos os seus arquivos e subpastas")
    @DeleteMapping("/pastas/{id}")
    public ResponseEntity<ResponseAPI<Void>> deletarPasta(@PathVariable Long id) {
        try {
            arquivoStorageService.deletarPasta(id);
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

    // ==================== NAVEGAÇÃO ====================

    @Operation(summary = "Navegar pasta do modulo", description = "Retorna conteudo de uma pasta do modulo de arquivos (subpastas + arquivos). Se pastaId nao informado, retorna a raiz com TODAS as pastas (sistema + usuário).")
    @GetMapping("/navegar")
    public ResponseEntity<ResponseAPI<PastaArquivoDTO>> navegarPasta(
            @Parameter(description = "ID da pasta do modulo (opcional, null = raiz)")
            @RequestParam(value = "pastaId", required = false) Long pastaId) {
        try {
            PastaArquivoDTO resultado = arquivoStorageService.navegarPasta(pastaId);
            return ResponseEntity.ok(
                    ResponseAPI.<PastaArquivoDTO>builder()
                            .success(true)
                            .message("Conteúdo listado com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<PastaArquivoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Navegar pasta do sistema", description = "Retorna conteudo de uma pasta do sistema (colaboradores, servicos, produtos, organizacao, arquivos). Arquivos do sistema sao somente leitura.")
    @GetMapping("/navegar/sistema/{nomePasta}")
    public ResponseEntity<ResponseAPI<PastaArquivoDTO>> navegarPastaSistema(
            @Parameter(description = "Nome da pasta do sistema: colaboradores, servicos, produtos, organizacao, arquivos")
            @PathVariable String nomePasta) {
        try {
            PastaArquivoDTO resultado = arquivoStorageService.navegarPastaSistema(nomePasta);
            return ResponseEntity.ok(
                    ResponseAPI.<PastaArquivoDTO>builder()
                            .success(true)
                            .message("Conteúdo listado com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<PastaArquivoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Navegar subpasta do sistema", description = "Retorna conteudo de uma subpasta do sistema (ex: organizacao/logo, organizacao/banner). Somente leitura.")
    @GetMapping("/navegar/sistema/**")
    public ResponseEntity<ResponseAPI<PastaArquivoDTO>> navegarSubpastaSistema(
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Extrair o caminho apos /navegar/sistema/
            String fullPath = request.getRequestURI();
            String prefix = "/api/v1/arquivos/navegar/sistema/";
            String caminhoCompleto = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());

            // Se contem apenas um segmento (sem /), eh tratado pelo endpoint /{nomePasta}
            if (!caminhoCompleto.contains("/")) {
                PastaArquivoDTO resultado = arquivoStorageService.navegarPastaSistema(caminhoCompleto);
                return ResponseEntity.ok(
                        ResponseAPI.<PastaArquivoDTO>builder()
                                .success(true)
                                .message("Conteúdo listado com sucesso.")
                                .dados(resultado)
                                .build());
            }

            PastaArquivoDTO resultado = arquivoStorageService.navegarSubpastaSistema(caminhoCompleto);
            return ResponseEntity.ok(
                    ResponseAPI.<PastaArquivoDTO>builder()
                            .success(true)
                            .message("Conteúdo listado com sucesso.")
                            .dados(resultado)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<PastaArquivoDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // ==================== STORAGE ====================

    @Operation(summary = "Uso de storage", description = "Retorna informacoes de uso de armazenamento da organizacao")
    @GetMapping("/storage")
    public ResponseEntity<ResponseAPI<StorageUsageDTO>> obterUsoStorage() {
        StorageUsageDTO usage = arquivoStorageService.obterUsoStorage();
        return ResponseEntity.ok(
                ResponseAPI.<StorageUsageDTO>builder()
                        .success(true)
                        .message("Uso de storage obtido com sucesso.")
                        .dados(usage)
                        .build());
    }
}
