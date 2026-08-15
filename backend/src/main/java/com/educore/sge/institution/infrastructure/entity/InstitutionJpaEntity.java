package com.educore.sge.institution.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "tenants")
public class InstitutionJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", unique = true, nullable = false)
    private String slug;

    @Column(name = "status", nullable = false)
    private String status;

    // ---- NUEVOS CAMPOS OPCIONALES DIGITALES Y DE CONTACTO ----
    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "google_maps_url")
    private String googleMapsUrl;

    @Column(name = "phone")
    private String phone;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "social_facebook")
    private String socialFacebook;

    @Column(name = "social_instagram")
    private String socialInstagram;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cuit")
    private String cuit;

    // ---- CORREOS INSTITUCIONALES PARA EL MÓDULO ARANCELARIO ----
    @Column(name = "receipt_email")
    private String receiptEmail = "administracion@onceunidos.com";

    @Column(name = "fee_query_email")
    private String feeQueryEmail = "tesoreria@onceunidos.com";

    public InstitutionJpaEntity() {}

    // Getters y Setters de los correos arancelarios
    public String getReceiptEmail() {
        return receiptEmail;
    }

    public void setReceiptEmail(String receiptEmail) {
        this.receiptEmail = receiptEmail;
    }

    public String getFeeQueryEmail() {
        return feeQueryEmail;
    }

    public void setFeeQueryEmail(String feeQueryEmail) {
        this.feeQueryEmail = feeQueryEmail;
    }

    // Demás Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getGoogleMapsUrl() { return googleMapsUrl; }
    public void setGoogleMapsUrl(String googleMapsUrl) { this.googleMapsUrl = googleMapsUrl; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }

    public String getSocialFacebook() { return socialFacebook; }
    public void setSocialFacebook(String socialFacebook) { this.socialFacebook = socialFacebook; }

    public String getSocialInstagram() { return socialInstagram; }
    public void setSocialInstagram(String socialInstagram) { this.socialInstagram = socialInstagram; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }
}