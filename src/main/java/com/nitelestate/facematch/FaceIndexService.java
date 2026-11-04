package com.nitelestate.facematch;

import com.nitelestate.household.HouseholdMemberRepository;
import com.nitelestate.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Keeps the face collection in step with registered photos. Indexing runs off the request
 * thread and never fails the save that triggered it - a worker registration must not break
 * because AWS is slow or down.
 *
 * People stay indexed after their pass ends (completed/suspended workers, revoked household
 * members): footage often surfaces later, and former workers are exactly who it needs to match.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceIndexService {

    private final FaceEngine faceEngine;
    private final FaceIndexEntryRepository entryRepository;
    private final WorkerRepository workerRepository;
    private final HouseholdMemberRepository householdMemberRepository;

    /** Replaces this person's indexed face with the given photo (null photo = just remove it). */
    @Async("notificationExecutor")
    public void syncAsync(FaceSubjectType type, Long subjectId, String photoDataUri) {
        sync(type, subjectId, photoDataUri);
    }

    public boolean sync(FaceSubjectType type, Long subjectId, String photoDataUri) {
        return syncOutcome(type, subjectId, photoDataUri) == Outcome.INDEXED;
    }

    enum Outcome { INDEXED, NO_FACE, FAILED, SKIPPED }

    private Outcome syncOutcome(FaceSubjectType type, Long subjectId, String photoDataUri) {
        if (!faceEngine.isEnabled() || subjectId == null) {
            return Outcome.SKIPPED;
        }
        try {
            List<FaceIndexEntry> existing = entryRepository.findBySubjectTypeAndSubjectId(type, subjectId);
            faceEngine.delete(existing.stream().map(FaceIndexEntry::getFaceId).toList());
            entryRepository.deleteAll(existing);

            byte[] image = ImageData.decode(photoDataUri);
            if (image == null) {
                return Outcome.SKIPPED;
            }
            String faceId = faceEngine.index(image, type.externalId(subjectId));
            if (faceId == null) {
                log.info("No usable face in {} {}'s photo - not indexed", type, subjectId);
                return Outcome.NO_FACE;
            }
            FaceIndexEntry entry = new FaceIndexEntry();
            entry.setSubjectType(type);
            entry.setSubjectId(subjectId);
            entry.setFaceId(faceId);
            entryRepository.save(entry);
            return Outcome.INDEXED;
        } catch (RuntimeException e) {
            log.warn("Face indexing failed for {} {}: {}", type, subjectId, e.getMessage());
            return Outcome.FAILED;
        }
    }

    public record BackfillResult(int indexed, int total, int failed) {
    }

    /**
     * Indexes every worker and household photo that isn't indexed yet - used by the one-time
     * FaceIndexBackfillJob. Already-indexed people are skipped, so a retry only redoes what's missing.
     */
    public BackfillResult indexMissing() {
        int indexed = 0, total = 0, failed = 0;
        for (var w : workerRepository.findAll()) {
            if (w.getPhoto() == null || !entryRepository.findBySubjectTypeAndSubjectId(FaceSubjectType.WORKER, w.getId()).isEmpty()) {
                continue;
            }
            total++;
            Outcome o = syncOutcome(FaceSubjectType.WORKER, w.getId(), w.getPhoto());
            if (o == Outcome.INDEXED) indexed++;
            if (o == Outcome.FAILED) failed++;
        }
        for (var m : householdMemberRepository.findAll()) {
            if (m.getPhoto() == null || !entryRepository.findBySubjectTypeAndSubjectId(FaceSubjectType.HOUSEHOLD_MEMBER, m.getId()).isEmpty()) {
                continue;
            }
            total++;
            Outcome o = syncOutcome(FaceSubjectType.HOUSEHOLD_MEMBER, m.getId(), m.getPhoto());
            if (o == Outcome.INDEXED) indexed++;
            if (o == Outcome.FAILED) failed++;
        }
        return new BackfillResult(indexed, total, failed);
    }
}
