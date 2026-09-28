package com.educore.sge.kindergarten.infrastructure.entity;

import com.educore.sge.kindergarten.domain.BirthCountry;
import com.educore.sge.kindergarten.domain.DniStatus;
import com.educore.sge.kindergarten.domain.GenderIdentity;
import com.educore.sge.shared.BaseInstitutionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "students")
public class StudentJpaEntity extends BaseInstitutionEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "legajo_number")
    private String legajoNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "classroom")
    private String classroom;

    @Column(name = "gender")
    private String gender;

    @Column(name = "blood_type")
    private String bloodType;

    @Column(name = "health_insurance")
    private String healthInsurance;

    @Column(name = "allergies")
    private String allergies;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "address")
    private String address;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear = LocalDate.now().getYear();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    // ========================================================
    // 1. IDENTIDAD Y DOCUMENTACIÓN LEGAL (PLANILLA OFICIAL)
    // ========================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "dni_status", length = 20)
    private DniStatus dniStatus;

    @Column(name = "cuil", length = 15)
    private String cuil;

    @Column(name = "has_cpi")
    private Boolean hasCpi;

    @Column(name = "has_foreign_document")
    private Boolean hasForeignDocument;

    @Column(name = "foreign_document_type", length = 50)
    private String foreignDocumentType;

    @Column(name = "foreign_document_number", length = 50)
    private String foreignDocumentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_identity", length = 30)
    private GenderIdentity genderIdentity;

    // ========================================================
    // 2. ORIGEN Y NACIMIENTO
    // ========================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "birth_country", length = 20)
    private BirthCountry birthCountry;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Column(name = "birth_province", length = 100)
    private String birthProvince;

    @Column(name = "birth_district", length = 100)
    private String birthDistrict;

    @Column(name = "birth_locality", length = 100)
    private String birthLocality;

    // ========================================================
    // 3. DOMICILIO ESTRUCTURADO Y CONTACTO
    // ========================================================
    @Column(name = "address_street")
    private String street;

    @Column(name = "address_number", length = 20)
    private String streetNumber;

    @Column(name = "address_floor", length = 10)
    private String floor;

    @Column(name = "address_tower", length = 20)
    private String tower;

    @Column(name = "address_apartment", length = 10)
    private String apartment;

    @Column(name = "address_between_streets")
    private String betweenStreets;

    @Column(name = "address_other_details")
    private String otherAddressDetails;

    @Column(name = "address_district", length = 100)
    private String addressDistrict;

    @Column(name = "address_locality", length = 100)
    private String addressLocality;

    @Column(name = "student_landline_phone", length = 30)
    private String studentLandlinePhone;

    @Column(name = "student_cellphone", length = 30)
    private String studentCellphone;

    // ========================================================
    // 4. OTROS DATOS (SOCIODEMOGRÁFICOS Y ESCOLARES)
    // ========================================================
    @Column(name = "has_siblings")
    private Boolean hasSiblings;

    @Column(name = "sibling_count")
    private Integer siblingCount;

    @Column(name = "siblings_in_this_school")
    private Integer siblingsInThisSchool;

    @Column(name = "speaks_other_language_at_home")
    private Boolean speaksOtherLanguageAtHome;

    @Column(name = "indigenous_language")
    private Boolean indigenousLanguage;

    @Column(name = "other_language")
    private Boolean otherLanguage;

    @Column(name = "belongs_to_native_people")
    private Boolean belongsToNativePeople;

    @Column(name = "receives_auh")
    private Boolean receivesAuh;

    @Column(name = "transportation_methods")
    private String transportationMethods; // Ej: "COLECTIVO,BICICLETA"

    // ========================================================
    // RELACIONES
    // ========================================================
    @ManyToMany
    @JoinTable(
            name = "student_tutors",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "tutor_id")
    )
    @ToString.Exclude
    private List<TutorJpaEntity> tutors = new ArrayList<>();

    public void addTutor(TutorJpaEntity tutor) {
        if (tutor != null && !this.tutors.contains(tutor)) {
            this.tutors.add(tutor);
        }
    }
}