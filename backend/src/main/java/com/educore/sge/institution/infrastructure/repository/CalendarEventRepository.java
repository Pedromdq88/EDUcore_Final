package com.educore.sge.institution.infrastructure.repository;

import com.educore.sge.institution.infrastructure.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findBySection(String section);
}