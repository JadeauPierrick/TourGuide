package tourGuide.proxies;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tourGuide.beans.Attraction;
import tourGuide.beans.VisitedLocation;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "gpsUtil", url = "localhost:8081")
public interface GpsUtilProxy {

    @GetMapping(value = "/userLocation")
    VisitedLocation getUserLocation(@RequestParam UUID userId);

    @GetMapping(value = "/attractions")
    List<Attraction> getAttractions();
}