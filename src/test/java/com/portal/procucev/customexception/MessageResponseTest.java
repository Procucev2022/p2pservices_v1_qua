package com.portal.procucev.customexception;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageResponseTest {

    @Test
    void testMessageResponseConstructorsAndGettersSetters() {
        MessageResponse resp1 = new MessageResponse("200", "msg1", Collections.singletonList("err1"), "SUCCESS");
        assertEquals("200", resp1.getStatusCode());
        assertEquals("msg1", resp1.getMessage());
        assertEquals(1, resp1.getErrorMsg().size());
        assertEquals("SUCCESS", resp1.getStatus());

        Date now = new Date();
        MessageResponse resp2 = new MessageResponse("200", "msg2", now, "OK", "TYPE1");
        assertEquals(now, resp2.getTimestamp());
        assertEquals("TYPE1", resp2.getType());

        MessageResponse resp3 = new MessageResponse("msg3", "STATUS3");
        assertEquals("msg3", resp3.getMessage());
        assertEquals("STATUS3", resp3.getStatus());

        MessageResponse resp4 = new MessageResponse("200", "msg4", Collections.singletonList("err4"), now, "STATUS4", "TYPE4");
        assertEquals("TYPE4", resp4.getType());

        Map<String, Object> data = new HashMap<>();
        data.put("key", "val");
        MessageResponse resp5 = new MessageResponse("200", "msg5", data, "STATUS5", now);
        assertEquals(data, resp5.getData());

        resp1.setType("NEW_TYPE");
        assertEquals("NEW_TYPE", resp1.getType());
        resp1.setStatus("NEW_STATUS");
        assertEquals("NEW_STATUS", resp1.getStatus());
        resp1.setData(data);
        assertEquals(data, resp1.getData());
        resp1.setStatusCode("500");
        assertEquals("500", resp1.getStatusCode());
        resp1.setMessage("NEW_MSG");
        assertEquals("NEW_MSG", resp1.getMessage());
        resp1.setErrorMsg(Collections.emptyList());
        assertTrue(resp1.getErrorMsg().isEmpty());
        resp1.setTimestamp(now);
        assertEquals(now, resp1.getTimestamp());

        MessageResponse success = MessageResponse.success("SuccessMsg", data);
        assertEquals("200", success.getStatusCode());
        assertEquals("SuccessMsg", success.getMessage());
        assertEquals(data, success.getData());

        MessageResponse error = MessageResponse.error("ErrorMsg", Collections.singletonList("e1"));
        assertEquals("400", error.getStatusCode());
        assertEquals("ErrorMsg", error.getMessage());
        assertEquals(1, error.getErrorMsg().size());
    }
}
