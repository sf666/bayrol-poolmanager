package de.sf666.bayrol.api;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import de.sf666.bayrol.domain.BayrolMainDisplayValues;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OutputRequestTest {
    @Test void repeatedLiveReadsResetThePortalDeltaBaselineWithoutWritingRegisters() throws Exception {
        BayrolHttpConnector connector = new BayrolHttpConnector("unused", "unused");
        Field sessions = BayrolHttpConnector.class.getDeclaredField("plantIdSidMap");
        sessions.setAccessible(true);
        ((Map<String,String>)sessions.get(connector)).put("12799", "testsession");
        List<String> requests = new ArrayList<>();
        boolean[] reset = {false};
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request request = chain.request();
            String response;
            if (request.method().equals("GET")) {
                assertEquals("3.16912.0", request.url().queryParameter("cmd"));
                reset[0] = true;
                requests.add("home");
                response = "<html>home</html>";
            } else {
                assertTrue(reset[0], "Each live read must reset the delta baseline");
                reset[0] = false;
                Buffer buffer = new Buffer(); request.body().writeTo(buffer);
                String payload = buffer.readUtf8();
                assertFalse(payload.contains("\"set\""), "No register writes allowed");
                requests.add("read");
                response = "{\"data\":{\"34.4001.value\":\"7.25\",\"34.4008.value\":\"0.08\",\"34.4033.value\":\"12.7\",\"55.17101.status\":\"0\",\"55.17106.status\":\"1\"},\"status\":{\"code\":0}}";
            }
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(ResponseBody.create(response, MediaType.parse("application/json"))).build();
        }).build();
        Field field = BayrolHttpConnector.class.getDeclaredField("okClient");
        field.setAccessible(true); field.set(connector,client);
        for (int i=0; i<3; i++) {
            assertEquals("1", connector.readFilterPumpState("12799"));
            assertEquals("0", connector.readLightState("12799"));
            BayrolMainDisplayValues state = connector.updateAndGetState("12799");
            assertEquals(7.25,state.ph); assertEquals(0.08,state.cl);
            assertEquals("0",state.lightState); assertEquals("1",state.filterPumpState);
        }
        assertEquals(18,requests.size());
    }
}
