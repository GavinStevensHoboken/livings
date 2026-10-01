package com.livings.university;

import org.springframework.stereotype.Service;

import com.livings.common.ApiException;

@Service
public class UniversityService {

    private final UniversityRepository repository;

    public UniversityService(UniversityRepository repository) {
        this.repository = repository;
    }

    public University require(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("University", id));
    }

    /** Null-safe lookup for optional university references on requests. */
    public University findOptional(Long id) {
        return id == null ? null : require(id);
    }
}
