package com.cyrev.nitelestate.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {
    Optional<Vehicle> findByPlateNumberIgnoreCase(String plateNumber);
    boolean existsByPlateNumberIgnoreCase(String plateNumber);
    Optional<Vehicle> findByQrToken(String qrToken);
}
