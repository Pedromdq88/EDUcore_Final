-- ===================================================================================
-- 1. TABLA MAESTRA DE INSTITUCIONES (COLEGIOS / JARDINES)
-- ===================================================================================
CREATE TABLE IF NOT EXISTS tenants (
                                       id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    website_url VARCHAR(255) NULL,
    direccion VARCHAR(255) NULL,
    google_maps_url TEXT NULL,
    phone VARCHAR(50) NULL,
    whatsapp_number VARCHAR(50) NULL,
    social_facebook VARCHAR(255) NULL,
    social_instagram VARCHAR(255) NULL,
    logo_url VARCHAR(255) NULL,
    cuit VARCHAR(50) NULL,
    receipt_email VARCHAR(150) NULL DEFAULT 'administracion@onceunidos.com',
    fee_query_email VARCHAR(150) NULL DEFAULT 'tesoreria@onceunidos.com',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_tenants_slug (slug)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 2. TABLA DE USUARIOS / PERSONAL DE LA INSTITUCIÓN
-- ===================================================================================
CREATE TABLE IF NOT EXISTS users (
                                     id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    email VARCHAR(150) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    classroom VARCHAR(100) NULL,
    hire_date DATE NULL,
    document_number VARCHAR(50) NULL,
    phone VARCHAR(50) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    INDEX idx_users_tenant (tenant_id),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 3. TABLA DE TUTORES / RESPONSABLES LEGALES
-- ===================================================================================
CREATE TABLE IF NOT EXISTS tutors (
                                      id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    nacionalidad VARCHAR(100) NULL DEFAULT 'Argentina',
    profesion VARCHAR(150) NULL,
    condicion_actividad VARCHAR(100) NULL DEFAULT 'Trabaja',
    phone VARCHAR(50) NULL,
    phone_fijo VARCHAR(50) NULL,
    email VARCHAR(150) NULL,
    convive VARCHAR(10) NULL DEFAULT 'Sí',
    direccion VARCHAR(255) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_tutors_tenant (tenant_id),
    INDEX idx_tutors_dni (document_number),
    CONSTRAINT fk_tutors_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 4. TABLA DE ESTUDIANTES / ALUMNOS
-- ===================================================================================
CREATE TABLE IF NOT EXISTS students (
                                        id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    classroom VARCHAR(100) NULL,
    legajo_number VARCHAR(50) NULL,
    gender VARCHAR(20) NULL,
    blood_type VARCHAR(10) NULL,
    health_insurance VARCHAR(150) NULL,
    allergies TEXT NULL,
    lugar_nacimiento VARCHAR(150) NULL,
    direccion VARCHAR(255) NULL,
    telefono_contacto VARCHAR(50) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_students_tenant (tenant_id),
    INDEX idx_students_dni (document_number),
    INDEX idx_students_classroom (classroom),
    CONSTRAINT fk_students_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 5. TABLA INTERMEDIA: ALUMNOS <-> TUTORES
-- ===================================================================================
CREATE TABLE IF NOT EXISTS student_tutors (
                                              student_id VARCHAR(36) NOT NULL,
    tutor_id VARCHAR(36) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (student_id, tutor_id),
    CONSTRAINT fk_st_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_st_tutor FOREIGN KEY (tutor_id) REFERENCES tutors(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 6. TABLA DE CUOTAS Y ARANCELES
-- ===================================================================================
CREATE TABLE IF NOT EXISTS student_fees (
                                            id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    academic_year INT NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    month_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    due_date DATE NULL,
    payment_date DATETIME(6) NULL,
    registered_by_user_id VARCHAR(100) NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_student_fee_month UNIQUE (student_id, academic_year, fee_type, month_number),
    INDEX idx_fees_student_year (student_id, academic_year),
    INDEX idx_fees_status (status),
    CONSTRAINT fk_fees_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===================================================================================
-- 7. TABLAS DE HISTORIAL DE BAJAS
-- ===================================================================================
CREATE TABLE IF NOT EXISTS historial_alumnos_baja (
                                                      id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    classroom VARCHAR(100) NULL,
    motivo_baja TEXT NULL,
    fecha_baja TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dado_de_baja_por VARCHAR(100) NULL,
    PRIMARY KEY (id),
    INDEX idx_hist_alum_tenant (tenant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS historial_tutores_baja (
                                                      id VARCHAR(36) NOT NULL,
    tutor_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    relationship VARCHAR(50) NULL,
    motivo_baja TEXT NULL,
    fecha_baja TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dado_de_baja_por VARCHAR(100) NULL,
    PRIMARY KEY (id),
    INDEX idx_hist_tutor_tenant (tenant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS historial_personal_baja (
                                                       id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL,
    hire_date DATE NULL,
    fecha_egreso DATE NOT NULL,
    antiguedad_calculada VARCHAR(150) NULL,
    fecha_baja TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dado_de_baja_por VARCHAR(100) NULL,
    PRIMARY KEY (id),
    INDEX idx_hist_staff_tenant (tenant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;