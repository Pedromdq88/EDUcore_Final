package com.educore.sge.kindergarten.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorHistoryRepository extends JpaRepository<TutorHistoryJpaEntity, String> {
    // Hereda .save() para la tabla 'historial_tutores_baja'
}
