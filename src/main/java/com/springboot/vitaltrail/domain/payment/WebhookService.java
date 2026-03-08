package com.springboot.vitaltrail.domain.payment;

import com.stripe.model.Event;
import com.stripe.exception.StripeException;

public interface WebhookService {
    // Método sincrónico para validar y aceptar el evento
    void validateAndQueueEvent(Event event, String test ) throws StripeException;

    // Método asincrónico para procesamiento
    void processStripeEvent(Event event, String test) throws StripeException;
}
