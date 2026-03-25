package com.springboot.vitaltrail.api.invoice;

import com.springboot.vitaltrail.api.security.authorization.CheckSecurity;
import com.springboot.vitaltrail.domain.invoice.InvoiceService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payments/stripe")
@RequiredArgsConstructor
public class InvoiceController {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;

    /**
     * Obtiene la lista de facturas del customerId indicado.
     * Solo accesible por el propio propietario o un usuario ADMIN.
     *
     * @param customerId identificador del cliente en Stripe
     * @return Lista de facturas del cliente
     */
    @CheckSecurity.Protected.isAdminOrOwner
    @GetMapping("/invoices/{customerId}")
    public ResponseEntity<List<InvoiceDto>> getInvoices(@PathVariable String customerId) {
        List<InvoiceDto> invoices = invoiceService.getInvoices(customerId);
        return ResponseEntity.ok(invoices);
    }
}
