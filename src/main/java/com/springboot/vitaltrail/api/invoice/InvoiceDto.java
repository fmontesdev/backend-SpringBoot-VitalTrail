package com.springboot.vitaltrail.api.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class InvoiceDto {

    /** Identificador único de la factura en Stripe (inv_xxx) */
    private String id;

    /** Número de factura legible (nullable) */
    private String number;

    /** Concepto de la factura: "Suscripción mensual", "Suscripción anual", etc. (nullable) */
    private String description;

    /** Inicio del período de validez en epoch seconds (nullable) */
    private Long periodStart;

    /** Fin del período de validez en epoch seconds (nullable) */
    private Long periodEnd;

    /** Estado de la factura: paid | open | void | uncollectible | draft */
    private String status;

    /** Importe total en céntimos */
    private Long amountTotal;

    /** Moneda en minúsculas: "eur", "usd" */
    private String currency;

    /** Fecha de creación en epoch seconds */
    private Long created;

    /** URL pública de la factura en Stripe (nullable) */
    private String invoiceUrl;

    /** URL del PDF de la factura en Stripe (nullable) */
    private String invoicePdf;
}
