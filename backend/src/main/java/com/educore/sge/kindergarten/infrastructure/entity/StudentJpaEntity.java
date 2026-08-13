package com.educore.sge.kindergarten.infrastructure.entity;

import com.educore.sge.shared.BaseInstitutionEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
public class StudentJpaEntity extends BaseInstitutionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "document_number", nullable = false)
    private String documentNumber; // Actúa como DNI

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "gender")
    private String gender;

    @Column(name = "legajo_number")
    private String legajoNumber;

    @Column(name = "classroom")
    private String classroom; // Nombre o ID de la Sala

    @Column(name = "blood_type")
    private String bloodType;

    @Column(name = "health_insurance")
    private String healthInsurance;

    @Column(name = "allergies")
    private String allergies;

    // ---- NUEVOS CAMPOS INCORPORADOS ----
    @Column(name = "lugar_nacimiento")
    private String lugarNacimiento;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "telefono_contacto")
    private String telefonoContacto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    @ManyToMany
    @JoinTable(
        name = "student_tutors",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "tutor_id")
    )
    private List<TutorJpaEntity> tutors = new ArrayList<>();
    public StudentJpaEntity() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getLegajoNumber() { return legajoNumber; }
    public void setLegajoNumber(String legajoNumber) { this.legajoNumber = legajoNumber; }

    public String getClassroom() { return classroom; }
    public void setClassroom(String classroom) { this.classroom = classroom; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getHealthInsurance() { return healthInsurance; }
    public void setHealthInsurance(String healthInsurance) { this.healthInsurance = healthInsurance; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getLugarNacimiento() { return lugarNacimiento; }
    public void setLugarNacimiento(String lugarNacimiento) { this.lugarNacimiento = lugarNacimiento; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }

    public List<TutorJpaEntity> getTutors() { return tutors; }
    public void setTutors(List<TutorJpaEntity> tutors) { this.tutors = tutors; }


    // Método de vinculación lógica para soportar uno o más tutores
    public void addTutor(TutorJpaEntity tutor) {
        if (!this.tutors.contains(tutor)) {
            this.tutors.add(tutor);
        }
    }

    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus status) { this.status = status; }
}
