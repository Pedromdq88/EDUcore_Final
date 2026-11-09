package com.educore.sge.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, String> {
    Optional<AuthUser> findByTenantIdAndEmailIgnoreCase(String tenantId, String email);
    Optional<AuthUser> findByTenantIdAndTutorId(String tenantId, String tutorId);
    Optional<AuthUser> findByTenantIdAndStaffId(String tenantId, String staffId);
}
