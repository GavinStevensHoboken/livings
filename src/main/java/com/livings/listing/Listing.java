package com.livings.listing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.livings.common.Timestamped;
import com.livings.geo.UsState;
import com.livings.listing.ListingEnums.Amenity;
import com.livings.listing.ListingEnums.Category;
import com.livings.listing.ListingEnums.GenderPreference;
import com.livings.listing.ListingEnums.PropertyType;
import com.livings.listing.ListingEnums.RoomType;
import com.livings.listing.ListingEnums.Status;
import com.livings.university.University;
import com.livings.user.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
public class Listing extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 30)
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", nullable = false, length = 30)
    private GenderPreference genderPreference = GenderPreference.ANY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    // --- Location ---------------------------------------------------------------------------

    @Column(name = "street_address", nullable = false)
    private String streetAddress;

    private String unit;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private UsState state;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    private Double latitude;

    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nearest_university_id")
    private University nearestUniversity;

    // --- Price & terms ----------------------------------------------------------------------

    @Column(name = "monthly_rent", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "security_deposit", precision = 10, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "utilities_included", nullable = false)
    private boolean utilitiesIncluded;

    @Column(name = "estimated_utilities", precision = 10, scale = 2)
    private BigDecimal estimatedUtilities;

    @Column(name = "available_from", nullable = false)
    private LocalDate availableFrom;

    @Column(name = "min_lease_months", nullable = false)
    private int minLeaseMonths;

    // --- The space --------------------------------------------------------------------------

    @Column(nullable = false)
    private int bedrooms;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal bathrooms;

    /** People already living there (relevant for roommate listings). */
    @Column(name = "current_occupants", nullable = false)
    private int currentOccupants;

    @Column(nullable = false)
    private boolean furnished;

    @Column(name = "pets_allowed", nullable = false)
    private boolean petsAllowed;

    // --- International-student friendliness -------------------------------------------------

    /** Application does not require a US Social Security Number. */
    @Column(name = "no_ssn_required", nullable = false)
    private boolean noSsnRequired;

    /** No US credit history required (e.g. accepts bank statements / I-20 funding proof). */
    @Column(name = "no_credit_history_required", nullable = false)
    private boolean noCreditHistoryRequired;

    /** Accepts international guarantor services or an overseas guarantor. */
    @Column(name = "international_guarantor_accepted", nullable = false)
    private boolean internationalGuarantorAccepted;

    /** Can be toured by video call before arriving in the US. */
    @Column(name = "virtual_tour_available", nullable = false)
    private boolean virtualTourAvailable;

    // --- Collections ------------------------------------------------------------------------

    @ElementCollection
    @CollectionTable(name = "listing_photos", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", nullable = false, length = 1000)
    private List<String> photoUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "listing_amenities", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "amenity", nullable = false, length = 40)
    private Set<Amenity> amenities = EnumSet.noneOf(Amenity.class);
}
