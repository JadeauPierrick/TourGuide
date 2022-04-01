package com.GpsUtil.GpsUtil.controller;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class GpsUtilController {

    @Autowired
    private GpsUtil gpsUtil;

    @GetMapping(value = "/userLocation")
    public VisitedLocation getUserLocation(@RequestParam UUID userId) {
        return gpsUtil.getUserLocation(userId);
    }

    @GetMapping(value = "/attractions")
    public List<Attraction> getAttractions() {
        return gpsUtil.getAttractions();
    }
}