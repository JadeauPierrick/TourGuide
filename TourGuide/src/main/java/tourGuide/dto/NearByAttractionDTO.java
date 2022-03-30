package tourGuide.dto;

import gpsUtil.location.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearByAttractionDTO {

    private Location userLocation;

    private List<AttractionDTO> attractions;
}
