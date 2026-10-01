package com.livings.university;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livings.geo.UsState;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/universities")
@Tag(name = "Reference data")
public class UniversityController {

    private final UniversityRepository repository;
    private final UniversityService service;

    public UniversityController(UniversityRepository repository, UniversityService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Search universities by name/city, optionally within a state")
    public List<UniversityResponse> search(@RequestParam(required = false) String state,
                                           @RequestParam(required = false) String q) {
        UsState usState = state == null || state.isBlank() ? null : UsState.fromCode(state);
        String query = q == null || q.isBlank() ? null : q.trim();
        return repository.search(usState, query).stream().map(UniversityResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UniversityResponse get(@PathVariable Long id) {
        return UniversityResponse.from(service.require(id));
    }
}
