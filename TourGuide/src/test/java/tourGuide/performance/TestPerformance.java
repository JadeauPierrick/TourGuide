package tourGuide.performance;

import org.apache.commons.lang3.time.StopWatch;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tourGuide.beans.Attraction;
import tourGuide.beans.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.proxies.RewardCentralProxy;
import tourGuide.proxies.TripPricerProxy;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class TestPerformance {

    @Autowired
    private GpsUtilProxy gpsUtilProxy;

    @Autowired
    private RewardCentralProxy rewardCentralProxy;

    @Autowired
    private TripPricerProxy tripPricerProxy;

    private int visitations;

    @BeforeAll
    static void setUp() {
        Locale.setDefault(Locale.US);
    }


    @Test
    public void highVolumeTrackLocation() {
        // Users should be incremented up to 100,000, and test finishes within 15 minutes
        RewardsService rewardsService = new RewardsService(gpsUtilProxy, rewardCentralProxy);
        InternalTestHelper.setInternalUserNumber(100);
        TourGuideService tourGuideService = new TourGuideService(gpsUtilProxy, rewardsService, tripPricerProxy,true, false);

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
        RewardsService rewardsService = new RewardsService(gpsUtilProxy, rewardCentralProxy);
        InternalTestHelper.setInternalUserNumber(100);
        TourGuideService tourGuideService = new TourGuideService(gpsUtilProxy, rewardsService,tripPricerProxy, true,false);

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