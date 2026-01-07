package org.exemplo.bellory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

@Service
public class FileStorageService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    private final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public enum TipoUpload {
        FOTO_PERFIL_COLABORADOR("colaboradores"),
        FOTO_PERFIL_CLIENTE("clientes"),
        FOTO_SERVICO("servicos"),
        FOTO_PRODUTO("produtos");

        private final String pasta;

        TipoUpload(String pasta) {
            this.pasta = pasta;
        }

        public String getPasta() {
            return pasta;
        }
    }

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            System.out.println("✅ Diretório de uploads criado/verificado: " + uploadPath);
            System.out.println("✅ URL base para uploads: " + baseUrl);
        } catch (IOException e) {
            throw new RuntimeException("❌ Não foi possível criar diretório de uploads!", e);
        }
    }

    /**
     * Armazena arquivo e retorna o path relativo
     */
    public String storeFile(MultipartFile file, Long entidadeId, Long organizacaoId, TipoUpload tipo) {
        try {
            validarArquivo(file);

            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);

            // Gerar nome único
            String filename = String.format("%d_%d_%d.%s",
                    organizacaoId,
                    entidadeId,
                    System.currentTimeMillis(),
                    extension
            );

            // Criar estrutura: uploadDir/organizacaoId/tipo/
            Path targetDirectory = Paths.get(uploadDir,
                    organizacaoId.toString(),
                    tipo.getPasta());
            Files.createDirectories(targetDirectory);

            // Salvar arquivo
            Path targetLocation = targetDirectory.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retornar path relativo para construir URL
            return String.format("%d/%s/%s", organizacaoId, tipo.getPasta(), filename);

        } catch (IOException ex) {
            throw new RuntimeException("Erro ao armazenar arquivo", ex);
        }
    }

    /**
     * Método específico para foto de perfil (compatibilidade)
     */
    public String storeProfilePicture(MultipartFile file, Long funcionarioId, Long organizacaoId) {
        return storeFile(file, funcionarioId, organizacaoId, TipoUpload.FOTO_PERFIL_COLABORADOR);
    }

    /**
     * Constrói URL completa a partir do path relativo
     */
    public String getFileUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        return baseUrl + "/" + relativePath;
    }

    /**
     * Extrai path relativo de uma URL completa
     */
    public String getRelativePathFromUrl(String url) {
        if (url == null || !url.startsWith(baseUrl)) {
            return url; // Já é path relativo ou inválido
        }
        return url.replace(baseUrl + "/", "");
    }

    /**
     * Deleta arquivo usando path relativo
     */
    public void deleteFile(String relativePath, Long organizacaoId) {
        try {
            if (relativePath == null || relativePath.isEmpty()) {
                return;
            }

            Path filePath = Paths.get(uploadDir, relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao deletar arquivo", ex);
        }
    }

    /**
     * Verifica se arquivo existe
     */
    public boolean fileExists(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        Path filePath = Paths.get(uploadDir, relativePath).normalize();
        return Files.exists(filePath);
    }

    private void validarArquivo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande. Máximo: 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Formato não permitido. Use: " + ALLOWED_EXTENSIONS);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
