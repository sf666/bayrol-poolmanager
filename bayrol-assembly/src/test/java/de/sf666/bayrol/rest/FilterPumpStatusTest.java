package de.sf666.bayrol.rest;

import de.sf666.bayrol.bridge.BayrolBridge;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilterPumpStatusTest {
    @Test
    void unknownResponseIsUnavailableRatherThanOff() {
        BayrolBridge bridge = mock(BayrolBridge.class);
        RestBayrolBridge rest = new RestBayrolBridge();
        ReflectionTestUtils.setField(rest, "bayrolBridge", bridge);
        when(bridge.readFilterPumpState("12799")).thenReturn("0", "1", "", "11");
        assertEquals("1", rest.filterPumpStatus("12799"));
        assertEquals("0", rest.filterPumpStatus("12799"));
        for (int i = 0; i < 2; i++) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> rest.filterPumpStatus("12799"));
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        }
        verify(bridge, never()).lightOn(anyString());
        verify(bridge, never()).lightOff(anyString());
    }
}
