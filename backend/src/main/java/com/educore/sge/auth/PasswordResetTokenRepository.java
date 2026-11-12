package com.educore.sge.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PasswordResetToken r where r.userId = :userId")
    Optional<PasswordResetToken> lockByUserId(@Param("userId") String userId);
}
