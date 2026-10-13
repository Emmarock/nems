package com.nitelestate.estatesecurity;

import com.nitelestate.accesspolicy.AccessPolicyService;
import com.nitelestate.accesspolicy.dto.AccessPolicyResponse;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.vehicle.VehicleRepository;
import com.nitelestate.visitor.VisitorRepository;
import com.nitelestate.visitor.VisitorStatus;
import com.nitelestate.worker.WorkerRepository;
import com.nitelestate.worker.WorkerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityDashboardService {

    private final VisitorRepository visitorRepository;
    private final WorkerRepository workerRepository;
    private final VehicleRepository vehicleRepository;
    private final ResidentRepository residentRepository;
    private final AccessPolicyService accessPolicyService;

    public SecurityDashboardResponse dashboard() {
        AccessPolicyResponse settings = accessPolicyService.getSettings();
        long accountsInArrears = settings.enforceArrears()
                ? residentRepository.countResidentsWithOutstandingAbove(settings.arrearsThreshold())
                : 0;

        return new SecurityDashboardResponse(
                visitorRepository.countByStatus(VisitorStatus.ACTIVE),
                workerRepository.countByStatus(WorkerStatus.ACTIVE),
                vehicleRepository.count(),
                accountsInArrears
        );
    }
}
