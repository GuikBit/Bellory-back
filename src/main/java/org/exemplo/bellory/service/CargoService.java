package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.dto.CargoDTO;
import org.exemplo.bellory.model.entity.funcionario.Cargo;
import org.exemplo.bellory.model.entity.organizacao.Organizacao;
import org.exemplo.bellory.model.repository.funcionario.CargoRepository;
import org.exemplo.bellory.model.repository.organizacao.OrganizacaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;
    private final OrganizacaoRepository organizacaoRepository;

    public CargoService(CargoRepository cargoRepository, OrganizacaoRepository organizacaoRepository) {
        this.cargoRepository = cargoRepository;
        this.organizacaoRepository = organizacaoRepository;
    }

    @Transactional
    public CargoDTO createCargo(CargoDTO dto) {
        validar(dto);
        Long organizacaoId = getOrganizacaoIdFromContext();

        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização com ID " + organizacaoId + " não encontrada."));

        Cargo cargo = new Cargo();
        cargo.setOrganizacao(organizacao);
        cargo.setNome(dto.getNome().trim());
        cargo.setDescricao(dto.getDescricao());
        cargo.setDataCriacao(LocalDateTime.now());
        cargo.setAtivo(true);

        cargo = cargoRepository.save(cargo);
        return toDTO(cargo);
    }

    public List<CargoDTO> getAllCargos() {
        Long organizacaoId = getOrganizacaoIdFromContext();

        return cargoRepository.findAllByOrganizacao_IdAndAtivoTrue(organizacaoId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private CargoDTO convertToDTO(Cargo cargo){
        return CargoDTO.builder()
                .id(cargo.getId())
                .nome(cargo.getNome())
                .descricao(cargo.getDescricao())
                .ativo(cargo.isAtivo())
                .build();
    }

    public CargoDTO getCargoById(Long id) {
        Cargo cargo = cargoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cargo com ID " + id + " não encontrado."));

        validarOrganizacao(cargo.getOrganizacao().getId());

        if (!cargo.isAtivo()) {
            throw new NoSuchElementException("Cargo com ID " + id + " não encontrado.");
        }
        return toDTO(cargo);
    }

    @Transactional
    public CargoDTO updateCargo(Long id, CargoDTO dto) {
        validar(dto);

        Cargo cargo = cargoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cargo com ID " + id + " não encontrado."));

        validarOrganizacao(cargo.getOrganizacao().getId());

        if (!cargo.isAtivo()) {
            throw new NoSuchElementException("Cargo com ID " + id + " não encontrado.");
        }

        cargo.setNome(dto.getNome().trim());
        cargo.setDescricao(dto.getDescricao());

        cargo = cargoRepository.save(cargo);
        return toDTO(cargo);
    }

    @Transactional
    public void deleteCargo(Long id) {
        Cargo cargo = cargoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cargo com ID " + id + " não encontrado."));

        validarOrganizacao(cargo.getOrganizacao().getId());

        cargo.setAtivo(false); // soft delete
        cargoRepository.save(cargo);
    }

    // --------------------
    // Métodos de Validação
    // --------------------

    private void validarOrganizacao(Long entityOrganizacaoId) {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada no token");
        }

        if (!organizacaoId.equals(entityOrganizacaoId)) {
            throw new SecurityException("Acesso negado: Você não tem permissão para acessar este recurso");
        }
    }

    private Long getOrganizacaoIdFromContext() {
        Long organizacaoId = TenantContext.getCurrentOrganizacaoId();

        if (organizacaoId == null) {
            throw new SecurityException("Organização não identificada. Token inválido ou expirado");
        }

        return organizacaoId;
    }

    // --------------------
    // Helpers
    // --------------------

    private void validar(CargoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dados do cargo não informados.");
        }
        if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do cargo é obrigatório.");
        }
        if (dto.getNome().trim().length() > 100) {
            throw new IllegalArgumentException("O nome do cargo deve ter no máximo 100 caracteres.");
        }
        if (dto.getDescricao() != null && dto.getDescricao().length() > 500) {
            throw new IllegalArgumentException("A descrição do cargo deve ter no máximo 500 caracteres.");
        }
    }

    private CargoDTO toDTO(Cargo cargo) {
        CargoDTO dto = new CargoDTO();
        dto.setId(cargo.getId());
        dto.setNome(cargo.getNome());
        dto.setDescricao(cargo.getDescricao());
        dto.setAtivo(cargo.isAtivo());
        return dto;
    }
}
