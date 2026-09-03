package com.cyrev.nitelestate.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("select u.email from User u")
    List<String> findAllEmails();

    @Query("select u.residentId from User u where u.residentId is not null")
    List<Long> findAllLinkedResidentIds();
}
