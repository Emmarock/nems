package com.nitelestate.facematch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaceIndexEntryRepository extends JpaRepository<FaceIndexEntry, Long> {
    List<FaceIndexEntry> findBySubjectTypeAndSubjectId(FaceSubjectType subjectType, Long subjectId);
}
