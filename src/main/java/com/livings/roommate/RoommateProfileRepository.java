package com.livings.roommate;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoommateProfileRepository extends JpaRepository<RoommateProfile, Long>,
        JpaSpecificationExecutor<RoommateProfile> {

    Optional<RoommateProfile> findByUserId(Long userId);
}
