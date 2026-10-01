package com.livings.listing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.livings.geo.GeoUtils;
import com.livings.geo.UsState;
import com.livings.listing.ListingEnums.Amenity;
import com.livings.listing.ListingEnums.Status;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

final class ListingSpecifications {

    private ListingSpecifications() {
    }

    /** A resolved search origin for radius queries. */
    record GeoOrigin(double latitude, double longitude, double radiusMiles) {
    }

    static Specification<Listing> search(ListingSearchCriteria c, UsState state, GeoOrigin origin) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("status"), Status.ACTIVE));

            if (c.q() != null && !c.q().isBlank()) {
                String like = "%" + c.q().trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (state != null) {
                p.add(cb.equal(root.get("state"), state));
            }
            if (c.city() != null && !c.city().isBlank()) {
                p.add(cb.equal(cb.lower(root.get("city")), c.city().trim().toLowerCase(Locale.ROOT)));
            }
            if (c.zipCode() != null && !c.zipCode().isBlank()) {
                p.add(cb.like(root.get("zipCode"), c.zipCode().trim() + "%"));
            }
            addEqual(p, cb, root, "category", c.category());
            addEqual(p, cb, root, "roomType", c.roomType());
            addEqual(p, cb, root, "propertyType", c.propertyType());
            if (c.genderPreference() != null) {
                // A female student filtering FEMALE_ONLY still wants listings open to ANY gender.
                p.add(root.get("genderPreference").in(c.genderPreference(), ListingEnums.GenderPreference.ANY));
            }
            if (c.minRent() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("monthlyRent"), c.minRent()));
            }
            if (c.maxRent() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("monthlyRent"), c.maxRent()));
            }
            if (c.minBedrooms() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), c.minBedrooms()));
            }
            if (c.minBathrooms() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), c.minBathrooms()));
            }
            if (c.availableBy() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("availableFrom"), c.availableBy()));
            }
            if (c.maxLeaseMonths() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("minLeaseMonths"), c.maxLeaseMonths()));
            }
            addTrue(p, cb, root, "furnished", c.furnished());
            addTrue(p, cb, root, "petsAllowed", c.petsAllowed());
            addTrue(p, cb, root, "utilitiesIncluded", c.utilitiesIncluded());
            addTrue(p, cb, root, "noSsnRequired", c.noSsnRequired());
            addTrue(p, cb, root, "noCreditHistoryRequired", c.noCreditHistoryRequired());
            addTrue(p, cb, root, "internationalGuarantorAccepted", c.internationalGuarantorAccepted());
            addTrue(p, cb, root, "virtualTourAvailable", c.virtualTourAvailable());

            if (c.amenities() != null) {
                // Listing must have every requested amenity.
                for (Amenity amenity : c.amenities()) {
                    Subquery<Long> sub = query.subquery(Long.class);
                    Root<Listing> subRoot = sub.from(Listing.class);
                    Join<Listing, Amenity> join = subRoot.join("amenities");
                    sub.select(subRoot.get("id"))
                            .where(cb.equal(subRoot.get("id"), root.get("id")), cb.equal(join, amenity));
                    p.add(cb.exists(sub));
                }
            }

            if (origin != null) {
                p.addAll(withinRadius(root, cb, origin));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /**
     * Bounding-box prefilter (index friendly) plus an exact great-circle check using the spherical law
     * of cosines rearranged to avoid ACOS: d <= r  <=>  cos-sum >= cos(r / R).
     */
    private static List<Predicate> withinRadius(Root<Listing> root, CriteriaBuilder cb, GeoOrigin o) {
        Expression<Double> lat = root.get("latitude");
        Expression<Double> lng = root.get("longitude");
        double dLat = GeoUtils.latDelta(o.radiusMiles());
        double dLng = GeoUtils.lngDelta(o.radiusMiles(), o.latitude());

        double lat0 = Math.toRadians(o.latitude());
        double lng0 = Math.toRadians(o.longitude());
        Expression<Double> latRad = cb.function("radians", Double.class, lat);
        Expression<Double> lngRad = cb.function("radians", Double.class, lng);
        Expression<Double> cosTerm = cb.prod(
                cb.prod(cb.function("cos", Double.class, latRad), Math.cos(lat0)),
                cb.function("cos", Double.class, cb.diff(lngRad, lng0)));
        Expression<Double> sinTerm = cb.prod(cb.function("sin", Double.class, latRad), Math.sin(lat0));
        double threshold = Math.cos(o.radiusMiles() / GeoUtils.EARTH_RADIUS_MILES);

        return List.of(
                cb.isNotNull(lat),
                cb.isNotNull(lng),
                cb.between(lat, o.latitude() - dLat, o.latitude() + dLat),
                cb.between(lng, o.longitude() - dLng, o.longitude() + dLng),
                cb.greaterThanOrEqualTo(cb.sum(cosTerm, sinTerm), threshold));
    }

    private static void addEqual(List<Predicate> p, CriteriaBuilder cb, Root<Listing> root, String attr, Object value) {
        if (value != null) {
            p.add(cb.equal(root.get(attr), value));
        }
    }

    private static void addTrue(List<Predicate> p, CriteriaBuilder cb, Root<Listing> root, String attr, Boolean value) {
        if (Boolean.TRUE.equals(value)) {
            p.add(cb.isTrue(root.get(attr)));
        }
    }
}
