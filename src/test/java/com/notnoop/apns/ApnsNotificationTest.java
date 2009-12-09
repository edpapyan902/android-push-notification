package com.notnoop.apns;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.experimental.theories.*;
import org.junit.runner.RunWith;

import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.PayloadBuilder;
import static com.notnoop.apns.PayloadBuilder.*;
import static com.notnoop.apns.internal.Utilities.*;

@RunWith(Theories.class)
public class ApnsNotificationTest {

    // Device Tokens
    @DataPoints public static String[] deviceTokens =
    {
        "298893742908AB98C",
        "98234098203BACCCC93284092"
    };

    // Messages
    @DataPoints public static PayloadBuilder[] payloaders =
    {
        newPayload().alertBody("test").sound("default"),
        newPayload().sound("chimes").actionKey("Cancel"),
        newPayload().customField("notice", "this")
    };

    @Theory
    public void lengthConsistency(String deviceToken, PayloadBuilder payload) {
        ApnsNotification msg = new ApnsNotification(deviceToken, payload.build());
        assertEquals(msg.marshall().length, msg.length());
    }

    @Theory
    public void commandIsZero(String deviceToken, PayloadBuilder payload) {
        ApnsNotification msg = new ApnsNotification(deviceToken, payload.build());
        byte[] bytes = msg.marshall();
        assertEquals(0, /*command part*/ bytes[0]);
    }

    @Theory
    public void deviceTokenPart(String deviceToken, PayloadBuilder payload) {
        ApnsNotification msg = new ApnsNotification(deviceToken, payload.build());
        byte[] bytes = msg.marshall();

        byte[] dt = decodeHex(deviceToken);
        assertEquals(dt.length, /* found length */ (bytes[1] << 8) + bytes[2]);

        // verify the device token part
        assertArrayEquals(dt, Arrays.copyOfRange(bytes, 3, 3 + dt.length));
    }

    @Theory
    public void payloadPart(String deviceToken, PayloadBuilder payload) {
        String payloadString = payload.build();
        ApnsNotification msg = new ApnsNotification(deviceToken, payloadString);
        byte[] bytes = msg.marshall();

        byte[] pl = toUTF8Bytes(payloadString);

        // in reverse
        int plBegin = bytes.length - pl.length;

        /// verify the payload part
        assertArrayEquals(pl, Arrays.copyOfRange(bytes, plBegin, bytes.length));
        assertEquals(pl.length, (bytes[plBegin - 2] << 8) + bytes[plBegin - 1]);
    }

    @Theory
    public void allPartsLength(String deviceToken, PayloadBuilder payload) {
        String payloadString = payload.build();
        ApnsNotification msg = new ApnsNotification(deviceToken, payloadString);
        byte[] bytes = msg.marshall();

        int expectedLength = 1
            + 2 + decodeHex(deviceToken).length
            + 2 + toUTF8Bytes(payloadString).length;
        assertEquals(expectedLength, bytes.length);
    }
}
