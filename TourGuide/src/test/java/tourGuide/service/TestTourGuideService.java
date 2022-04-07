package tourGuide.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import tourGuide.beans.Location;
import tourGuide.beans.Provider;
import tourGuide.beans.VisitedLocation;
import tourGuide.dto.NearByAttractionDTO;
import tourGuide.dto.UserPreferencesDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;

@SpringBootTest
public class TestTourGuideService {

	@Autowired
	private TourGuideService tourGuideService;

	@Autowired
	private GpsUtilProxy gpsUtilProxy;

	@Autowired
	private RewardsService rewardsService;

	@MockBean
	private InternalTestHelper internalTestHelper;

	private User user;
	private Map<String, User> userMap;


	@BeforeAll
	public void setUp() {
		Locale.setDefault(Locale.US);
		InternalTestHelper.setInternalUserNumber(0);

		userMap = new HashMap<>();
		user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		userMap.put(user.getUserName(), user);

		when(internalTestHelper.getInternalUserMap()).thenReturn(userMap);

		tourGuideService.tracker.stopTracking();
	}

	@Test
	public void getUserLocation() {
		VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);
		assertThat(visitedLocation.getUserId()).isEqualTo(user.getUserId());
	}

	@Test
	public void getUser() {
		User u = tourGuideService.getUser(user.getUserName());
		assertThat(u.getUserName()).isEqualTo(user.getUserName());
	}

	@Test
	public void addUser() {
		User newUser = new User(UUID.randomUUID(),"Antoine","090909","toto@tourGuide.com");
		tourGuideService.addUser(newUser);

		assertThat(userMap.size()).isEqualTo(2);
	}
	
	@Test
	public void getAllUsers() {
		List<User> allUsers = tourGuideService.getAllUsers();

		Assertions.assertTrue(allUsers.contains(user));
	}
	
	@Test
	public void trackUser() {
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

		assertThat(visitedLocation.getUserId()).isEqualTo(user.getUserId());
	}
	
	@Test
	public void getNearbyAttractions() {
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

		NearByAttractionDTO nearByAttractionDTO = tourGuideService.getNearByAttractions(visitedLocation,user);

		assertThat(nearByAttractionDTO.getAttractions().size()).isEqualTo(5);
	}

	@Test
	public void getTripDeals() {
		List<Provider> providers = tourGuideService.getTripDeals(user);

		assertThat(providers.size()).isEqualTo(5);
	}
	
	@Test
	public void getAllCurrentLocations() {
		Map<String, Location> allCurrentLocations = tourGuideService.getAllCurrentLocations();

		assertThat(allCurrentLocations.get(user.getUserId().toString())).isEqualTo(user.getLastVisitedLocation());
	}

	@Test
	public void updateUserPreferences() {
		UserPreferencesDTO userPreferencesDTO = new UserPreferencesDTO();
		userPreferencesDTO.setAttractionProximity(50);
		userPreferencesDTO.setNumberOfAdults(3);
		userPreferencesDTO.setNumberOfChildren(5);
		userPreferencesDTO.setTripDuration(10);

		UserPreferences userPreferences = tourGuideService.updateUserPreferences(user,userPreferencesDTO);

		assertThat(userPreferences.getAttractionProximity()).isEqualTo(50);
		assertThat(userPreferences.getNumberOfAdults()).isEqualTo(3);
		assertThat(userPreferences.getNumberOfChildren()).isEqualTo(5);
		assertThat(userPreferences.getTripDuration()).isEqualTo(10);
	}
}
