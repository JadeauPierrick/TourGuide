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
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtilProxy gpsUtilProxy;
	private final RewardsService rewardsService;
	@Autowired
	private TripPricerProxy tripPricerProxy;
	public final Tracker tracker;
	boolean testMode = true;
	
	public TourGuideService(GpsUtilProxy gpsUtilProxy, RewardsService rewardsService) {
		this.gpsUtilProxy = gpsUtilProxy;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}
	
	public tourGuide.beans.VisitedLocation getUserLocation(User user) {
		tourGuide.beans.VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user);
			rewardsService.calculateRewards(user);
		return visitedLocation;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<tourGuide.beans.Provider> providers = tripPricerProxy.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}
	
	public tourGuide.beans.VisitedLocation trackUserLocation(User user) {
		Locale.setDefault(new Locale("en", "US"));
		tourGuide.beans.VisitedLocation visitedLocation = gpsUtilProxy.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		return visitedLocation;
	}

	public void trackSeveralUsersLocation(List<User> userList) {
		logger.info("Multithreading trackSeveralUserLocation begins");
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

	public NearByAttractionDTO getNearByAttractions(VisitedLocation visitedLocation, User user) {
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

	public Map<UUID, tourGuide.beans.Location> getAllCurrentLocations() {
		Map<UUID, tourGuide.beans.Location> allCurrentLocations = new HashMap<>();
		List<User> allUsers = getAllUsers();

		allUsers.forEach(user -> allCurrentLocations.put(user.getUserId(), getUserLocation(user).getLocation()));

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
			user.addToVisitedLocations(new tourGuide.beans.VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
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
