package com.cyrev.nitelestate.reporting;

import com.cyrev.nitelestate.billing.InvoiceRepository;
import com.cyrev.nitelestate.complaint.ComplaintRepository;
import com.cyrev.nitelestate.complaint.ComplaintStatus;
import com.cyrev.nitelestate.payment.PaymentRepository;
import com.cyrev.nitelestate.property.PropertyRepository;
import com.cyrev.nitelestate.reporting.dto.DashboardResponse;
import com.cyrev.nitelestate.resident.ResidentRepository;
import com.cyrev.nitelestate.vehicle.VehicleRepository;
import com.cyrev.nitelestate.visitor.VisitorRepository;
import com.cyrev.nitelestate.visitor.VisitorStatus;
import com.cyrev.nitelestate.worker.WorkerRepository;
import com.cyrev.nitelestate.worker.WorkerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final VehicleRepository vehicleRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final WorkerRepository workerRepository;
    private final VisitorRepository visitorRepository;
    private final ComplaintRepository complaintRepository;

    public DashboardResponse dashboard() {
        BigDecimal totalBilling = invoiceRepository.sumAllIssuedAmount();
        BigDecimal collected = paymentRepository.sumAllSuccessfulAmount();
        BigDecimal outstanding = totalBilling.subtract(collected);
        double collectionRate = totalBilling.signum() == 0 ? 0.0
                : collected.multiply(BigDecimal.valueOf(100))
                        .divide(totalBilling, 1, RoundingMode.HALF_UP).doubleValue();

        long openComplaints = complaintRepository.countByStatus(ComplaintStatus.OPEN)
                + complaintRepository.countByStatus(ComplaintStatus.ASSIGNED)
                + complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);

        return new DashboardResponse(
                residentRepository.count(),
                propertyRepository.count(),
                vehicleRepository.count(),
                totalBilling,
                collected,
                outstanding,
                collectionRate,
                workerRepository.countByStatus(WorkerStatus.ACTIVE),
                visitorRepository.countByStatus(VisitorStatus.ACTIVE),
                openComplaints
        );
    }
}
