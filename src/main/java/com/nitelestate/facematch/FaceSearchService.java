package com.nitelestate.facematch;

import com.nitelestate.audit.AuditService;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.facematch.dto.FaceSearchResponse;
import com.nitelestate.facematch.dto.FaceSearchResponse.Candidate;
import com.nitelestate.facematch.dto.FaceSearchResponse.FaceResult;
import com.nitelestate.household.HouseholdMemberRepository;
import com.nitelestate.property.Property;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.resident.Resident;
import com.nitelestate.resident.ResidentRepository;
import com.nitelestate.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Searches a CCTV still against registered workers and household members. Every face in the
 * still is cropped and searched on its own (Rekognition only searches the largest face in an
 * image). The uploaded still itself is never stored; each search is audit-logged with who ran
 * it, the incident note, and which people came back.
 *
 * Results are leads for a human to check, not identifications - CCTV is often blurry or angled
 * and face matching makes mistakes, more so on poor footage.
 */
@Service
@RequiredArgsConstructor
public class FaceSearchService {

    private static final int MAX_FACES = 10;
    private static final int MAX_CANDIDATES_PER_FACE = 5;

    private final FaceEngine faceEngine;
    private final FaceMatchProperties props;
    private final WorkerRepository workerRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final ResidentRepository residentRepository;
    private final PropertyRepository propertyRepository;
    private final AuditService auditService;

    public FaceSearchResponse search(String imageDataUri, String note) {
        if (!faceEngine.isEnabled()) {
            throw new BadRequestException("Face matching isn't switched on for this estate yet - an administrator needs to configure it");
        }
        byte[] bytes = ImageData.decode(imageDataUri);
        if (bytes == null) {
            throw new BadRequestException("Please attach an image");
        }
        if (bytes.length > ImageData.MAX_BYTES) {
            throw new BadRequestException("Image is too large - the limit is 5MB. Try a screenshot of the frame instead");
        }
        BufferedImage frame = ImageData.read(bytes);

        List<FaceEngine.Box> boxes = faceEngine.detect(bytes).stream().limit(MAX_FACES).toList();
        List<FaceResult> faces = new ArrayList<>();
        List<String> auditHits = new ArrayList<>();
        for (FaceEngine.Box box : boxes) {
            byte[] crop = ImageData.jpeg(ImageData.crop(frame, box));
            List<FaceEngine.Hit> hits = faceEngine.search(crop, props.minSimilarity(), 20);
            List<Candidate> candidates = resolve(hits);
            candidates.forEach(c -> auditHits.add(c.subjectType().externalId(c.subjectId())
                    + String.format("@%.1f", c.similarity())));
            faces.add(new FaceResult(ImageData.dataUri(crop), candidates));
        }

        auditService.record("FaceSearch", "-", "SEARCH",
                "note=" + (note == null ? "" : note) + "; faces=" + faces.size() + "; matches=" + auditHits);
        return new FaceSearchResponse(faces.size(), faces);
    }

    /** One candidate per person (their best-scoring face), best first; drops faces whose record is gone. */
    private List<Candidate> resolve(List<FaceEngine.Hit> hits) {
        Map<String, FaceEngine.Hit> best = new LinkedHashMap<>();
        for (FaceEngine.Hit h : hits) {
            best.merge(h.externalId(), h, (a, b) -> a.similarity() >= b.similarity() ? a : b);
        }
        List<Candidate> out = new ArrayList<>();
        for (FaceEngine.Hit h : best.values()) {
            Candidate c = toCandidate(h);
            if (c != null) out.add(c);
        }
        return out.stream().sorted(Comparator.comparingDouble(Candidate::similarity).reversed())
                .limit(MAX_CANDIDATES_PER_FACE).collect(Collectors.toList());
    }

    private Candidate toCandidate(FaceEngine.Hit hit) {
        String ext = hit.externalId();
        int sep = ext == null ? -1 : ext.lastIndexOf('_');
        if (sep < 0) return null;
        FaceSubjectType type;
        Long id;
        try {
            type = FaceSubjectType.valueOf(ext.substring(0, sep));
            id = Long.valueOf(ext.substring(sep + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
        return switch (type) {
            case WORKER -> workerRepository.findById(id).map(w -> {
                Resident sponsor = sponsor(w.getSponsorResidentId());
                String role = w.getWorkType() != null ? w.getWorkType() : w.getContractorName();
                return new Candidate(type, id, w.getFullName(), w.getPhone(), w.getPhoto(), w.getStatus().name(), role,
                        sponsor != null ? sponsor.getFullName() : null, sponsor != null ? sponsor.getPhone() : null,
                        houseNumber(w.getSiteId()), hit.similarity());
            }).orElse(null);
            case HOUSEHOLD_MEMBER -> householdMemberRepository.findById(id).map(m -> {
                Resident sponsor = sponsor(m.getSponsorResidentId());
                return new Candidate(type, id, m.getFullName(), m.getPhone(), m.getPhoto(), m.getStatus().name(),
                        m.getRelationship() != null ? m.getRelationship().name() : null,
                        sponsor != null ? sponsor.getFullName() : null, sponsor != null ? sponsor.getPhone() : null,
                        houseNumber(m.getPropertyId()), hit.similarity());
            }).orElse(null);
        };
    }

    private Resident sponsor(Long residentId) {
        return residentId == null ? null : residentRepository.findById(residentId).orElse(null);
    }

    private String houseNumber(Long propertyId) {
        return propertyId == null ? null : propertyRepository.findById(propertyId).map(Property::getHouseNumber).orElse(null);
    }
}
