package com.educore.sge.institution.infrastructure.finance;

public enum FeeStatus {
    PAID,       /// Abonado (Verde)
    PENDING,    /// En término para abonar (Verde/Amarillo)
    OVERDUE,    ///Vencido  (Rojo)
    INACTIVE    /// No habilitado por calendario (Negro/Gris)
}