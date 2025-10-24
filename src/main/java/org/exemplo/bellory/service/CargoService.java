package org.exemplo.bellory.service;

import jakarta.transaction.Transactional;
import org.exemplo.bellory.model.dto.CargoDTO;
import org.exemplo.bellory.model.entity.funcionario.Cargo;
import org.exemplo.bellory.model.entity.servico.Categoria;
import org.exemplo.bellory.model.repository.funcionario.CargoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CargoService {

    private final CargoRepository cargoRepository;

    public CargoService(CargoRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }

    @Transactional
    public CargoDTO createCargo(CargoDTO dto) {
        validar(dto);

        Cargo cargo = new Cargo();
        cargo.setNome(dto.getNome().trim());
        cargo.setDescricao(dto.getDescricao());
        cargo.setDataCriacao(LocalDateTime.now());
        cargo.setAtivo(true);

        cargo = cargoRepository.save(cargo);
        return toDTO(cargo);
    }

    public List<CargoDTO> getAllCargos() {
        return cargoRepository.findAll()
                .stream()
                .filter(Cargo::isAtivo) // apenas ativos
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CargoDTO getCargoById(Long id) {
        Cargo cargo = cargoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cargo com ID " + id + " não encontrado."));
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

        cargo.setAtivo(false); // soft delete
        cargoRepository.save(cargo);
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

        dto.setNome(cargo.getNome());
        dto.setDescricao(cargo.getDescricao());
        // Se seu CargoDTO tiver mais campos (ex.: dataCriacao/ativo), ajuste aqui:
        // dto.setDataCriacao(cargo.getDataCriacao());
        // dto.setAtivo(cargo.isAtivo());
        return dto;
    }
}
