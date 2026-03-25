package com.springboot.vitaltrail.domain.invoice;

import com.springboot.vitaltrail.api.invoice.InvoiceDto;

import java.util.List;

public interface InvoiceService {

    /**
     * Devuelve la lista de facturas asociadas al customerId indicado.
     * Solo puede invocar este método el propio cliente o un usuario con rol ADMIN.
     *
     * @param customerId identificador del cliente en Stripe
     * @return Lista de facturas (vacía si el cliente no tiene facturas en Stripe)
     */
    List<InvoiceDto> getInvoices(String customerId);
}
