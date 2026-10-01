package com.livings.roommate;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.livings.common.Timestamped;
import com.livings.geo.UsState;
import com.livings.listing.ListingEnums.GenderPreference;
import com.livings.university.University;
import com.livings.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A student's "looking for a roommate" card, used to match people who want to rent together. */
@Entity
@Table(name = "roommate_profiles")
@Getter
@Setter
@NoArgsConstructor
public class RoommateProfile extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String headline;

    @Column(name = "budget_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_state", nullable = false, length = 2)
    private UsState preferredState;

    @Column(name = "preferred_city")
    private String preferredCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "lease_months")
    private Integer leaseMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", nullable = false, length = 30)
    private GenderPreference genderPreference = GenderPreference.ANY;

    @Column(nullable = false)
    private boolean smoker;

    @Column(name = "has_pets", nullable = false)
    private boolean hasPets;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_schedule", length = 30)
    private SleepSchedule sleepSchedule;

    /** 1 (relaxed) to 5 (very tidy). */
    @Column(name = "cleanliness_level")
    private Integer cleanlinessLevel;

    private String bio;

    @Column(nullable = false)
    private boolean active = true;
}
