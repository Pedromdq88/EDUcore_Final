-- ===================================================================================
-- EDUCORE SGE - MIGRACIÓN V1: ESQUEMA INICIAL MULTI-TENANT (EDICIÓN DEFINITIVA MYSQL)
-- ===================================================================================

-- Crear la base de datos si no existe con el set de caracteres adecuado
CREATE DATABASE IF NOT EXISTS educore_sge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Indicarle a MySQL que use esta base de datos para los siguientes comandos
USE educore_sge;

-- 1. Tabla Maestra de Instituciones (Colegios / Jardines)
CREATE TABLE tenants (
                       id VARCHAR(36) PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       slug VARCHAR(100) UNIQUE NOT NULL,
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
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla de Alumnos (Ficha Escolar y Médica Completa)
CREATE TABLE students (
                        tenant_id VARCHAR(36) NOT NULL,
                        id VARCHAR(36) NOT NULL,
                        first_name VARCHAR(100) NOT NULL,
                        last_name VARCHAR(100) NOT NULL,
                        document_number VARCHAR(50) NOT NULL, -- DNI
                        birth_date DATE NOT NULL,
                        gender VARCHAR(20) NULL,
                        legajo_number VARCHAR(50) NULL,
                        classroom VARCHAR(100) NULL, -- Sala
                        blood_type VARCHAR(10) NULL,
                        health_insurance VARCHAR(150) NULL,
                        allergies TEXT NULL,
                        lugar_nacimiento VARCHAR(150) NULL,
                        direccion VARCHAR(255) NULL,
                        telefono_contacto VARCHAR(50) NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, PENDING_WITHDRAWAL, WITHDRAWN

                        PRIMARY KEY (tenant_id, id),
                        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- 3. Tabla de Tutores (Responsables Legales)
CREATE TABLE tutors (
                      tenant_id VARCHAR(36) NOT NULL,
                      id VARCHAR(36) NOT NULL,
                      first_name VARCHAR(100) NOT NULL,
                      last_name VARCHAR(100) NOT NULL,
                      document_number VARCHAR(50) NOT NULL,
                      email VARCHAR(150) NULL,
                      phone VARCHAR(50) NULL,
                      relationship VARCHAR(50) NOT NULL, -- MADRE, PADRE, TUTOR_LEGAL

                      PRIMARY KEY (tenant_id, id),
                      FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- 4. Tabla Intermedia de Vinculación (Muchos a Muchos: Alumnos <-> Tutores)
CREATE TABLE student_tutors (
                              student_id VARCHAR(36) NOT NULL,
                              tutor_id VARCHAR(36) NOT NULL,

                              PRIMARY KEY (student_id, tutor_id)
);

-- 5. Tabla de Personal de la Escuela (Usuarios del Sistema)
-- 5. Tabla de Personal de la Escuela (Usuarios del Sistema - ACTUALIZADA)
CREATE TABLE users (
                     tenant_id VARCHAR(36) NOT NULL,
                     id VARCHAR(36) NOT NULL,
                     email VARCHAR(150) NOT NULL UNIQUE,  -- Pasa a ser UNIQUE y la credencial principal
                     first_name VARCHAR(100) NOT NULL,    -- Nombre real del docente
                     last_name VARCHAR(100) NOT NULL,     -- Apellido real del docente
                     password VARCHAR(255) NOT NULL,
                     role VARCHAR(50) NOT NULL,           -- OWNER, DIRECTOR, ADMINISTRATIVE, TEACHER
                     hire_date DATE NULL,

                     PRIMARY KEY (tenant_id, id),
                     FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);
