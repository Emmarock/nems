package com.nitelestate.announcement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** What a tenant's own announcement list is scoped to — see AnnouncementService.search. */
    Page<Announcement> findByIncludeTenantsTrue(Pageable pageable);

    long countByIncludeTenantsTrue();
}
