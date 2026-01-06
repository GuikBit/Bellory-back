package org.exemplo.bellory.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
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

    private final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // Enum para tipos de upload
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

    /**
     * Método genérico para armazenar qualquer tipo de imagem
     */
    public String storeFile(MultipartFile file, Long entidadeId, Long organizacaoId, TipoUpload tipo) {
        try {
            // Validações
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

            return filename;

        } catch (IOException ex) {
            throw new RuntimeException("Erro ao armazenar arquivo", ex);
        }
    }

    /**
     * Método específico para foto de perfil (mantém compatibilidade)
     */
    public String storeProfilePicture(MultipartFile file, Long funcionarioId, Long organizacaoId) {
        return storeFile(file, funcionarioId, organizacaoId, TipoUpload.FOTO_PERFIL_COLABORADOR);
    }

    /**
     * Carregar arquivo como Resource
     */
    public Resource loadFileAsResource(String filename, Long organizacaoId, TipoUpload tipo) {
        try {
            Path filePath = Paths.get(uploadDir,
                            organizacaoId.toString(),
                            tipo.getPasta())
                    .resolve(filename)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Arquivo não encontrado ou não legível: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Arquivo não encontrado: " + filename, ex);
        }
    }

    /**
     * Deletar arquivo
     */
    public void deleteFile(String filename, Long organizacaoId, TipoUpload tipo) {
        try {
            Path filePath = Paths.get(uploadDir,
                            organizacaoId.toString(),
                            tipo.getPasta())
                    .resolve(filename)
                    .normalize();

            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao deletar arquivo", ex);
        }
    }

    /**
     * Validações centralizadas
     */
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

    /**
     * Método utilitário para determinar Content-Type
     */
    public String getContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();

        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }
}
