package tourGuide.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.jsoniter.output.JsonStream;

import tourGuide.beans.Location;
import tourGuide.beans.Provider;
import tourGuide.beans.VisitedLocation;
import tourGuide.dto.UserPreferencesDTO;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;

@RestController
public class TourGuideController {

	private final TourGuideService tourGuideService;

	private final RewardsService rewardsService;

    public TourGuideController(TourGuideService tourGuideService, RewardsService rewardsService) {
        this.tourGuideService = tourGuideService;
        this.rewardsService = rewardsService;
    }

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public String getLocation(@RequestParam String userName) {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(tourGuideService.getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
    }

    @RequestMapping("/getNearbyAttractions") 
    public String getNearbyAttractions(@RequestParam String userName) {
        User user = tourGuideService.getUser(userName);
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(tourGuideService.getUser(userName));
    	return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation, user));
    }
    
    @RequestMapping("/getRewards") 
    public String getRewards(@RequestParam String userName) {
    	return JsonStream.serialize(rewardsService.getUserRewards(tourGuideService.getUser(userName)));
    }
    
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
        Map<String, Location> allCurrentLocations = tourGuideService.getAllCurrentLocations();
    	return JsonStream.serialize(allCurrentLocations);
    }
    
    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(tourGuideService.getUser(userName));
    	return JsonStream.serialize(providers);
    }

    @GetMapping(value = "/getUserPreferences")
    public String getUserPreferences(@RequestParam String userName) {
        UserPreferencesDTO userPreferencesDTO = tourGuideService.getUserPreferences(userName);
        return JsonStream.serialize(userPreferencesDTO);
    }

    @PutMapping("/updateUserPreferences")
    public String updateUserPreferences(@RequestParam String userName, @RequestBody UserPreferencesDTO userPreferencesDTO) {
        UserPreferencesDTO newUserPreferences = tourGuideService.updateUserPreferences(userName, userPreferencesDTO);
        return JsonStream.serialize(newUserPreferences);
    }
}