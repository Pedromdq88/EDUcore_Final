package com.educore.sge.institution.web;

import com.educore.sge.institution.infrastructure.entity.CalendarEvent;
import com.educore.sge.institution.infrastructure.repository.CalendarEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/calendar/events")
@CrossOrigin(origins = "*")

public class CalendarEventController {

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @GetMapping
    public ResponseEntity<List<CalendarEvent>> getAllEvents(@RequestParam(required = false) String section) {
        List<CalendarEvent> events;
        if (section != null && !section.equalsIgnoreCase("TODAS")) {
            events = calendarEventRepository.findBySection(section);
        } else {
            events = calendarEventRepository.findAll();
        }
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<CalendarEvent> createEvent(@RequestBody CalendarEvent event) {
        CalendarEvent savedEvent = calendarEventRepository.save(event);
        return ResponseEntity.ok(savedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (calendarEventRepository.existsById(id)) {
            calendarEventRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}