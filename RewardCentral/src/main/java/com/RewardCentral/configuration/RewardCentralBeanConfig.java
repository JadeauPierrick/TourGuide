package com.RewardCentral.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rewardCentral.RewardCentral;

@Configuration
public class RewardCentralBeanConfig {

    @Bean
    public RewardCentral getRewardCentral() {
        return new RewardCentral();
    }
}
