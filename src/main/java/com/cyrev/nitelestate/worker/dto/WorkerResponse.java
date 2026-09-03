package com.cyrev.nitelestate.worker.dto;

import com.cyrev.nitelestate.worker.Worker;
import com.cyrev.nitelestate.worker.WorkerStatus;

import java.time.LocalDate;

public record WorkerResponse(
        Long id,
        String fullName,
        String phone,
        String nationalId,
        String contractorName,
        String workType,
        Long siteId,
        String siteHouseNumber,
        Long sponsorResidentId,
        LocalDate startDate,
        LocalDate expectedEndDate,
        WorkerStatus status,
        String qrToken,
        String photo
) {
    public static WorkerResponse from(Worker w, String siteHouseNumber) {
        return new WorkerResponse(w.getId(), w.getFullName(), w.getPhone(), w.getNationalId(), w.getContractorName(),
                w.getWorkType(), w.getSiteId(), siteHouseNumber, w.getSponsorResidentId(), w.getStartDate(),
                w.getExpectedEndDate(), w.getStatus(), w.getQrToken(), w.getPhoto());
    }
}
