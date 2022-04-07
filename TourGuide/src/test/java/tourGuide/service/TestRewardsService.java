package tourGuide.service;


import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tourGuide.beans.Attraction;
import tourGuide.beans.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.user.User;
import tourGuide.user.UserReward;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class TestRewardsService {

	@Autowired
	private RewardsService rewardsService;

	@Autowired
	private GpsUtilProxy gpsUtilProxy;

	private User user;

	@BeforeAll
	public void setUp() {
		Locale.setDefault(Locale.US);

		user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
	}

	@Test
	public void userGetRewards() {
		Attraction attraction = gpsUtilProxy.getAttractions().get(0);
		user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
		rewardsService.calculateRewards(user);

		List<UserReward> userRewards = user.getUserRewards();

		assertThat(userRewards.size()).isEqualTo(1);
	}
	
	@Test
	public void isWithinAttractionProximity() {
		Attraction attraction = gpsUtilProxy.getAttractions().get(0);
		Assertions.assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}
	
	@Test
	public void nearAllAttractions() {
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);

		InternalTestHelper.setInternalUserNumber(1);
		TourGuideService tourGuideService = new TourGuideService(gpsUtilProxy, rewardsService);
		
		rewardsService.calculateRewards(tourGuideService.getAllUsers().get(0));
		List<UserReward> userRewards = rewardsService.getUserRewards(tourGuideService.getAllUsers().get(0));

		Assertions.assertEquals(gpsUtilProxy.getAttractions().size(), userRewards.size());
	}
	
}
