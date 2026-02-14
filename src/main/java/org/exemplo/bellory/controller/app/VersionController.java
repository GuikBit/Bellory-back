package org.exemplo.bellory.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/version")
@Tag(name = "Versao", description = "Informacoes de versao da API")
public class VersionController {

    private final BuildProperties buildProperties;

    public VersionController(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties.orElse(null);
    }

    @GetMapping
    @Operation(summary = "Retorna informacoes de versao da API")
    public Map<String, String> getVersion() {
        return Map.of(
                "name", buildProperties != null ? buildProperties.getName() : "Bellory",
                "version", buildProperties != null ? buildProperties.getVersion() : "dev",
                "build_time", buildProperties != null && buildProperties.getTime() != null
                        ? buildProperties.getTime().toString()
                        : "N/A"
        );
    }
}
