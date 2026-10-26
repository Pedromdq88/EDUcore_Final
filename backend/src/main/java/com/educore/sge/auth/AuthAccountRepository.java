package com.educore.sge.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, String> {
    Optional<AuthAccount> findByTenantIdAndEmailIgnoreCase(String tenantId, String email);
    Optional<AuthAccount> findByTenantIdAndPersonTypeAndPersonId(String tenantId, String personType, String personId);
    Optional<AuthAccount> findByResetTokenHash(String resetTokenHash);
    boolean existsByTenantId(String tenantId);
}
