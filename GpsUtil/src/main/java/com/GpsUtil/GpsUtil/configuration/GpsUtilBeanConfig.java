package com.GpsUtil.GpsUtil.configuration;

import gpsUtil.GpsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GpsUtilBeanConfig {

    @Bean
    public GpsUtil getGpsUtil() {
        return new GpsUtil();
    }
}