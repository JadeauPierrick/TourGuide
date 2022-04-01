package com.TripPricer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tripPricer.Provider;
import tripPricer.TripPricer;

import java.util.List;
import java.util.UUID;

@RestController
public class TripPricerController {

    @Autowired
    private TripPricer tripPricer;

    @GetMapping(value = "/price")
    public List<Provider> getPrice(@RequestParam("tripPricerApiKey") String tripPricerApiKey, @RequestParam("attractionId") UUID attractionId,
                                   @RequestParam("adults") int adults, @RequestParam("children") int children, @RequestParam("nightsStay") int nightsStay,
                                   @RequestParam("rewardsPoints") int rewardsPoints) {
        return tripPricer.getPrice(tripPricerApiKey, attractionId, adults, children, nightsStay, rewardsPoints);
    }
}
