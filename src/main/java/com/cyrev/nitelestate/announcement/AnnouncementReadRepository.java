package com.cyrev.nitelestate.announcement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);

    void deleteByAnnouncementIdAndUserId(Long announcementId, Long userId);

    long countByUserId(Long userId);

    @Query("select ar.announcementId from AnnouncementRead ar where ar.userId = :userId")
    List<Long> findAnnouncementIdsByUserId(Long userId);
}
