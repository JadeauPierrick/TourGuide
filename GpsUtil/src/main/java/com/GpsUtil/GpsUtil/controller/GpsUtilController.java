package com.GpsUtil.GpsUtil.controller;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class GpsUtilController {

    private final GpsUtil gpsUtil;

    public GpsUtilController(GpsUtil gpsUtil) {
        this.gpsUtil = gpsUtil;
    }

    @GetMapping(value = "/userLocation")
    public VisitedLocation getUserLocation(@RequestParam UUID userId) {
        return gpsUtil.getUserLocation(userId);
    }

    @GetMapping(value = "/attractions")
    public List<Attraction> getAttractions() {
        return gpsUtil.getAttractions();
    }
}