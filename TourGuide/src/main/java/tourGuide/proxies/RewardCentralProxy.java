package tourGuide.proxies;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "rewardCentral", url = "http://localhost:8082")
public interface RewardCentralProxy {

    @GetMapping(value = "/getAttractionRewardPoints")
    int getAttractionRewardPoints(@RequestParam("attractionId")UUID attractionId, @RequestParam("userId") UUID userId);
}
