package tourGuide.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttractionDTO {

    private String name;

    private double attractionLatitude;

    private double attractionLongitude;

    private double distance;

    private int rewardPoints;
}
