-- ===================================================================================
-- 0. CONFIGURACIÓN INICIAL Y LIMPIEZA TOTAL
-- ===================================================================================
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS student_judicial_restrictions;
DROP TABLE IF EXISTS student_authorized_pickups;
DROP TABLE IF EXISTS student_fees;
DROP TABLE IF EXISTS student_tutors;
DROP TABLE IF EXISTS institution_staff;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS tutors;
DROP TABLE IF EXISTS institution_settings;
DROP TABLE IF EXISTS tenants;

SET FOREIGN_KEY_CHECKS = 1;

-- ===================================================================================
-- 1. TABLA: TENANTS (INSTITUCIÓN / COLEGIO)
-- ===================================================================================
CREATE TABLE tenants (
                         id VARCHAR(36) NOT NULL,
                         name VARCHAR(150) NOT NULL,
                         cue VARCHAR(50) NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 2. TABLA: CONFIGURACIONES INSTITUCIONALES (EMAILS Y CONTACTO)
-- ===================================================================================
CREATE TABLE institution_settings (
                                      id VARCHAR(36) NOT NULL,
                                      tenant_id VARCHAR(36) NOT NULL,
                                      receipt_email VARCHAR(150) NULL,
                                      fee_query_email VARCHAR(150) NULL,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      PRIMARY KEY (id),
                                      UNIQUE KEY uq_settings_tenant (tenant_id),
                                      CONSTRAINT fk_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 3. TABLA: ALUMNOS (STUDENTS) - INCLUYE LEGAJO ADMINISTRATIVO EDITABLE
-- ===================================================================================
CREATE TABLE students (
                          id VARCHAR(36) NOT NULL,                      -- UUID Técnico del sistema
                          tenant_id VARCHAR(36) NOT NULL,
                          legajo_number VARCHAR(50) NULL,               -- N° de Legajo Administrativo (editable)
                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100) NOT NULL,
                          document_number VARCHAR(50) NOT NULL,
                          birth_date DATE NOT NULL,
                          classroom VARCHAR(100) NULL,                  -- Maternal, Sala de 2 y 3, Sala de 4, Sala de 5
                          gender VARCHAR(20) NULL,
                          blood_type VARCHAR(10) NULL,
                          health_insurance VARCHAR(150) NULL,
                          allergies TEXT NULL,
                          birth_place VARCHAR(150) NULL,
                          address VARCHAR(255) NULL,
                          contact_phone VARCHAR(50) NULL,
                          status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, HISTORIC
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          PRIMARY KEY (id),
                          INDEX idx_students_tenant (tenant_id),
                          INDEX idx_students_dni (document_number),
                          INDEX idx_students_legajo (legajo_number),
                          INDEX idx_students_classroom (classroom),
                          CONSTRAINT fk_students_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 4. TABLA: TUTORES / PADRES (TUTORS)
-- ===================================================================================
CREATE TABLE tutors (
                        id VARCHAR(36) NOT NULL,
                        tenant_id VARCHAR(36) NOT NULL,
                        first_name VARCHAR(100) NOT NULL,
                        last_name VARCHAR(100) NOT NULL,
                        document_number VARCHAR(50) NOT NULL,
                        relationship VARCHAR(50) NOT NULL,           -- MADRE, PADRE, TUTOR_LEGAL
                        nationality VARCHAR(100) DEFAULT 'Argentina',
                        occupation VARCHAR(150) NULL,
                        activity_status VARCHAR(50) NULL,            -- Trabaja, Estudia, Busca trabajo
                        education_level VARCHAR(100) NULL,
                        phone VARCHAR(50) NULL,
                        landline_phone VARCHAR(50) NULL,
                        email VARCHAR(150) NULL,
                        lives_with_student VARCHAR(10) DEFAULT 'Sí',  -- Sí, No
                        address VARCHAR(255) NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        PRIMARY KEY (id),
                        INDEX idx_tutors_tenant (tenant_id),
                        INDEX idx_tutors_dni (document_number),
                        CONSTRAINT fk_tutors_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 5. TABLA INTERMEDIA: VINCULACIÓN ALUMNO - TUTOR (RELACIÓN N:M)
-- ===================================================================================
CREATE TABLE student_tutors (
                                id VARCHAR(36) NOT NULL,
                                student_id VARCHAR(36) NOT NULL,
                                tutor_id VARCHAR(36) NOT NULL,
                                is_primary BOOLEAN NOT NULL DEFAULT TRUE,     -- Contacto N°1 o N°2
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                PRIMARY KEY (id),
                                UNIQUE KEY uq_student_tutor (student_id, tutor_id),
                                INDEX idx_st_student (student_id),
                                INDEX idx_st_tutor (tutor_id),
                                CONSTRAINT fk_st_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                                CONSTRAINT fk_st_tutor FOREIGN KEY (tutor_id) REFERENCES tutors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 6. TABLA: PERSONAL / STAFF DOCENTE
-- ===================================================================================
CREATE TABLE institution_staff (
                                   id VARCHAR(36) NOT NULL,
                                   tenant_id VARCHAR(36) NOT NULL,
                                   first_name VARCHAR(100) NOT NULL,
                                   last_name VARCHAR(100) NOT NULL,
                                   document_number VARCHAR(50) NULL,
                                   email VARCHAR(150) NOT NULL,
                                   password VARCHAR(255) NOT NULL DEFAULT '123',
                                   role VARCHAR(50) NOT NULL,                   -- TEACHER, ADMINISTRATIVE, DIRECTOR
                                   hire_date DATE NOT NULL,
                                   classroom VARCHAR(100) NULL,
                                   phone VARCHAR(50) NULL,
                                   status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                   PRIMARY KEY (id),
                                   INDEX idx_staff_tenant (tenant_id),
                                   INDEX idx_staff_email (email),
                                   CONSTRAINT fk_staff_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 7. TABLA: CUOTAS / ARANCELES ESCOLARES (STUDENT FEES)
-- ===================================================================================
CREATE TABLE student_fees (
                              id VARCHAR(36) NOT NULL,
                              student_id VARCHAR(36) NOT NULL,
                              academic_year INT NOT NULL,                   -- Ej: 2026
                              month_number INT NOT NULL,                   -- 0 = Matrícula, 3 = Marzo, ..., 12 = Diciembre
                              status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',-- PAID, UNPAID
                              paid_at TIMESTAMP NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                              PRIMARY KEY (id),
                              UNIQUE KEY uq_fee_student_year_month (student_id, academic_year, month_number),
                              INDEX idx_fees_student (student_id),
                              CONSTRAINT fk_fees_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 8. TABLA: PERSONAS AUTORIZADAS PARA RETIRO (ART. 154)
-- ===================================================================================
CREATE TABLE student_authorized_pickups (
                                            id VARCHAR(36) NOT NULL,
                                            student_id VARCHAR(36) NOT NULL,
                                            full_name VARCHAR(150) NOT NULL,
                                            document_number VARCHAR(50) NOT NULL,
                                            birth_date DATE NOT NULL,
                                            age INT NOT NULL,                             -- Calculado (>= 18 años)
                                            relationship VARCHAR(100) NOT NULL,
                                            phone VARCHAR(50) NOT NULL,
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                            PRIMARY KEY (id),
                                            INDEX idx_pickups_student (student_id),
                                            CONSTRAINT fk_pickups_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 9. TABLA: RESTRICCIONES POR DECISIONES JUDICIALES
-- ===================================================================================
CREATE TABLE student_judicial_restrictions (
                                               id VARCHAR(36) NOT NULL,
                                               student_id VARCHAR(36) NOT NULL,
                                               last_name VARCHAR(100) NOT NULL,
                                               first_name VARCHAR(100) NOT NULL,
                                               document_type VARCHAR(20) NOT NULL DEFAULT 'DNI',
                                               document_number VARCHAR(50) NOT NULL,
                                               description TEXT NOT NULL,
                                               legajo_number VARCHAR(50) NULL,
                                               matrix_number VARCHAR(50) NULL,
                                               folio_number VARCHAR(50) NULL,
                                               inscription_date DATE NULL,
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                               PRIMARY KEY (id),
                                               INDEX idx_restrictions_student (student_id),
                                               CONSTRAINT fk_restrictions_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 10. DATOS SEMILLA (INSTITUCIÓN ONCE UNIDOS POR DEFECTO)
-- ===================================================================================
INSERT INTO tenants (id, name, cue)
VALUES ('88888888-4444-4444-4444-121212121212', 'Jardín Once Unidos', '06000001');

INSERT INTO institution_settings (id, tenant_id, receipt_email, fee_query_email)
VALUES (
           '11111111-2222-3333-4444-555555555555',
           '88888888-4444-4444-4444-121212121212',
           'administracion@onceunidos.com',
           'tesoreria@onceunidos.com'
       );