package tourGuide.service;


import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tourGuide.beans.Attraction;
import tourGuide.beans.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.proxies.RewardCentralProxy;
import tourGuide.user.User;
import tourGuide.user.UserReward;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestRewardsService {

	@Autowired
	private RewardCentralProxy rewardCentralProxy;

	@Autowired
	private GpsUtilProxy gpsUtilProxy;


	@BeforeAll
	public void setUp() {
		Locale.setDefault(Locale.US);
	}

	@Test
	@Order(1)
	public void userGetRewards() {
		RewardsService rewardsService = new RewardsService(gpsUtilProxy,rewardCentralProxy);
		Attraction attraction = gpsUtilProxy.getAttractions().get(0);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

		user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
		rewardsService.calculateRewards(user);

		List<UserReward> userRewards = user.getUserRewards();

		assertThat(userRewards.size()).isEqualTo(1);
	}
	
	@Test
	@Order(2)
	public void isWithinAttractionProximity() {
		RewardsService rewardsService = new RewardsService(gpsUtilProxy, rewardCentralProxy);
		Attraction attraction = gpsUtilProxy.getAttractions().get(0);
		Assertions.assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}
	
	@Test
	@Order(3)
	public void nearAllAttractions() {
		RewardsService rewardsService = new RewardsService(gpsUtilProxy, rewardCentralProxy);
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);

		InternalTestHelper.setInternalUserNumber(1);
		TourGuideService tourGuideService = new TourGuideService(gpsUtilProxy, rewardsService);

		rewardsService.calculateRewards(tourGuideService.getAllUsers().get(0));
		List<UserReward> userRewards = rewardsService.getUserRewards(tourGuideService.getAllUsers().get(0));
		tourGuideService.tracker.stopTracking();

		Assertions.assertEquals(gpsUtilProxy.getAttractions().size(), userRewards.size());
	}
	
}
