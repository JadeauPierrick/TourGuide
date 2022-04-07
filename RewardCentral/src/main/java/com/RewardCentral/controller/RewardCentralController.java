package com.RewardCentral.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rewardCentral.RewardCentral;

import java.util.UUID;

@RestController
public class RewardCentralController {

    private final RewardCentral rewardCentral;

    public RewardCentralController(RewardCentral rewardCentral) {
        this.rewardCentral = rewardCentral;
    }

    @GetMapping(value = "/getAttractionRewardPoints")
    public int getAttractionRewardPoints(@RequestParam("attractionId") UUID attractionId, @RequestParam("userId") UUID userId) {
        return rewardCentral.getAttractionRewardPoints(attractionId, userId);
    }
}
