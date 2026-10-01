package com.livings.geo;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/states")
@Tag(name = "Reference data")
public class StateController {

    public record StateResponse(String code, String name) {
    }

    @GetMapping
    @Operation(summary = "States in the service area (contiguous US + DC), for iOS pickers")
    public List<StateResponse> list() {
        return Arrays.stream(UsState.values())
                .map(s -> new StateResponse(s.name(), s.getDisplayName()))
                .toList();
    }
}
