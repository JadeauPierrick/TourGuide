package tourGuide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tourGuide.beans.Attraction;
import tourGuide.beans.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;

@SpringBootTest
public class TestPerformance {

	@Autowired
	private GpsUtilProxy gpsUtilProxy;

	@Autowired
	private RewardsService rewardsService;



	private int visitations;
	
	/*
	 * A note on performance improvements:
	 *     
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *     
	 *     		InternalTestHelper.setInternalUserNumber(100000);
	 *     
	 *     
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent. 
	 * 
	 *     These are performance metrics that we are trying to hit:
	 *     
	 *     highVolumeTrackLocation: 100,000 users within 15 minutes:
	 *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     *     highVolumeGetRewards: 100,000 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */

	@Before
	public void setUp() {
		Locale.setDefault(Locale.US);
	}

	@Test
	public void highVolumeTrackLocation() {
		// Users should be incremented up to 100,000, and test finishes within 15 minutes
		InternalTestHelper.setInternalUserNumber(100000);
		TourGuideService tourGuideService = new TourGuideService(rewardsService, new InternalTestHelper());
		tourGuideService.tracker.stopTracking();

		List<User> allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(User::clearVisitedLocations);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		tourGuideService.trackSeveralUsersLocation(allUsers);
		stopWatch.stop();

		while (visitations < allUsers.size()) {
			visitations = 0;
			allUsers.forEach(user -> {
				if (user.getVisitedLocations().size() > 0) {
					visitations++;
				}
			});
		}

		System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
		assertEquals(visitations, allUsers.size());
	}

	@Test
	public void highVolumeGetRewards() {
		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		InternalTestHelper.setInternalUserNumber(100000);

		TourGuideService tourGuideService = new TourGuideService(rewardsService, new InternalTestHelper());
		tourGuideService.tracker.stopTracking();
		List<User> allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(User::clearVisitedLocations);
		allUsers.forEach(u-> u.setUserRewards(new ArrayList<>()));

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		Attraction attraction = gpsUtilProxy.getAttractions().get(0);

		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

		rewardsService.calculateSeveralRewards(allUsers);

		stopWatch.stop();

		for (User user : allUsers) {
			assertTrue(user.getUserRewards().size() > 0);
		}


		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	
}
