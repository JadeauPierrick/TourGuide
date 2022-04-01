package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
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
	@Autowired
	private GpsUtilProxy gpsUtilProxy;
	@Autowired
	private RewardCentralProxy rewardCentralProxy;

	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}
	
	public void calculateRewards(User user) {
		List<tourGuide.beans.VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
		List<tourGuide.beans.Attraction> attractions = gpsUtilProxy.getAttractions();
		
		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {
				if(user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
					if(nearAttraction(visitedLocation, attraction)) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
				}
			}
		}
	}

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

	public boolean isWithinAttractionProximity(tourGuide.beans.Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(tourGuide.beans.VisitedLocation visitedLocation, tourGuide.beans.Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(tourGuide.beans.Attraction attraction, User user) {
		return rewardCentralProxy.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(tourGuide.beans.Attraction loc1, tourGuide.beans.Location loc2) {
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

}
