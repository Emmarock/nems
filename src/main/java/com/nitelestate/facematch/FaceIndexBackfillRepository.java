package com.nitelestate.facematch;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FaceIndexBackfillRepository extends JpaRepository<FaceIndexBackfill, Long> {
}
