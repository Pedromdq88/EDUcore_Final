package com.educore.sge.institution.infrastructure.repository;
import com.educore.sge.institution.infrastructure.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
    // Hereda todos los métodos CRUD automáticos de Spring Data JPA
}
