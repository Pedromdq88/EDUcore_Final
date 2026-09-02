-- ===================================================================================
-- SCRIPT DE BASE DE DATOS: EDUCORE SGE
-- ===================================================================================

CREATE DATABASE IF NOT EXISTS educore_sge;
USE educore_sge;

-- 1. TABLA INSTITUCIONES (TENANTS)
CREATE TABLE IF NOT EXISTS tenants (
                                       id VARCHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    cue_code VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    );

-- 2. TABLA ALUMNOS / ESTUDIANTES
CREATE TABLE IF NOT EXISTS students (
                                        id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    legajo_number VARCHAR(50) NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    academic_year INT NOT NULL,
    classroom VARCHAR(100) NULL,
    gender VARCHAR(20) NULL,
    blood_type VARCHAR(10) NULL,
    health_insurance VARCHAR(150) NULL,
    allergies TEXT NULL,
    birth_place VARCHAR(150) NULL,
    address VARCHAR(255) NULL,
    contact_phone VARCHAR(50) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_students_tenant (tenant_id),
    INDEX idx_students_dni (document_number),
    INDEX idx_students_legajo (legajo_number),
    INDEX idx_students_year (academic_year),
    INDEX idx_students_classroom (classroom),
    CONSTRAINT fk_students_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

-- 3. TABLA TUTORES Y RESPONSABLES
CREATE TABLE IF NOT EXISTS tutors (
                                      id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    nacionalidad VARCHAR(100) NULL DEFAULT 'Argentina',
    profesion VARCHAR(150) NULL,
    condicion_actividad VARCHAR(50) NULL DEFAULT 'Trabaja',
    phone VARCHAR(50) NULL,
    phone_fijo VARCHAR(50) NULL,
    email VARCHAR(150) NULL,
    convive VARCHAR(10) NULL DEFAULT 'Sí',
    direccion VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_tutors_tenant (tenant_id),
    INDEX idx_tutors_dni (document_number),
    CONSTRAINT fk_tutors_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

-- 4. TABLA RELACIÓN ALUMNO - TUTORES
CREATE TABLE IF NOT EXISTS student_tutors (
                                              student_id VARCHAR(36) NOT NULL,
    tutor_id VARCHAR(36) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (student_id, tutor_id),
    INDEX idx_st_student (student_id),
    INDEX idx_st_tutor (tutor_id),
    CONSTRAINT fk_st_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_st_tutor FOREIGN KEY (tutor_id) REFERENCES tutors(id) ON DELETE CASCADE
    );

-- 5. TABLA STAFF (DOCENTES Y PERSONAL)
CREATE TABLE IF NOT EXISTS institution_staff (
                                                 id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    document_number VARCHAR(50) NULL,
    phone VARCHAR(50) NULL,
    hire_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX unq_staff_email_tenant (email, tenant_id),
    INDEX idx_staff_tenant (tenant_id),
    INDEX idx_staff_role (role),
    CONSTRAINT fk_staff_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

-- TABLA ASIGNACIONES DOCENTE
CREATE TABLE IF NOT EXISTS staff_assignments (
                                                 id VARCHAR(36) NOT NULL,
    staff_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    classroom VARCHAR(100) NOT NULL,
    shift VARCHAR(50) NOT NULL DEFAULT 'MANANA',
    is_classroom_teacher BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_asg_staff (staff_id),
    INDEX idx_asg_classroom (classroom),
    INDEX idx_asg_tenant (tenant_id),
    CONSTRAINT fk_asg_staff FOREIGN KEY (staff_id) REFERENCES institution_staff(id) ON DELETE CASCADE,
    CONSTRAINT fk_asg_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );
-- 7. TABLA DE COMUNICADOS INSTITUCIONALES
CREATE TABLE IF NOT EXISTS institution_announcements (
                                                         id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    author_name VARCHAR(150) NOT NULL,
    author_role VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    scope VARCHAR(50) NOT NULL,
    target_classroom VARCHAR(100) NULL,
    target_student_id VARCHAR(36) NULL,
    media_url VARCHAR(500) NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_announcement_tenant (tenant_id),
    INDEX idx_announcement_scope (scope, target_classroom),
    INDEX idx_announcement_student (target_student_id),
    CONSTRAINT fk_announcement_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_student FOREIGN KEY (target_student_id) REFERENCES students(id) ON DELETE CASCADE
    );

-- 8. TABLA PERSONAS AUTORIZADAS PARA RETIRO
CREATE TABLE IF NOT EXISTS student_authorized_pickups (
                                                          id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    age INT NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_pickup_student (student_id),
    INDEX idx_pickup_tenant (tenant_id),
    CONSTRAINT fk_pickup_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_pickup_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

-- 9. TABLA RESTRICCIONES JUDICIALES CERTIFICADAS
CREATE TABLE IF NOT EXISTS student_judicial_restrictions (
                                                             id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_type VARCHAR(20) NOT NULL DEFAULT 'DNI',
    document_number VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    legajo_number VARCHAR(50) NULL,
    matrix_number VARCHAR(50) NULL,
    folio_number VARCHAR(50) NULL,
    inscription_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_restr_student (student_id),
    INDEX idx_restr_tenant (tenant_id),
    CONSTRAINT fk_restr_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_restr_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

-- 10. TABLA CUOTAS Y ARANCELES ESCOLARES
CREATE TABLE IF NOT EXISTS student_fees (
                                            id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    academic_year INT NOT NULL,
    month_number INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX unq_student_fee (student_id, academic_year, month_number),
    INDEX idx_fees_tenant (tenant_id),
    CONSTRAINT fk_fees_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_fees_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

-- 11. TABLA HISTÓRICO DE ALUMNOS DADOS DE BAJA
CREATE TABLE IF NOT EXISTS student_history (
                                               id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    classroom VARCHAR(100) NULL,
    birth_date DATE NOT NULL,
    contact_phone VARCHAR(50) NULL,
    address VARCHAR(255) NULL,
    baja_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS staff_history (
                                             id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL,
    hire_date DATE NOT NULL,
    baja_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_staff_history_tenant (tenant_id)
    );

-- 12. TABLA PORTFOLIOS PEDAGÓGICOS DOCENTES
CREATE TABLE IF NOT EXISTS teacher_portfolios (
                                                  id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    staff_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'CURSO',
    description TEXT NOT NULL,
    media_url VARCHAR(500) NULL,
    activity_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_portfolio_staff (staff_id),
    INDEX idx_portfolio_tenant (tenant_id),
    CONSTRAINT fk_portf_staff FOREIGN KEY (staff_id) REFERENCES institution_staff(id) ON DELETE CASCADE,
    CONSTRAINT fk_portf_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );

-- TABLA CATÁLOGO DE MATERIAS (Gestionada desde el sistema)
CREATE TABLE IF NOT EXISTS academic_subjects (
                                                 id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX unq_subject_name_tenant (tenant_id, name),
    CONSTRAINT fk_subject_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    );
-- INSERCIÓN DEL TENANT INICIAL
INSERT INTO tenants (id, name, cue_code)
VALUES ('88888888-4444-4444-4444-121212121212', 'Jardín Once Unidos', '0600123-4')
    ON DUPLICATE KEY UPDATE name = 'Jardín Once Unidos';