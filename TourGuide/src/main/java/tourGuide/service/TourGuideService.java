package tourGuide.service;


import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private GpsUtilProxy gpsUtilProxy;
	@Autowired
	private TripPricerProxy tripPricerProxy;

	private final RewardsService rewardsService;
	private final InternalTestHelper internalTestHelper;

	public final Tracker tracker;
	boolean testMode = true;
	private static final String tripPricerApiKey = "test-server-api-key";


	public TourGuideService(RewardsService rewardsService, InternalTestHelper internalTestHelper) {
		this.rewardsService = rewardsService;
		this.internalTestHelper = internalTestHelper;

		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			this.internalTestHelper.initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
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
		return internalTestHelper.getInternalUserMap().get(userName);
	}
	
	public List<User> getAllUsers() {
		logger.info("Get all users");
		return internalTestHelper.getInternalUserMap().values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		logger.info("Add user : " + user.getUserName());
		if(!internalTestHelper.getInternalUserMap().containsKey(user.getUserName())) {
			internalTestHelper.getInternalUserMap().put(user.getUserName(), user);
		}
	}

	/**
	 *
	 *
	 * @param user
	 * @return
	 */
	public List<Provider> getTripDeals(User user) {
		logger.info("Get trip deals of " + user.getUserName());
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
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

	public UserPreferences updateUserPreferences(User user, UserPreferencesDTO newUserPreferences) {
		UserPreferences currentPreferences = user.getUserPreferences();
		if (newUserPreferences.getAttractionProximity() >= 0) {
			currentPreferences.setAttractionProximity(newUserPreferences.getAttractionProximity());
		}
		if (newUserPreferences.getLowerPricePoint() != null) {
			currentPreferences.setLowerPricePoint(newUserPreferences.getLowerPricePoint());
		}
		if (newUserPreferences.getHighPricePoint() != null) {
			currentPreferences.setHighPricePoint(newUserPreferences.getHighPricePoint());
		}
		if (newUserPreferences.getTripDuration() >= 0) {
			currentPreferences.setTripDuration(newUserPreferences.getTripDuration());
		}
		if (newUserPreferences.getTicketQuantity() >= 0) {
			currentPreferences.setTicketQuantity(newUserPreferences.getTicketQuantity());
		}
		if (newUserPreferences.getNumberOfAdults() >= 0) {
			currentPreferences.setNumberOfAdults(newUserPreferences.getNumberOfAdults());
		}
		if (newUserPreferences.getNumberOfChildren() >= 0) {
			currentPreferences.setNumberOfChildren(newUserPreferences.getNumberOfChildren());
		}
		return currentPreferences;
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
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	
}
