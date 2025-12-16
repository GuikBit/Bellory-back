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

    private final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    private final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public String storeProfilePicture(MultipartFile file, Long funcionarioId, Long organizacaoId) {
        try {
            // Validações
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

            // Gerar nome único para o arquivo
            String filename = String.format("%d_%d_%d.%s",
                    organizacaoId,
                    funcionarioId,
                    System.currentTimeMillis(),
                    extension
            );

            // Criar subdiretório por organização
            Path orgDirectory = Paths.get(uploadDir, organizacaoId.toString());
            Files.createDirectories(orgDirectory);

            // Salvar arquivo
            Path targetLocation = orgDirectory.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return filename;

        } catch (IOException ex) {
            throw new RuntimeException("Erro ao armazenar arquivo", ex);
        }
    }

    public Resource loadFileAsResource(String filename, Long organizacaoId) {
        try {
            Path filePath = Paths.get(uploadDir, organizacaoId.toString()).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("Arquivo não encontrado: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Arquivo não encontrado: " + filename, ex);
        }
    }

    public void deleteFile(String filename, Long organizacaoId) {
        try {
            Path filePath = Paths.get(uploadDir, organizacaoId.toString()).resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Erro ao deletar arquivo", ex);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
