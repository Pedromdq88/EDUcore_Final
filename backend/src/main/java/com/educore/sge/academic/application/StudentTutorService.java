package com.educore.sge.academic.application;

import com.educore.sge.academic.infrastructure.entity.StudentJpaEntity;
import com.educore.sge.academic.infrastructure.entity.TutorJpaEntity;
import com.educore.sge.academic.infrastructure.repository.StudentJpaRepository;
import com.educore.sge.academic.infrastructure.repository.TutorJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentTutorService {

    private final StudentJpaRepository studentRepository;
    private final TutorJpaRepository tutorRepository;

    public StudentTutorService(StudentJpaRepository studentRepository, TutorJpaRepository tutorRepository) {
        this.studentRepository = studentRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public void linkTutorToStudent(String studentId, String tutorId) {
        StudentJpaEntity student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

        TutorJpaEntity tutor = tutorRepository.findById(tutorId)
            .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        student.addTutor(tutor);
        studentRepository.save(student);
    }
}
