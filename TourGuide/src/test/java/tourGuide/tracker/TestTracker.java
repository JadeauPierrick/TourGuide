package tourGuide.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tourGuide.helper.InternalTestHelper;
import tourGuide.proxies.GpsUtilProxy;
import tourGuide.proxies.RewardCentralProxy;
import tourGuide.proxies.TripPricerProxy;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TestTracker {

    @Autowired
    private GpsUtilProxy gpsUtilProxy;

    @Autowired
    private RewardCentralProxy rewardCentralProxy;

    @Autowired
    private TripPricerProxy tripPricerProxy;

    private int visitations;

    @Test
    public void run() {
        RewardsService rewardsService = new RewardsService(gpsUtilProxy,rewardCentralProxy);
        InternalTestHelper.setInternalUserNumber(100);
        TourGuideService tourGuideService = new TourGuideService(gpsUtilProxy,rewardsService,tripPricerProxy,true,true);

        List<User> allUsers = tourGuideService.getAllUsers();

        while (visitations < allUsers.size()) {
            visitations = 0;
            allUsers.forEach(user -> {
                if (user.getVisitedLocations().size() > 0) {
                    visitations++;
                }
            });
        }

        tourGuideService.tracker.stopTracking();

        assertEquals(visitations, allUsers.size());

    }
}
