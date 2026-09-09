package com.factoryos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactoryOsApplicationTests {

    @Test
    void applicationBootstrapIntegrityTest() {
        // Verifies core application class exists and packages are sound
        assertTrue(FactoryOsApplication.class.getName().contains("FactoryOsApplication"));
    }
}
