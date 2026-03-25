package com.springboot.vitaltrail.domain.invoice;

import com.springboot.vitaltrail.api.invoice.InvoiceDto;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;
import com.springboot.vitaltrail.domain.payment.StripeDataService;

import com.stripe.exception.StripeException;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    private final StripeDataService stripeDataService;

    /**
     * Devuelve la lista de facturas del customerId indicado.
     * El control de acceso (propietario o ADMIN) se delega a @CheckSecurity en el controlador.
     *
     * @param customerId identificador del cliente en Stripe
     * @return Lista de InvoiceDto (vacía si no hay facturas)
     */
    @Override
    public List<InvoiceDto> getInvoices(String customerId) {
        logger.info("Obteniendo facturas de Stripe para customerId {}", customerId);
        try {
            return stripeDataService.getCustomerInvoices(customerId);
        } catch (StripeException e) {
            logger.error("Error de Stripe al obtener facturas para customerId {}: {}", customerId, e.getMessage());
            throw new AppException(Error.STRIPE_ERROR, e);
        }
    }
}
