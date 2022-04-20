package tourGuide.service;


import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tourGuide.beans.Attraction;
import tourGuide.beans.Location;
import tourGuide.beans.Provider;
import tourGuide.beans.VisitedLocation;
import tourGuide.dto.AttractionDTO;
import tourGuide.dto.NearByAttractionDTO;
import tourGuide.dto.UserPreferencesDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.proxies.TripPricerProxy;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.user.UserReward;

@Service
public class TourGuideService {
	private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);

	private final GpsUtilProxy gpsUtilProxy;
	private final RewardsService rewardsService;
	private final TripPricerProxy tripPricerProxy;

	public final Tracker tracker;

	@Autowired
	public TourGuideService(GpsUtilProxy gpsUtilProxy, RewardsService rewardsService, TripPricerProxy tripPricerProxy, @Value("${testmode}") boolean testMode, @Value("${tracking}") boolean tracking) {
		this.gpsUtilProxy = gpsUtilProxy;
		this.rewardsService = rewardsService;
		this.tripPricerProxy = tripPricerProxy;

		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		if (tracking) {
			tracker = new Tracker(this);
			addShutDownHook();
		} else {
			tracker = null;
		}

	}

	/**
	 * Get the last visited location. If there isn't, call of the trackUserLocation method to get one.
	 *
	 * @param user the user whose location we want to obtain
	 * @return the last visited location
	 */
	public VisitedLocation getUserLocation(User user) {
		logger.info("Get the last visited location of " + user.getUserName());
		if (user.getVisitedLocations().size() > 0) {
			return user.getLastVisitedLocation();
		}else {
			VisitedLocation visitedLocation = trackUserLocation(user);
			rewardsService.calculateRewards(user);
			return visitedLocation;
		}
	}
	
	public User getUser(String userName) {
		logger.info("Get the user : " + userName);
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		logger.info("Get all users");
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		logger.info("Add user : " + user.getUserName());
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public UserPreferencesDTO getUserPreferences(String userName) {
		logger.info("Get preferences of " + userName);
		UserPreferencesDTO userPreferencesDTO = new UserPreferencesDTO();
		getUser(userName).getUserPreferences().mapToUserPreferencesDTO(userPreferencesDTO);
		return userPreferencesDTO;
	}

	/**
	 * Get 5 trip deals based on user's preferences and his rewards points
	 *
	 * @param user the user whose trip deals we want to obtain
	 * @return 5 trip deals
	 */
	public List<Provider> getTripDeals(User user) {
		logger.info("Get trip deals of " + user.getUserName());
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();
		List<Provider> providers = tripPricerProxy.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Get the current location of one user
	 *
	 * @param user the user whose location we want to obtain
	 * @return the current location
	 */
	public VisitedLocation trackUserLocation(User user) {
		logger.info("Research the location of " + user.getUserName());
		Locale.setDefault(new Locale("en", "US"));
		VisitedLocation visitedLocation = gpsUtilProxy.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		return visitedLocation;
	}

	/**
	 * Get all the current locations of each user on the list
	 *
	 * @param userList the list of the users
	 */
	public void trackSeveralUsersLocation(List<User> userList) {
		logger.info("Multithreading to get all locations");
		ExecutorService executor = Executors.newFixedThreadPool(100);
		List<Future<?>> results = new ArrayList<>();

		for (User user : userList) {
			Future<?> future = executor.submit(()->{
				trackUserLocation(user);
			});
			results.add(future);
		}

		results.forEach(r-> {
			try {
				r.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});

		executor.shutdown();
		rewardsService.calculateSeveralRewards(userList);
	}

	/**
	 * Get the 5 nearest attractions based on a user's location
	 *
	 * @param visitedLocation the last visited location of the user
	 * @param user the user in question
	 * @return the 5 nearest attractions
	 */
	public NearByAttractionDTO getNearByAttractions(VisitedLocation visitedLocation, User user) {
		logger.info("Get the 5 nearest attractions for " + user.getUserName());
		NearByAttractionDTO nearByAttractionDTO = new NearByAttractionDTO();
		nearByAttractionDTO.setUserLocation(visitedLocation.location);

		List<AttractionDTO> attractionsDTOList = new ArrayList<>();
		List<Attraction> attractions = gpsUtilProxy.getAttractions();

		attractions.forEach(attraction -> {
			AttractionDTO attractionDTO = new AttractionDTO();
			attractionDTO.setName(attraction.attractionName);
			attractionDTO.setAttractionLatitude(attraction.latitude);
			attractionDTO.setAttractionLongitude(attraction.longitude);
			attractionDTO.setDistance(rewardsService.getDistance(attraction, visitedLocation.location));
			attractionDTO.setRewardPoints(rewardsService.getRewardPoints(attraction, user));
			attractionsDTOList.add(attractionDTO);
		});

		List<AttractionDTO> finalAttractionDTOList = attractionsDTOList.stream()
														.sorted(Comparator.comparingDouble(AttractionDTO::getDistance))
														.limit(5)
														.collect(Collectors.toList());

		nearByAttractionDTO.setAttractions(finalAttractionDTOList);

		return nearByAttractionDTO;
	}

	/**
	 * Get all the current locations of all users on a map
	 *
	 * @return a map of all locations
	 */
	public Map<String, Location> getAllCurrentLocations() {
		Map<String, Location> allCurrentLocations = new HashMap<>();
		List<User> allUsers = getAllUsers();

		allUsers.forEach(user -> allCurrentLocations.put(user.getUserId().toString(), getUserLocation(user).getLocation()));

		return allCurrentLocations;
	}

	public UserPreferencesDTO updateUserPreferences(String userName, UserPreferencesDTO newUserPreferences) {
		logger.info("Update preferences of " + userName);
		getUser(userName).getUserPreferences().mapDTOToUserPreferences(newUserPreferences);

		return getUserPreferences(userName);
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
		      public void run() {
		        tracker.stopTracking();
		      } 
		    }); 
	}

	/**********************************************************************************
	 *
	 * Methods Below: For Internal Testing
	 *
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
