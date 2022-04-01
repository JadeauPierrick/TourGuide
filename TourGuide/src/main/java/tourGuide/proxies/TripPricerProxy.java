package tourGuide.proxies;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tourGuide.beans.Provider;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "tripPricer", url = "http://localhost:8083")
public interface TripPricerProxy {

    @GetMapping(value = "/price")
    List<Provider> getPrice(@RequestParam("tripPricerApiKey") String tripPricerApiKey, @RequestParam("attractionId")UUID attractionId,
                            @RequestParam("adults") int adults, @RequestParam("children") int children, @RequestParam("nightsStay") int nightsStay,
                            @RequestParam("rewardsPoints") int rewardsPoints);
}
