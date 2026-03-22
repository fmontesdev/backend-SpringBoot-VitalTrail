package com.springboot.vitaltrail.api.payment;

import com.springboot.vitaltrail.api.subscription.SubscriptionDto;
import com.springboot.vitaltrail.domain.payment.StripeService;
import com.springboot.vitaltrail.domain.payment.WebhookService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.stripe.exception.SignatureVerificationException;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
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
        try {
            // Crear la sesión de checkout
            String sessionId = stripeService.createCheckoutSession(request);
            
            // Devolver el ID de la sesión
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            //! Excepcione centralizadas
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Collections.singletonMap("error", e.getMessage())
            );
        }
    }
    
    /**
     * Obtener información de una sesión de checkout
     * @param sessionId
     * @return
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getCheckoutSession(@PathVariable String sessionId) {
        // System.out.println("sessionId: " + sessionId);
        try {
            // Recuperar la información de la sesión
            Map<String, Object> session = stripeService.retrieveCheckoutSession(sessionId);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            //! Excepcione centralizadas
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Collections.singletonMap("error", e.getMessage())
            );
        }
    }

    /**
     * Manejar eventos de Stripe
     * @param payload
     * @param sigHeader
     * @return
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {       
        try {
            // Verificar la firma y construir el evento
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            
            // Validar y encolar el evento para procesamiento asincrónico (con un tag de prueba para identificarlo)
            // webhookService.validateAndQueueEvent(event, "test_prueba4");

            // Validar y encolar el evento para procesamiento asincrónico
            webhookService.validateAndQueueEvent(event, null);
            
            // Responder con éxito
            return ResponseEntity.ok("Webhook received");
        } catch (SignatureVerificationException e) {
            //! Excepciones centralizadas
            // Error de firma - posible intento de falsificación
            logger.error("Invalid signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid signature");
                
        } catch (Exception e) {
            //! Excepciones centralizadas
            logger.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook: " + e.getMessage());
        }
    }

    /**
     * Cancelar la suscripción del usuario actual
     * @return Información de la suscripción actualizada
     */
    @GetMapping("/cancel-subscription/{customerId}")
    public ResponseEntity<?> cancelSubscription(@PathVariable String customerId) {
        try {
            // Cancelar la suscripción y obtener la información actualizada
            SubscriptionDto result = stripeService.cancelSubscription(customerId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            //! Excepciones centralizadas
            logger.error("Error de negocio al cancelar suscripción: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                Collections.singletonMap("error", e.getMessage())
            );
        } catch (Exception e) {
            //! Excepciones centralizadas
            logger.error("Error técnico al cancelar suscripción: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Collections.singletonMap("error", e.getMessage())
            );
        }
    }
}
