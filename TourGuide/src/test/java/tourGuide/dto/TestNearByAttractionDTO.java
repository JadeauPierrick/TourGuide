package tourGuide.dto;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestNearByAttractionDTO {

    @Test
    public void equalsContract() {
        EqualsVerifier.simple().forClass(NearByAttractionDTO.class).verify();
    }
}
