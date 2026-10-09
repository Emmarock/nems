package com.cyrev.nitelestate.household;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long>, JpaSpecificationExecutor<HouseholdMember> {
    Optional<HouseholdMember> findByQrToken(String qrToken);
}
