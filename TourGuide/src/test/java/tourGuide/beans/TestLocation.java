package tourGuide.beans;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestLocation {

    @Test
    public void equalsContract() {
        EqualsVerifier.simple().forClass(Location.class).verify();

    }
}
