package com.nitelestate.facematch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * One-time job: the first time the app starts with face matching switched on, it indexes every
 * worker and household photo that was registered before then. Runs in the background so startup
 * isn't held up, and records a FaceIndexBackfill row when done so later restarts skip it. New and
 * edited photos are indexed as they're saved (FaceIndexService.syncAsync), so nothing else needs it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaceIndexBackfillJob {

    private final FaceEngine faceEngine;
    private final FaceIndexService faceIndexService;
    private final FaceIndexBackfillRepository backfillRepository;

    @Async("notificationExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void runOnce() {
        if (!faceEngine.isEnabled() || backfillRepository.count() > 0) {
            return;
        }
        log.info("Face index backfill starting");
        FaceIndexService.BackfillResult result = faceIndexService.indexMissing();
        if (result.failed() > 0) {
            // Leave the marker unset so the next restart retries the ones that errored (e.g. AWS was down).
            log.warn("Face index backfill: {} of {} photos indexed, {} failed - will retry on next start",
                    result.indexed(), result.total(), result.failed());
            return;
        }
        FaceIndexBackfill done = new FaceIndexBackfill();
        done.setIndexed(result.indexed());
        done.setTotal(result.total());
        done.setCompletedAt(java.time.Instant.now());
        backfillRepository.save(done);
        log.info("Face index backfill finished: {} of {} photos indexed", result.indexed(), result.total());
    }
}
