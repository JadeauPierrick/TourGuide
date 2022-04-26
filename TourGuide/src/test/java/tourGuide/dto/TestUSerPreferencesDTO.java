package tourGuide.dto;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestUSerPreferencesDTO {

    @Test
    public void equalsContract() {
        EqualsVerifier.simple().forClass(UserPreferencesDTO.class).verify();
    }
}
