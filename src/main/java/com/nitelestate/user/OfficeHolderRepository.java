package com.nitelestate.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OfficeHolderRepository extends JpaRepository<OfficeHolder, Long> {
    Optional<OfficeHolder> findByRole(Role role);
    List<OfficeHolder> findAllByRoleIn(Collection<Role> roles);
}
