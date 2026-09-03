package com.cyrev.nitelestate.estatesecurity;

import com.cyrev.nitelestate.accesspolicy.AccessPolicyService;
import com.cyrev.nitelestate.accesspolicy.dto.AccessPolicyResponse;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.vehicle.VehicleRepository;
import com.cyrev.nitelestate.visitor.VisitorRepository;
import com.cyrev.nitelestate.visitor.VisitorStatus;
import com.cyrev.nitelestate.worker.WorkerRepository;
import com.cyrev.nitelestate.worker.WorkerStatus;
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
