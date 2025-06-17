package org.exemplo.bellory.controller;

import lombok.Getter;
import lombok.Setter;
import org.exemplo.bellory.model.entity.LandingPage;
import org.exemplo.bellory.model.entity.Section;
import org.exemplo.bellory.model.repository.LandingPageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import adicionado
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// --- DTOs (Data Transfer Objects) ---
// Estes podem ser record ou classes normais. Podem até ficar em ficheiros separados.

@Getter
@Setter
class LandingPageDto {
    private String slug;
    private String internalTitle;
    private List<SectionDto> sections;
}

@Getter
@Setter
class SectionDto {
    private String sectionType;
    private int displayOrder;
    // A mágica acontece aqui: transformamos a lista de blocos num mapa para fácil acesso no front-end.
    private Map<String, String> content;
}


// --- Service ---
// A lógica de negócio fica aqui.

@Service
class LandingPageService {

    private final LandingPageRepository repository;

    public LandingPageService(LandingPageRepository repository) {
        this.repository = repository;
    }

    // CORREÇÃO: Adicionada a anotação @Transactional.
    // Isto garante que todas as operações de busca ocorram dentro de uma transação,
    // o que é necessário para aceder a campos @Lob e a coleções lazy-loaded.
    @Transactional(readOnly = true)
    public LandingPageDto getPageBySlug(String slug) {
        // Encontra a página ou lança uma exceção (aqui simplificado)
        LandingPage page = repository.findBySlug(slug).orElse(null);
        if (page == null) {
            return null;
        }
        return mapToDto(page);
    }

    private LandingPageDto mapToDto(LandingPage page) {
        LandingPageDto pageDto = new LandingPageDto();
        pageDto.setSlug(page.getSlug());
        pageDto.setInternalTitle(page.getInternalTitle());
        pageDto.setSections(page.getSections().stream().map(this::mapSectionToDto).collect(Collectors.toList()));
        return pageDto;
    }

    private SectionDto mapSectionToDto(Section section) {
        SectionDto sectionDto = new SectionDto();
        sectionDto.setSectionType(section.getSectionType());
        sectionDto.setDisplayOrder(section.getDisplayOrder());

        // Transforma a List<ContentBlock> num Map<String, String>
        Map<String, String> contentMap = section.getContentBlocks().stream()
                .collect(Collectors.toMap(
                        contentBlock -> contentBlock.getContentKey(),
                        contentBlock -> contentBlock.getContentValue()
                ));
        sectionDto.setContent(contentMap);
        return sectionDto;
    }
}


// --- Controller ---
// Expõe o endpoint público.

@RestController
@RequestMapping("/api/pages")
public class LandingPageController {

    private final LandingPageService service;

    public LandingPageController(LandingPageService service) {
        this.service = service;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<LandingPageDto> getPageBySlug(@PathVariable String slug) {
        LandingPageDto page = service.getPageBySlug(slug);
        if (page != null) {
            return ResponseEntity.ok(page);
        }
        return ResponseEntity.notFound().build();
    }
}
