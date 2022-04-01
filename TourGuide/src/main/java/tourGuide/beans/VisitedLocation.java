package tourGuide.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitedLocation {

    public UUID userId;

    public Location location;

    public Date timeVisited;
}
