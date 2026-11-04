package com.nitelestate.facematch;

import com.nitelestate.household.HouseholdMemberRepository;
import com.nitelestate.worker.Worker;
import com.nitelestate.worker.WorkerRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FaceIndexBackfillJobTest {

    private final FaceEngine engine = mock(FaceEngine.class);
    private final FaceIndexEntryRepository entries = mock(FaceIndexEntryRepository.class);
    private final WorkerRepository workers = mock(WorkerRepository.class);
    private final HouseholdMemberRepository members = mock(HouseholdMemberRepository.class);
    private final FaceIndexBackfillRepository backfills = mock(FaceIndexBackfillRepository.class);
    private final FaceIndexBackfillJob job =
            new FaceIndexBackfillJob(engine, new FaceIndexService(engine, entries, workers, members), backfills);

    private static Worker worker(long id, String photo) {
        Worker w = new Worker();
        w.setId(id);
        w.setPhoto(photo);
        return w;
    }

    @Test
    void indexesExistingPhotosOnce_andMarksDone() {
        when(engine.isEnabled()).thenReturn(true);
        when(backfills.count()).thenReturn(0L);
        when(workers.findAll()).thenReturn(List.of(worker(1, "aGVsbG8="), worker(2, null)));
        when(members.findAll()).thenReturn(List.of());
        when(engine.index(any(), anyString())).thenReturn("face-1");

        job.runOnce();

        verify(engine).index(any(), org.mockito.ArgumentMatchers.eq("WORKER_1"));
        verify(backfills).save(any(FaceIndexBackfill.class));
    }

    @Test
    void skipsWhenAlreadyRun() {
        when(engine.isEnabled()).thenReturn(true);
        when(backfills.count()).thenReturn(1L);

        job.runOnce();

        verify(workers, never()).findAll();
    }

    @Test
    void doesNotMarkDone_whenAwsErrors_soNextStartRetries() {
        when(engine.isEnabled()).thenReturn(true);
        when(backfills.count()).thenReturn(0L);
        when(workers.findAll()).thenReturn(List.of(worker(1, "aGVsbG8=")));
        when(members.findAll()).thenReturn(List.of());
        when(engine.index(any(), anyString())).thenThrow(new RuntimeException("AWS down"));

        job.runOnce();

        verify(backfills, never()).save(any());
    }

    @Test
    void doesNothing_whenFaceMatchingIsOff() {
        when(engine.isEnabled()).thenReturn(false);

        job.runOnce();

        verify(backfills, never()).count();
    }
}
