package com.springboot.vitaltrail.domain.payment;

import com.stripe.model.checkout.Session;
import com.stripe.exception.StripeException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeDataService {
    /**
     * Recupera información completa de una sesión de checkout
     * @param sessionId ID de la sesión de checkout
     * @return Mapa con los datos de la sesión
     * @throws StripeException Si ocurre un error al recuperar los datos
     */
    public Map<String, Object> getCheckoutSessionData(String sessionId) throws StripeException {
        // Recuperar la sesión
        Session session = Session.retrieve(sessionId);
        
        // Datos básicos de la sesión
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("clientReferenceId", session.getClientReferenceId());
        result.put("subscriptionId", session.getSubscription());
        result.put("status", session.getStatus());
        result.put("paymentStatus", session.getPaymentStatus());
        
        // Si existe una suscripción, obtener sus datos
        String subscriptionId = session.getSubscription();
        if (subscriptionId != null && !subscriptionId.isEmpty()) {
            // Obtener datos de suscripción y combinarlos con el resultado
            Map<String, Object> subscriptionData = getSubscriptionData(subscriptionId);
            result.putAll(subscriptionData);
        }
        
        return result;
    }

    /**
     * Recupera información completa de una suscripción
     * @param subscriptionId ID de la suscripción
     * @return Mapa con los datos de la suscripción
     * @throws StripeException Si ocurre un error al recuperar los datos
     */
    public Map<String, Object> getSubscriptionData(String subscriptionId) throws StripeException {
        Map<String, Object> result = new HashMap<>();
        result.put("subscriptionId", subscriptionId);
        
        // Opciones para expandir la respuesta de la API
        Map<String, Object> subParams = new HashMap<>();
        List<String> expandList = new ArrayList<>();
        expandList.add("items.data.price");
        expandList.add("default_payment_method"); // Expandir el método de pago predeterminado
        subParams.put("expand", expandList);
        
        // Recuperar la suscripción con los precios expandidos
        com.stripe.model.Subscription subscription = 
            com.stripe.model.Subscription.retrieve(subscriptionId, subParams, null);
        
        // Datos básicos de la suscripción
        result.put("subscriptionStatus", subscription.getStatus());
        result.put("currentPeriodStart", subscription.getCurrentPeriodStart());
        result.put("currentPeriodEnd", subscription.getCurrentPeriodEnd());
        result.put("cancelAtPeriodEnd", subscription.getCancelAtPeriodEnd());
        result.put("customerId", subscription.getCustomer());
        
        // Añadir información del método de pago
        addPaymentMethodInfo(result, subscription);

        // Si hay elementos de suscripción, obtener el primero
        if (hasSubscriptionItems(subscription)) {
            com.stripe.model.SubscriptionItem item = subscription.getItems().getData().get(0);
            addPriceData(result, item.getPrice());
        }
        
        return result;
    }
    
    /**
     * Añade información del método de pago al resultado
     * @param result Mapa de resultados
     * @param subscription Suscripción de Stripe
     * @throws StripeException Si ocurre un error al recuperar los datos
     */
    private void addPaymentMethodInfo(Map<String, Object> result, com.stripe.model.Subscription subscription) throws StripeException {
        // Obtener ID del método de pago predeterminado
        String paymentMethodId = subscription.getDefaultPaymentMethod();
    
        // Declarar la variable aquí, antes del if-else
        com.stripe.model.PaymentMethod paymentMethod = null;
        
        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            // Recuperar el objeto PaymentMethod completo usando el ID
            paymentMethod = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);

            result.put("paymentMethodId", paymentMethod.getId());
            result.put("paymentMethodType", paymentMethod.getType());
            
            // Para tarjetas, obtener detalles adicionales
            if ("card".equals(paymentMethod.getType()) && paymentMethod.getCard() != null) {
                com.stripe.model.PaymentMethod.Card card = paymentMethod.getCard();
                result.put("cardBrand", card.getBrand());
                result.put("cardLast4", card.getLast4());
                result.put("cardExpMonth", card.getExpMonth());
                result.put("cardExpYear", card.getExpYear());
            }
        } else {
            // Si no hay default_payment_method, buscar el método de pago en el cliente
            try {
                String customerId = subscription.getCustomer();
                if (customerId != null && !customerId.isEmpty()) {
                    // Buscar métodos de pago del cliente
                    Map<String, Object> params = new HashMap<>();
                    params.put("customer", customerId);
                    params.put("type", "card");
                    params.put("limit", 1);
                    
                    com.stripe.model.PaymentMethodCollection methods = 
                        com.stripe.model.PaymentMethod.list(params);
                    
                    if (methods != null && !methods.getData().isEmpty()) {
                        paymentMethod = methods.getData().get(0);
                        result.put("paymentMethodId", paymentMethod.getId());
                        result.put("paymentMethodType", paymentMethod.getType());
                        
                        if ("card".equals(paymentMethod.getType()) && paymentMethod.getCard() != null) {
                            com.stripe.model.PaymentMethod.Card card = paymentMethod.getCard();
                            result.put("cardBrand", card.getBrand());
                            result.put("cardLast4", card.getLast4());
                            result.put("cardExpMonth", card.getExpMonth());
                            result.put("cardExpYear", card.getExpYear());
                        }
                    }
                }
            } catch (Exception e) {
                // Manejar silenciosamente - este es solo un intento de fallback
            }
        }
    }
    
    /**
     * Verifica si una suscripción tiene elementos
     * @param subscription Suscripción de Stripe
     * @return Verdadero si la suscripción tiene elementos
     */
    private boolean hasSubscriptionItems(com.stripe.model.Subscription subscription) {
        return subscription.getItems() != null &&
            subscription.getItems().getData() != null &&
            !subscription.getItems().getData().isEmpty();
    }
    
    /**
     * Añade datos del precio y producto al resultado
     * @param result Mapa de resultados
     * @param price Precio de la suscripción
     */
    private void addPriceData(Map<String, Object> result, com.stripe.model.Price price) 
            throws StripeException {
        if (price != null) {
            // Información del precio
            result.put("priceId", price.getId());
            result.put("priceAmount", price.getUnitAmount());
            result.put("currency", price.getCurrency());
            
            // Información de intervalos de facturación
            if (price.getRecurring() != null) {
                result.put("billingInterval", price.getRecurring().getInterval());
            }
            
            // Añadir información del producto
            String productId = price.getProduct();
            if (productId != null && !productId.isEmpty()) {
                addProductData(result, productId);
            }
        }
    }
    
    /**
     * Añade datos del producto al resultado
     * @param result Mapa de resultados
     * @param productId ID del producto
     */
    private void addProductData(Map<String, Object> result, String productId) 
            throws StripeException {
        com.stripe.model.Product product = com.stripe.model.Product.retrieve(productId);
        
        if (product != null) {
            // Datos básicos del producto
            result.put("productId", product.getId());
            result.put("productName", product.getName());
            result.put("productDescription", product.getDescription());
            
            // Determinar el tipo de suscripción
            Map<String, String> metadata = product.getMetadata();
            String subscriptionType = (metadata != null && metadata.containsKey("type"))
                ? metadata.get("type")
                : product.getName().toLowerCase().replace(" ", "_");
                
            result.put("subscriptionType", subscriptionType);
        }
    }
}
