package tourGuide.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javamoney.moneta.Money;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDTO {

    private int attractionProximity;

    private Money lowerPricePoint;

    private Money highPricePoint;

    private int tripDuration = 1;

    private int ticketQuantity = 1;

    private int numberOfAdults = 1;

    private int numberOfChildren = 0;
}
