package com.jivRas.groceries.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.repository.LocationRepository;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationRepository locationRepository;

    public LocationController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(locationRepository.findDistinctStates());
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCities(@RequestParam String state) {
        return ResponseEntity.ok(locationRepository.findCitiesByState(state));
    }

    @GetMapping("/pincodes")
    public ResponseEntity<List<String>> getPincodes(@RequestParam String state, @RequestParam String city) {
        return ResponseEntity.ok(locationRepository.findPincodesByStateAndCity(state, city));
    }
}
