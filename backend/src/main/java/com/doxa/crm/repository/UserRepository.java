package com.doxa.crm.repository;

import com.doxa.crm.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u JOIN FETCH u.license WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.license.id = :licenseId ORDER BY u.email")
    List<User> findByLicenseId(@Param("licenseId") UUID licenseId);

    Optional<User> findByIdAndLicenseId(UUID id, UUID licenseId);
}
