package com.livings;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livings.university.UniversityRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LivingsApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UniversityRepository universities;

    // --- helpers -----------------------------------------------------------------------------

    record Account(long id, String token) {
    }

    Account register(String role) throws Exception {
        return register(role, null);
    }

    Account register(String role, String gender) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@school.edu";
        String body = json.writeValueAsString(Map.of(
                "email", email, "password", "s3cure-pass", "fullName", "Test " + role,
                "role", role, "homeCountry", "India"));
        JsonNode res = read(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.eduEmail").value(true)));
        Account account = new Account(res.at("/user/id").asLong(), res.get("accessToken").asText());
        if (gender != null) {
            mvc.perform(auth(patch("/api/v1/users/me"), account).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"gender\":\"" + gender + "\"}")).andExpect(status().isOk());
        }
        return account;
    }

    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Account account) {
        return req.header("Authorization", "Bearer " + account.token());
    }

    JsonNode read(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    Map<String, Object> bostonListing() {
        Map<String, Object> m = new HashMap<>();
        m.put("title", "Sunny private room near Northeastern");
        m.put("description", "Furnished room in a 3BR apartment, 5 min walk to campus.");
        m.put("category", "ROOMMATE");
        m.put("roomType", "PRIVATE_ROOM");
        m.put("propertyType", "APARTMENT");
        m.put("streetAddress", "100 Huntington Ave");
        m.put("city", "Boston");
        m.put("state", "MA");
        m.put("zipCode", "02115");
        m.put("latitude", 42.3420);
        m.put("longitude", -71.0850);
        m.put("monthlyRent", 1450);
        m.put("availableFrom", LocalDate.now().plusDays(30).toString());
        m.put("minLeaseMonths", 4);
        m.put("bedrooms", 3);
        m.put("bathrooms", 1.5);
        m.put("currentOccupants", 2);
        m.put("furnished", true);
        m.put("noSsnRequired", true);
        m.put("noCreditHistoryRequired", true);
        m.put("photoUrls", List.of("https://cdn.example.com/1.jpg"));
        m.put("amenities", List.of("IN_UNIT_LAUNDRY", "WIFI_INCLUDED"));
        return m;
    }

    long createListing(Account owner, Map<String, Object> listing) throws Exception {
        return read(mvc.perform(auth(post("/api/v1/listings"), owner).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(listing))).andExpect(status().isCreated())).get("id").asLong();
    }

    long universityId(String name) {
        return universities.findAll().stream().filter(u -> u.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    // --- tests -------------------------------------------------------------------------------

    @Test
    void registerLoginAndProfile() throws Exception {
        String email = "maria-" + UUID.randomUUID() + "@gmail.com";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        Map.of("email", email, "password", "password123", "fullName", "Maria", "role", "STUDENT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.eduEmail").value(false));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        Map.of("email", email.toUpperCase(), "password", "password123", "fullName", "Maria", "role", "STUDENT"))))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        String token = read(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())).get("accessToken").asText();

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void cannotSelfRegisterAsAdmin() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                        Map.of("email", "boss@x.com", "password", "password123", "fullName", "Boss", "role", "ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void referenceDataIsPublicAndExcludesAkHiPr() throws Exception {
        mvc.perform(get("/api/v1/states")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(49)));
        mvc.perform(get("/api/v1/universities").param("state", "MA").param("q", "northeastern"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].city").value("Boston"));
        mvc.perform(get("/api/v1/universities").param("state", "HI")).andExpect(status().isBadRequest());
    }

    @Test
    void listingsOutsideContiguousUsAreRejected() throws Exception {
        Account landlord = register("LANDLORD");

        Map<String, Object> alaska = bostonListing();
        alaska.put("state", "AK");
        mvc.perform(auth(post("/api/v1/listings"), landlord).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(alaska)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Alaska, Hawaii, Puerto Rico")));

        Map<String, Object> hawaiiZip = bostonListing();
        hawaiiZip.put("zipCode", "96813");
        mvc.perform(auth(post("/api/v1/listings"), landlord).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(hawaiiZip))).andExpect(status().isBadRequest());

        Map<String, Object> puertoRicoCoords = bostonListing();
        puertoRicoCoords.put("latitude", 18.4655);
        puertoRicoCoords.put("longitude", -66.1057);
        mvc.perform(auth(post("/api/v1/listings"), landlord).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(puertoRicoCoords))).andExpect(status().isBadRequest());

        Map<String, Object> wholePlaceAsRoommate = bostonListing();
        wholePlaceAsRoommate.put("roomType", "ENTIRE_PLACE");
        mvc.perform(auth(post("/api/v1/listings"), landlord).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(wholePlaceAsRoommate))).andExpect(status().isBadRequest());

        Map<String, Object> invalid = bostonListing();
        invalid.remove("title");
        invalid.put("monthlyRent", -5);
        mvc.perform(auth(post("/api/v1/listings"), landlord).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.monthlyRent").exists());

        mvc.perform(post("/api/v1/listings").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(bostonListing()))).andExpect(status().isUnauthorized());
    }

    @Test
    void createSearchUpdateAndArchiveListing() throws Exception {
        Account landlord = register("LANDLORD");
        Account other = register("LANDLORD");
        long northeastern = universityId("Northeastern University");
        long ucla = universityId("University of California, Los Angeles");

        Map<String, Object> listing = bostonListing();
        listing.put("title", "Search-test room " + UUID.randomUUID());
        long id = createListing(landlord, listing);

        mvc.perform(get("/api/v1/listings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nearestUniversity.name").value("Northeastern University"))
                .andExpect(jsonPath("$.distanceToUniversityMiles").isNumber())
                .andExpect(jsonPath("$.owner.fullName").value("Test LANDLORD"))
                .andExpect(jsonPath("$.owner.email").doesNotExist());

        String title = (String) listing.get("title");
        mvc.perform(get("/api/v1/listings").param("q", title).param("universityId", String.valueOf(northeastern))
                        .param("radiusMiles", "2").param("noSsnRequired", "true").param("maxRent", "1500")
                        .param("amenities", "WIFI_INCLUDED", "IN_UNIT_LAUNDRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].distanceMiles").isNumber());

        mvc.perform(get("/api/v1/listings").param("q", title).param("universityId", String.valueOf(ucla)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/listings").param("q", title).param("maxRent", "1000"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/listings").param("q", title).param("amenities", "POOL"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/listings").param("q", title).param("state", "ma").param("city", "boston")
                        .param("category", "ROOMMATE").param("sort", "monthlyRent,asc"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/listings").param("sort", "passwordHash")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/listings").param("state", "PR")).andExpect(status().isBadRequest());

        listing.put("monthlyRent", 1300);
        mvc.perform(auth(put("/api/v1/listings/{id}", id), other).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(listing))).andExpect(status().isForbidden());
        mvc.perform(auth(put("/api/v1/listings/{id}", id), landlord).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(listing)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRent").value(1300));

        mvc.perform(auth(patch("/api/v1/listings/{id}/status", id), landlord).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAUSED\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/listings").param("q", title)).andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(auth(delete("/api/v1/listings/{id}", id), landlord)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/listings/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/listings/mine"), landlord)).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void favorites() throws Exception {
        Account landlord = register("LANDLORD");
        Account student = register("STUDENT");
        long id = createListing(landlord, bostonListing());

        mvc.perform(auth(put("/api/v1/favorites/{id}", id), student)).andExpect(status().isNoContent());
        mvc.perform(auth(put("/api/v1/favorites/{id}", id), student)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/v1/favorites"), student))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(auth(delete("/api/v1/favorites/{id}", id), student)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/v1/favorites"), student)).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void studentMessagesLandlordAboutListing() throws Exception {
        Account landlord = register("LANDLORD");
        Account student = register("STUDENT");
        Account stranger = register("STUDENT");
        long listingId = createListing(landlord, bostonListing());

        long conversationId = read(mvc.perform(auth(post("/api/v1/conversations"), student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listingId\":" + listingId + ",\"message\":\"Hi! Is the room available in January?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listing.id").value(listingId))
                .andExpect(jsonPath("$.otherParticipant.id").value(landlord.id()))).get("id").asLong();

        // Second message from the same student reuses the thread.
        mvc.perform(auth(post("/api/v1/conversations"), student).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listingId\":" + listingId + ",\"message\":\"Also, do you accept I-20 as proof of funds?\"}"))
                .andExpect(jsonPath("$.id").value(conversationId));

        mvc.perform(auth(get("/api/v1/conversations"), landlord))
                .andExpect(jsonPath("$.content[0].id").value(conversationId))
                .andExpect(jsonPath("$.content[0].unreadCount").value(2));

        mvc.perform(auth(get("/api/v1/conversations/{id}/messages", conversationId), landlord))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].body", containsString("I-20")));
        mvc.perform(auth(get("/api/v1/conversations"), landlord))
                .andExpect(jsonPath("$.content[0].unreadCount").value(0));

        mvc.perform(auth(post("/api/v1/conversations/{id}/messages", conversationId), landlord)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Yes to both!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderId").value(landlord.id()));

        mvc.perform(auth(get("/api/v1/conversations/{id}/messages", conversationId), stranger))
                .andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/v1/conversations"), landlord).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listingId\":" + listingId + ",\"message\":\"talking to myself\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roommateMatching() throws Exception {
        Account alice = register("STUDENT", "FEMALE");
        Account bea = register("STUDENT", "FEMALE");
        Account carl = register("STUDENT", "MALE");
        long bu = universityId("Boston University");
        String city = "Boston-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> profile = new HashMap<>();
        profile.put("headline", "Grad student looking for a quiet roommate");
        profile.put("budgetMin", 900);
        profile.put("budgetMax", 1400);
        profile.put("preferredState", "MA");
        profile.put("preferredCity", city);
        profile.put("universityId", bu);
        profile.put("moveInDate", LocalDate.now().plusMonths(2).toString());
        profile.put("genderPreference", "FEMALE_ONLY");
        profile.put("sleepSchedule", "EARLY_BIRD");
        profile.put("cleanlinessLevel", 4);

        mvc.perform(auth(put("/api/v1/roommates/me"), alice).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.university.name").value("Boston University"));

        mvc.perform(auth(get("/api/v1/roommates"), bea).param("city", city).param("maxBudget", "1000")
                        .param("nonSmoker", "true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].gender").value("FEMALE"));
        mvc.perform(auth(get("/api/v1/roommates"), bea).param("city", city).param("maxBudget", "800"))
                .andExpect(jsonPath("$.totalElements").value(0));
        // Alice only wants female roommates, so Carl should not see her.
        mvc.perform(auth(get("/api/v1/roommates"), carl).param("city", city))
                .andExpect(jsonPath("$.totalElements").value(0));
        // You never see yourself.
        mvc.perform(auth(get("/api/v1/roommates"), alice).param("city", city))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/roommates")).andExpect(status().isUnauthorized());

        // Direct message from a roommate match.
        mvc.perform(auth(post("/api/v1/conversations"), bea).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientId\":" + alice.id() + ",\"message\":\"Hi Alice, want to room together?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listing").doesNotExist());

        profile.put("state", "HI");
        profile.put("preferredState", "HI");
        mvc.perform(auth(put("/api/v1/roommates/me"), alice).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(profile))).andExpect(status().isBadRequest());

        mvc.perform(auth(delete("/api/v1/roommates/me"), alice)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/v1/roommates"), bea).param("city", city))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void openApiDocsAreServed() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Livings API"));
    }
}
