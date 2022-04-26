package com.TripPricer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tripPricer.TripPricer;

@Configuration
public class TripPricerBeanConfig {

    @Bean
    public TripPricer getTripPricer() {
        return new TripPricer();
    }
}
