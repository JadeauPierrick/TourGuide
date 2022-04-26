package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tourGuide.beans.Attraction;
import tourGuide.beans.Location;
import tourGuide.beans.VisitedLocation;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.proxies.RewardCentralProxy;
import tourGuide.user.User;
import tourGuide.user.UserReward;

@Service
public class RewardsService {
	private Logger logger = LoggerFactory.getLogger(RewardsService.class);
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtilProxy gpsUtilProxy;
	private final RewardCentralProxy rewardCentralProxy;

	@Autowired
	public RewardsService(GpsUtilProxy gpsUtilProxy, RewardCentralProxy rewardCentralProxy) {
		this.gpsUtilProxy = gpsUtilProxy;
		this.rewardCentralProxy = rewardCentralProxy;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public int getProximityBuffer() {
		return proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	/**
	 *Calculate the reward based on his lasted visited location. If he's close to an attraction and he has not already
	 *a reward for it, then he gets a reward
	 *
	 * @param user the user whose the reward we want to calculate
	 */
	public void calculateRewards(User user) {
		logger.info("Calculate the reward of " + user.getUserName());
		List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<Attraction> attractions = gpsUtilProxy.getAttractions();
		
		for(Attraction attraction : attractions) {
			for(VisitedLocation visitedLocation : userLocations) {
				if(user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
					if(nearAttraction(visitedLocation, attraction)) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
				}
			}
		}
	}

	/**
	 * Calculate the reward of each user on the list
	 *
	 * @param userList the list of the users
	 */
	public void calculateSeveralRewards(List<User> userList) {
		logger.info("Multithreading calculateSeveralRewards begins");
		ExecutorService executor = Executors.newFixedThreadPool(100);
		List<Future<?>> results = new ArrayList<>();

		for (User user : userList) {
			Future<?> future = executor.submit(()->{
				calculateRewards(user);
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
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardCentralProxy.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Attraction loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}
}
