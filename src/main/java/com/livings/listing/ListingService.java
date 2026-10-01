package com.livings.listing;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livings.common.ApiException;
import com.livings.common.PageResponse;
import com.livings.geo.GeoUtils;
import com.livings.geo.ServiceArea;
import com.livings.geo.UsState;
import com.livings.listing.ListingDtos.ListingRequest;
import com.livings.listing.ListingDtos.ListingResponse;
import com.livings.listing.ListingEnums.Category;
import com.livings.listing.ListingEnums.GenderPreference;
import com.livings.listing.ListingEnums.RoomType;
import com.livings.listing.ListingEnums.Status;
import com.livings.listing.ListingSpecifications.GeoOrigin;
import com.livings.security.AuthUser;
import com.livings.university.University;
import com.livings.university.UniversityRepository;
import com.livings.university.UniversityService;
import com.livings.user.Role;
import com.livings.user.UserService;

@Service
public class ListingService {

    static final double DEFAULT_RADIUS_MILES = 10;
    static final double MAX_RADIUS_MILES = 100;
    /** A listing is auto-linked to the closest university within this distance. */
    private static final double NEAREST_UNIVERSITY_MAX_MILES = 30;

    private final ListingRepository listings;
    private final UserService userService;
    private final UniversityService universityService;
    private final UniversityRepository universities;

    public ListingService(ListingRepository listings, UserService userService,
                          UniversityService universityService, UniversityRepository universities) {
        this.listings = listings;
        this.userService = userService;
        this.universityService = universityService;
        this.universities = universities;
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> search(ListingSearchCriteria criteria, Pageable pageable) {
        UsState state = criteria.state() == null || criteria.state().isBlank() ? null : UsState.fromCode(criteria.state());
        GeoOrigin origin = resolveOrigin(criteria);
        var page = listings.findAll(ListingSpecifications.search(criteria, state, origin), pageable);
        return PageResponse.of(page, l -> ListingMapper.toResponse(l, origin));
    }

    @Transactional(readOnly = true)
    public ListingResponse get(Long id, AuthUser viewer) {
        Listing listing = require(id);
        boolean isOwner = viewer != null && listing.getOwner().getId().equals(viewer.id());
        if (listing.getStatus() == Status.ARCHIVED && !isOwner) {
            throw ApiException.notFound("Listing", id);
        }
        return ListingMapper.toResponse(listing, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> mine(AuthUser me, Pageable pageable) {
        return PageResponse.of(listings.findByOwnerIdAndStatusNot(me.id(), Status.ARCHIVED, pageable),
                l -> ListingMapper.toResponse(l, null));
    }

    @Transactional
    public ListingResponse create(AuthUser me, ListingRequest request) {
        Listing listing = new Listing();
        listing.setOwner(userService.require(me.id()));
        apply(listing, request);
        return ListingMapper.toResponse(listings.save(listing), null);
    }

    @Transactional
    public ListingResponse update(AuthUser me, Long id, ListingRequest request) {
        Listing listing = requireOwned(me, id);
        apply(listing, request);
        return ListingMapper.toResponse(listing, null);
    }

    @Transactional
    public ListingResponse updateStatus(AuthUser me, Long id, Status status) {
        Listing listing = requireOwned(me, id);
        listing.setStatus(status);
        return ListingMapper.toResponse(listing, null);
    }

    /** Soft delete: keeps conversations intact for both parties. */
    @Transactional
    public void archive(AuthUser me, Long id) {
        requireOwned(me, id).setStatus(Status.ARCHIVED);
    }

    /** A listing other users may interact with (favorite, message): anything but archived. */
    public Listing requireVisible(Long id) {
        Listing listing = require(id);
        if (listing.getStatus() == Status.ARCHIVED) {
            throw ApiException.notFound("Listing", id);
        }
        return listing;
    }

    public ListingResponse toResponse(Listing listing) {
        return ListingMapper.toResponse(listing, null);
    }

    Listing require(Long id) {
        return listings.findById(id).orElseThrow(() -> ApiException.notFound("Listing", id));
    }

    private Listing requireOwned(AuthUser me, Long id) {
        Listing listing = require(id);
        if (!listing.getOwner().getId().equals(me.id()) && me.role() != Role.ADMIN) {
            throw ApiException.forbidden("You can only modify your own listings");
        }
        return listing;
    }

    private void apply(Listing l, ListingRequest r) {
        if (r.category() == Category.ROOMMATE && r.roomType() == RoomType.ENTIRE_PLACE) {
            throw ApiException.badRequest("Roommate listings must offer a PRIVATE_ROOM or SHARED_ROOM");
        }
        UsState state = UsState.fromCode(r.state());
        String zip = ServiceArea.validateZip(r.zipCode());
        ServiceArea.validateCoordinates(r.latitude(), r.longitude());

        l.setTitle(r.title().trim());
        l.setDescription(r.description().trim());
        l.setCategory(r.category());
        l.setRoomType(r.roomType());
        l.setPropertyType(r.propertyType());
        l.setGenderPreference(Objects.requireNonNullElse(r.genderPreference(), GenderPreference.ANY));
        l.setStreetAddress(r.streetAddress().trim());
        l.setUnit(r.unit());
        l.setCity(r.city().trim());
        l.setState(state);
        l.setZipCode(zip);
        l.setLatitude(r.latitude());
        l.setLongitude(r.longitude());
        l.setNearestUniversity(r.nearestUniversityId() != null
                ? universityService.require(r.nearestUniversityId())
                : findNearestUniversity(r.latitude(), r.longitude()));
        l.setMonthlyRent(r.monthlyRent());
        l.setSecurityDeposit(r.securityDeposit());
        l.setUtilitiesIncluded(r.utilitiesIncluded());
        l.setEstimatedUtilities(r.estimatedUtilities());
        l.setAvailableFrom(r.availableFrom());
        l.setMinLeaseMonths(r.minLeaseMonths());
        l.setBedrooms(r.bedrooms());
        l.setBathrooms(r.bathrooms());
        l.setCurrentOccupants(r.currentOccupants());
        l.setFurnished(r.furnished());
        l.setPetsAllowed(r.petsAllowed());
        l.setNoSsnRequired(r.noSsnRequired());
        l.setNoCreditHistoryRequired(r.noCreditHistoryRequired());
        l.setInternationalGuarantorAccepted(r.internationalGuarantorAccepted());
        l.setVirtualTourAvailable(r.virtualTourAvailable());
        l.getPhotoUrls().clear();
        if (r.photoUrls() != null) {
            l.getPhotoUrls().addAll(r.photoUrls());
        }
        l.getAmenities().clear();
        l.getAmenities().addAll(r.amenities() == null ? EnumSet.noneOf(ListingEnums.Amenity.class) : r.amenities());
    }

    private University findNearestUniversity(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }
        return universities.findAll().stream()
                .min(Comparator.comparingDouble(u -> GeoUtils.distanceMiles(lat, lng, u.getLatitude(), u.getLongitude())))
                .filter(u -> GeoUtils.distanceMiles(lat, lng, u.getLatitude(), u.getLongitude()) <= NEAREST_UNIVERSITY_MAX_MILES)
                .orElse(null);
    }

    private GeoOrigin resolveOrigin(ListingSearchCriteria c) {
        double radius = c.radiusMiles() == null ? DEFAULT_RADIUS_MILES : c.radiusMiles();
        if (radius <= 0 || radius > MAX_RADIUS_MILES) {
            throw ApiException.badRequest("radiusMiles must be between 0 and " + (int) MAX_RADIUS_MILES);
        }
        if (c.universityId() != null) {
            University u = universityService.require(c.universityId());
            return new GeoOrigin(u.getLatitude(), u.getLongitude(), radius);
        }
        if (c.latitude() != null || c.longitude() != null) {
            ServiceArea.validateCoordinates(c.latitude(), c.longitude());
            return new GeoOrigin(c.latitude(), c.longitude(), radius);
        }
        if (c.radiusMiles() != null) {
            throw ApiException.badRequest("radiusMiles requires universityId or latitude/longitude");
        }
        return null;
    }
}
