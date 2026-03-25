package com.springboot.vitaltrail.api.payment;

import com.springboot.vitaltrail.api.security.authorization.CheckSecurity;
import com.springboot.vitaltrail.api.subscription.SubscriptionDto;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;
import com.springboot.vitaltrail.domain.payment.StripeService;
import com.springboot.vitaltrail.domain.payment.WebhookService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/payments/stripe")
@RequiredArgsConstructor
public class StripeController {
    private static final Logger logger = LoggerFactory.getLogger(StripeController.class);

    private final StripeService stripeService;
    private final WebhookService webhookService;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    /**
     * Crear una sesión de checkout en Stripe
     * @param request
     * @return
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody CheckoutSessionDto request) {
        String sessionId = stripeService.createCheckoutSession(request);
        Map<String, String> response = new HashMap<>();
        response.put("sessionId", sessionId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener información de una sesión de checkout
     * @param sessionId
     * @return
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getCheckoutSession(@PathVariable String sessionId) {
        Map<String, Object> session = stripeService.retrieveCheckoutSession(sessionId);
        return ResponseEntity.ok(session);
    }

    /**
     * Manejar eventos de Stripe
     * @param payload
     * @param signature
     * @return
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        try {
            // Verificar la firma del webhook y construir el evento
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            // Validar y procesar el evento. Si en vez de null se pasa test, se procesarán los datos de prueba
            webhookService.validateAndQueueEvent(event, null);
            return ResponseEntity.ok("Webhook received");
        } catch (SignatureVerificationException e) {
            logger.error("Firma del webhook de Stripe inválida: {}", e.getMessage());
            throw new AppException(Error.STRIPE_INVALID_SIGNATURE);
        } catch (StripeException e) {
            logger.error("Error de Stripe al construir el evento del webhook: {}", e.getMessage());
            throw new AppException(Error.STRIPE_ERROR, e);
        }
    }

    /**
     * Cancelar la suscripción del usuario actual
     * @return Información de la suscripción actualizada
     */
    @CheckSecurity.Protected.isAdminOrOwnerByAction
    @PostMapping("/cancel-subscription")
    public ResponseEntity<?> cancelSubscription(@RequestBody SubscriptionActionDto request) {
        SubscriptionDto result = stripeService.cancelSubscription(request.getCustomerId());
        return ResponseEntity.ok(result);
    }

    /**
     * Marcar la suscripción para cancelarse al final del período
     * @return Información de la suscripción actualizada
     */
    @CheckSecurity.Protected.isAdminOrOwnerByAction
    @PostMapping("/cancel-at-period-end")
    public ResponseEntity<?> cancelAtPeriodEnd(@RequestBody SubscriptionActionDto request) {
        SubscriptionDto result = stripeService.cancelAtPeriodEnd(request.getCustomerId());
        return ResponseEntity.ok(result);
    }

    /**
     * Reactivar suscripción marcada para cancelarse al final del período
     * @return Información de la suscripción actualizada
     */
    @CheckSecurity.Protected.isAdminOrOwnerByAction
    @PostMapping("/reactivate-subscription")
    public ResponseEntity<?> reactivateSubscription(@RequestBody SubscriptionActionDto request) {
        SubscriptionDto result = stripeService.reactivateSubscription(request.getCustomerId());
        return ResponseEntity.ok(result);
    }
}
