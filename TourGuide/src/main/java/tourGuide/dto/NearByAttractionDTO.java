package tourGuide.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tourGuide.beans.Location;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearByAttractionDTO {

    private Location userLocation;

    private List<AttractionDTO> attractions;
}
