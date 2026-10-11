package com.cyrev.nitelestate.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {
    Optional<Vehicle> findByPlateNumberIgnoreCase(String plateNumber);
    boolean existsByPlateNumberIgnoreCase(String plateNumber);
    Optional<Vehicle> findByQrToken(String qrToken);

    /** Caps self-service registration per tenant (see VehicleService.registerForResident) - counts every status, since nothing in this app ever removes a vehicle. */
    long countByResidentId(Long residentId);

    /** Caps how many of a tenant's vehicles a landlord can approve into ACTIVE (see VehicleService.approve). */
    long countByResidentIdAndStatus(Long residentId, VehicleStatus status);
}
