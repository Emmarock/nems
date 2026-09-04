package com.cyrev.nitelestate.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    Optional<User> findByResidentId(Long residentId);

    @Query("select u.email from User u")
    List<String> findAllEmails();

    @Query("select u.phone from User u where u.phone is not null")
    List<String> findAllPhones();

    @Query("select u.residentId from User u where u.residentId is not null")
    List<Long> findAllLinkedResidentIds();
}
