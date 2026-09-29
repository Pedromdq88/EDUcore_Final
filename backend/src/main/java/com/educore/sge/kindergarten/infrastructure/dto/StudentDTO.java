package com.educore.sge.kindergarten.infrastructure.dto; // O el paquete donde ubiques tus DTOs

import com.educore.sge.kindergarten.domain.BirthCountry;
import com.educore.sge.kindergarten.domain.DniStatus;
import com.educore.sge.kindergarten.domain.GenderIdentity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {

    private String id;
    private String legajoNumber;
    private String firstName;
    private String lastName;
    private String documentNumber;
    private LocalDate birthDate;
    private String classroom;
    private String gender;
    private String bloodType;
    private String healthInsurance;
    private String allergies;
    private String birthPlace;
    private String address;
    private String contactPhone;
    private Integer academicYear;
    private String status;

    // ========================================================
    // 1. IDENTIDAD Y DOCUMENTACIÓN LEGAL
    // ========================================================
    private DniStatus dniStatus;
    private String cuil;
    private Boolean hasCpi;
    private Boolean hasForeignDocument;
    private String foreignDocumentType;
    private String foreignDocumentNumber;
    private GenderIdentity genderIdentity;

    // ========================================================
    // 2. ORIGEN Y NACIMIENTO
    // ========================================================
    private BirthCountry birthCountry;
    private String nationality;
    private String birthProvince;
    private String birthDistrict;
    private String birthLocality;

    // ========================================================
    // 3. DOMICILIO ESTRUCTURADO Y CONTACTO
    // ========================================================
    private String street;
    private String streetNumber;
    private String floor;
    private String tower;
    private String apartment;
    private String betweenStreets;
    private String otherAddressDetails;
    private String addressDistrict;
    private String addressLocality;
    private String studentLandlinePhone;
    private String studentCellphone;

    // ========================================================
    // 4. OTROS DATOS (SOCIODEMOGRÁFICOS Y ESCOLAres)
    // ========================================================
    private Boolean hasSiblings;
    private Integer siblingCount;
    private Integer siblingsInThisSchool;

    private Boolean speaksOtherLanguageAtHome;
    private Boolean indigenousLanguage;
    private Boolean otherLanguage;

    private Boolean belongsToNativePeople;
    private Boolean receivesAuh;
    private String transportationMethods;
}