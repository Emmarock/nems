package com.cyrev.nitelestate.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccessEventRepository extends JpaRepository<AccessEvent, Long>, JpaSpecificationExecutor<AccessEvent> {
}
