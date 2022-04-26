package tourGuide.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


import java.util.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tourGuide.beans.Attraction;
import tourGuide.beans.Location;
import tourGuide.beans.Provider;
import tourGuide.beans.VisitedLocation;
import tourGuide.dto.NearByAttractionDTO;
import tourGuide.dto.UserPreferencesDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.proxies.TripPricerProxy;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.user.UserReward;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTourGuideService {

    @Autowired
    private GpsUtilProxy gpsUtilProxy;

    @Autowired
    private RewardsService rewardsService;

    @Autowired
    private TripPricerProxy tripPricerProxy;

    private TourGuideService tourGuideService;

    private User user;

    @BeforeAll
    public void setUp() {
        Locale.setDefault(Locale.US);
        InternalTestHelper.setInternalUserNumber(1);
        tourGuideService = new TourGuideService(gpsUtilProxy,rewardsService,tripPricerProxy,true,false);
        user = tourGuideService.getAllUsers().get(0);
    }


    @Test
    @Order(1)
    public void getUserLocation() {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
        assertThat(visitedLocation.getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    @Order(2)
    public void getUser() {
        User u = tourGuideService.getUser(user.getUserName());
        assertThat(u.getUserName()).isEqualTo(user.getUserName());
    }

    @Test
    @Order(3)
    public void addUser() {
        User newUser = new User(UUID.randomUUID(),"Antoine","090909","toto@tourGuide.com");
        tourGuideService.addUser(newUser);

        assertThat(tourGuideService.getAllUsers().size()).isEqualTo(2);
    }

    @Test
    @Order(4)
    public void getAllUsers() {
        List<User> allUsers = tourGuideService.getAllUsers();

        Assertions.assertTrue(allUsers.contains(user));
    }

    @Test
    @Order(5)
    public void trackUserLocation() {
        VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

        assertThat(visitedLocation.getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    @Order(6)
    public void getNearbyAttractions() {
        VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

        NearByAttractionDTO nearByAttractionDTO = tourGuideService.getNearByAttractions(visitedLocation,user);

        assertThat(nearByAttractionDTO.getAttractions().size()).isEqualTo(5);
    }

    @Test
    @Order(7)
    public void getTripDeals() {
        List<UserReward> userRewards = new ArrayList<>();
        Attraction attraction = new Attraction("Papéa", "Le Mans", "France", UUID.randomUUID());
        UserReward newReward = new UserReward(user.getLastVisitedLocation(), attraction, 10);
        userRewards.add(newReward);
        user.setUserRewards(userRewards);

        UserPreferences userPref = new UserPreferences();
        userPref.setNumberOfAdults(2);
        userPref.setNumberOfChildren(2);
        userPref.setTripDuration(10);
        user.setUserPreferences(userPref);

        List<Provider> providers = tourGuideService.getTripDeals(user);

        assertThat(providers.size()).isEqualTo(5);
    }

    @Test
    @Order(8)
    public void getAllCurrentLocations() {
        Map<String, Location> allCurrentLocations = tourGuideService.getAllCurrentLocations();

        assertThat(allCurrentLocations.get(user.getUserId().toString())).isEqualTo(user.getLastVisitedLocation().location);
    }

    @Test
    @Order(9)
    public void getUserPreferences() {
        UserPreferencesDTO userPreferencesDTO = tourGuideService.getUserPreferences(user.getUserName());

        assertThat(userPreferencesDTO.getTripDuration()).isEqualTo(10);
        assertThat(userPreferencesDTO.getNumberOfAdults()).isEqualTo(2);
        assertThat(userPreferencesDTO.getNumberOfChildren()).isEqualTo(2);
    }

    @Test
    @Order(10)
    public void updateUserPreferences() {
        UserPreferencesDTO userPreferencesDTO = new UserPreferencesDTO();
        userPreferencesDTO.setAttractionProximity(50);
        userPreferencesDTO.setNumberOfAdults(3);
        userPreferencesDTO.setNumberOfChildren(5);
        userPreferencesDTO.setTripDuration(15);

        UserPreferencesDTO userPreferences = tourGuideService.updateUserPreferences(user.getUserName(),userPreferencesDTO);

        assertThat(userPreferences.getAttractionProximity()).isEqualTo(50);
        assertThat(userPreferences.getNumberOfAdults()).isEqualTo(3);
        assertThat(userPreferences.getNumberOfChildren()).isEqualTo(5);
        assertThat(userPreferences.getTripDuration()).isEqualTo(15);
    }
}
