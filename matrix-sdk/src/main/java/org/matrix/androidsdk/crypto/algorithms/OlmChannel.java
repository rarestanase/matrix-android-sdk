package org.matrix.androidsdk.crypto.algorithms;

import java.util.Map;

public interface OlmChannel {

    void sendEventToDevice(String deviceId, String eventId, Map<String, Object> payload);

    void setChannelListener(OlmChannelListener listener);

    interface OlmChannelListener {
        void onEventReceived(
            String senderDeviceId,
            String eventId,
            Map<String, Object> payload
        );
    }
}
