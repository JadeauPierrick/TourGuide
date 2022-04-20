package tourGuide.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDTO {

    private int attractionProximity;

    private CurrencyUnit currency = Monetary.getCurrency("USD");

    private int lowerPricePoint;

    private int highPricePoint;

    private int tripDuration = 1;

    private int ticketQuantity = 1;

    private int numberOfAdults = 1;

    private int numberOfChildren = 0;
}
