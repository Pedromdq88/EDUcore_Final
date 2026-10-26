package com.educore.sge.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    List<AuthSession> findByAccountId(String accountId);
}
