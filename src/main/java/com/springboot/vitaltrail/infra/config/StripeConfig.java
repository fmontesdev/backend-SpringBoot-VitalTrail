package com.springboot.vitaltrail.infra.config;

import com.stripe.Stripe;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;


@Configuration
@AllArgsConstructor
public class StripeConfig {
    private final Dotenv dotenv;

    @Bean
    public String initStripe() { 
        return Stripe.apiKey = dotenv.get("STRIPE_API_KEY");
    }
}
