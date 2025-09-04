package org.exemplo.bellory.service;

import org.exemplo.bellory.model.dto.CargoDTO;
import org.exemplo.bellory.model.repository.categoria.CategoriaRepository;
import org.exemplo.bellory.model.repository.funcionario.CargoRepository;

public class CargoService {

    private final CargoRepository cargoRepository;

    public CargoService(CargoRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }


    public CargoDTO createCargo(CargoDTO cargo) {

    }
}
