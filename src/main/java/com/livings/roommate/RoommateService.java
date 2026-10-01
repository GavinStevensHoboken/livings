package com.livings.roommate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livings.common.ApiException;
import com.livings.common.PageResponse;
import com.livings.geo.UsState;
import com.livings.listing.ListingEnums.GenderPreference;
import com.livings.roommate.RoommateDtos.RoommateProfileRequest;
import com.livings.roommate.RoommateDtos.RoommateProfileResponse;
import com.livings.roommate.RoommateDtos.RoommateSearchCriteria;
import com.livings.security.AuthUser;
import com.livings.university.UniversityService;
import com.livings.user.Gender;
import com.livings.user.User;
import com.livings.user.UserService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

@Service
public class RoommateService {

    private final RoommateProfileRepository profiles;
    private final UserService userService;
    private final UniversityService universityService;

    public RoommateService(RoommateProfileRepository profiles, UserService userService,
                           UniversityService universityService) {
        this.profiles = profiles;
        this.userService = userService;
        this.universityService = universityService;
    }

    @Transactional(readOnly = true)
    public RoommateProfileResponse mine(AuthUser me) {
        return RoommateProfileResponse.from(profiles.findByUserId(me.id())
                .orElseThrow(() -> ApiException.notFound("Roommate profile for user", me.id())));
    }

    @Transactional(readOnly = true)
    public RoommateProfileResponse get(Long id) {
        RoommateProfile profile = profiles.findById(id).filter(RoommateProfile::isActive)
                .orElseThrow(() -> ApiException.notFound("Roommate profile", id));
        return RoommateProfileResponse.from(profile);
    }

    @Transactional
    public RoommateProfileResponse upsert(AuthUser me, RoommateProfileRequest r) {
        if (r.budgetMin().compareTo(r.budgetMax()) > 0) {
            throw ApiException.badRequest("budgetMin must not exceed budgetMax");
        }
        RoommateProfile p = profiles.findByUserId(me.id()).orElseGet(() -> {
            RoommateProfile created = new RoommateProfile();
            created.setUser(userService.require(me.id()));
            return created;
        });
        p.setHeadline(r.headline().trim());
        p.setBudgetMin(r.budgetMin());
        p.setBudgetMax(r.budgetMax());
        p.setPreferredState(UsState.fromCode(r.preferredState()));
        p.setPreferredCity(r.preferredCity() == null ? null : r.preferredCity().trim());
        p.setUniversity(universityService.findOptional(r.universityId()));
        p.setMoveInDate(r.moveInDate());
        p.setLeaseMonths(r.leaseMonths());
        p.setGenderPreference(Objects.requireNonNullElse(r.genderPreference(), GenderPreference.ANY));
        p.setSmoker(r.smoker());
        p.setHasPets(r.hasPets());
        p.setSleepSchedule(r.sleepSchedule());
        p.setCleanlinessLevel(r.cleanlinessLevel());
        p.setBio(r.bio());
        p.setActive(r.active() == null || r.active());
        return RoommateProfileResponse.from(profiles.save(p));
    }

    @Transactional
    public void deactivate(AuthUser me) {
        profiles.findByUserId(me.id()).ifPresent(p -> p.setActive(false));
    }

    @Transactional(readOnly = true)
    public PageResponse<RoommateProfileResponse> search(AuthUser me, RoommateSearchCriteria c, Pageable pageable) {
        User viewer = userService.require(me.id());
        UsState state = c.state() == null || c.state().isBlank() ? null : UsState.fromCode(c.state());
        return PageResponse.of(profiles.findAll(spec(c, state, viewer), pageable), RoommateProfileResponse::from);
    }

    private static Specification<RoommateProfile> spec(RoommateSearchCriteria c, UsState state, User viewer) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            Join<RoommateProfile, User> user = root.join("user");
            p.add(cb.isTrue(root.get("active")));
            p.add(cb.notEqual(user.get("id"), viewer.getId()));

            // Respect the other person's gender preference toward the viewer.
            if (viewer.getGender() == Gender.FEMALE) {
                p.add(root.get("genderPreference").in(GenderPreference.ANY, GenderPreference.FEMALE_ONLY));
            } else if (viewer.getGender() == Gender.MALE) {
                p.add(root.get("genderPreference").in(GenderPreference.ANY, GenderPreference.MALE_ONLY));
            } else {
                p.add(cb.equal(root.get("genderPreference"), GenderPreference.ANY));
            }

            if (state != null) {
                p.add(cb.equal(root.get("preferredState"), state));
            }
            if (c.city() != null && !c.city().isBlank()) {
                p.add(cb.equal(cb.lower(root.get("preferredCity")), c.city().trim().toLowerCase(Locale.ROOT)));
            }
            if (c.universityId() != null) {
                p.add(cb.equal(root.get("university").get("id"), c.universityId()));
            }
            if (c.maxBudget() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("budgetMin"), c.maxBudget()));
            }
            if (c.minBudget() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("budgetMax"), c.minBudget()));
            }
            if (c.moveInFrom() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("moveInDate"), c.moveInFrom()));
            }
            if (c.moveInTo() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("moveInDate"), c.moveInTo()));
            }
            if (c.gender() != null) {
                p.add(cb.equal(user.get("gender"), c.gender()));
            }
            if (Boolean.TRUE.equals(c.nonSmoker())) {
                p.add(cb.isFalse(root.get("smoker")));
            }
            if (Boolean.TRUE.equals(c.noPets())) {
                p.add(cb.isFalse(root.get("hasPets")));
            }
            if (c.sleepSchedule() != null) {
                p.add(cb.equal(root.get("sleepSchedule"), c.sleepSchedule()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
