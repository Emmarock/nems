package com.nitelestate.reporting;

import com.nitelestate.billing.InvoiceRepository;
import com.nitelestate.complaint.ComplaintRepository;
import com.nitelestate.complaint.ComplaintStatus;
import com.nitelestate.payment.PaymentRepository;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.reporting.dto.DashboardResponse;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.vehicle.VehicleRepository;
import com.nitelestate.visitor.VisitorRepository;
import com.nitelestate.visitor.VisitorStatus;
import com.nitelestate.worker.WorkerRepository;
import com.nitelestate.worker.WorkerStatus;
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
