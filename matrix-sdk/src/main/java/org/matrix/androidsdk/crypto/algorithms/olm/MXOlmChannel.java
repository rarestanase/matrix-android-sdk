package org.matrix.androidsdk.crypto.algorithms.olm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.crypto.algorithms.OlmChannel;
import org.matrix.androidsdk.crypto.data.MXDeviceInfo;
import org.matrix.androidsdk.crypto.data.MXOlmSessionResult;
import org.matrix.androidsdk.crypto.data.MXUsersDevicesMap;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.crypto.EncryptedMessage;
import org.matrix.androidsdk.util.JsonUtils;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MXOlmChannel implements OlmChannel {
    private static final String LOG_TAG = MXOlmChannel.class.getSimpleName();

    private final MXSession mSession;
    @Nullable private OlmChannelListener listener = null;

    public MXOlmChannel(MXSession mSession) {
        this.mSession = mSession;
    }

    @Override
    public void sendEventToDevice(
        final String deviceId,
        final String eventId,
        final Map<String, Object> payload
    ) {
        final String userId = mSession.getMyUserId();

        final MXDeviceInfo deviceInfo =
            mSession.getCrypto().mCryptoStore.getUserDevice(deviceId, userId);

        if (null != deviceInfo) {
            Map<String, List<MXDeviceInfo>> devicesByUser = new HashMap<>();
            devicesByUser.put(userId, new ArrayList<>(Arrays.asList(deviceInfo)));

            mSession.getCrypto().ensureOlmSessionsForDevices(
                devicesByUser,
                makeOlmSessionCallback(deviceId, eventId, payload, userId, deviceInfo)
            );
        } else {
            Log.e(
                LOG_TAG,
                "## sendEventToDevice() : ensureOlmSessionsForDevices " + userId
                    + ":" + deviceId + " not found"
            );
        }
    }

    @NonNull
    private ApiCallback<MXUsersDevicesMap<MXOlmSessionResult>> makeOlmSessionCallback(
        final String deviceId,
        final String eventId,
        final Map<String, Object> payload,
        final String userId,
        final MXDeviceInfo deviceInfo
    ) {
        return new ApiCallback<MXUsersDevicesMap<MXOlmSessionResult>>() {
            @Override
            public void onSuccess(MXUsersDevicesMap<MXOlmSessionResult> map) {
                sendPayloadToDevice(map, deviceId, userId, eventId, payload, deviceInfo);
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(
                    LOG_TAG,
                    "## sendEventToDevice() : ensureOlmSessionsForDevices " + userId
                        + ":" + deviceId + " failed "+ e.getMessage(),
                    e
                );
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(
                    LOG_TAG,
                    "## sendEventToDevice() : ensureOlmSessionsForDevices " + userId
                        + ":" + deviceId + " failed "+ e.getMessage()
                );
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(
                    LOG_TAG,
                    "## sendEventToDevice() : ensureOlmSessionsForDevices " + userId
                        + ":" + deviceId + " failed "+ e.getMessage(),
                    e
                );
            }
        };
    }

    private void sendPayloadToDevice(
        MXUsersDevicesMap<MXOlmSessionResult> map,
        String deviceId,
        String userId,
        String eventId,
        Map<String, Object> payload,
        MXDeviceInfo deviceInfo
    ) {
        MXOlmSessionResult olmSessionResult = map.getObject(deviceId, userId);

        if ((null == olmSessionResult) || (null == olmSessionResult.mSessionId)) {
            // no session with this device, probably because there
            // were no one-time keys.
            //
            // ensureOlmSessionsForUsers has already done the logging,
            // so just skip it.
            return;
        }

        Map<String, Object> payloadJson = new HashMap<>();
        payloadJson.put("type", eventId);
        payloadJson.put("content", payload);

        EncryptedMessage encodedPayload =
            mSession.getCrypto().encryptMessage(payloadJson, Arrays.asList(deviceInfo));

        MXUsersDevicesMap<EncryptedMessage> sendToDeviceMap = new MXUsersDevicesMap<>();
        sendToDeviceMap.setObject(encodedPayload, userId, deviceId);

        Log.d(LOG_TAG, "## sendEventToDevice() : sending to " + userId + ":" + deviceId);
        mSession.getCryptoRestClient().sendToDevice(
            Event.EVENT_TYPE_MESSAGE_ENCRYPTED,
            sendToDeviceMap,
            makeSendToDeviceCallback(userId, deviceId)
        );
    }

    @NonNull
    private ApiCallback<Void> makeSendToDeviceCallback(final String userId, final String deviceId) {
        return new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                Log.d(LOG_TAG, "## sendEventToDevice() : sent to " + userId + ":" + deviceId);
            }

            @Override
            public void onNetworkError(Exception e) {
                Log.e(
                    LOG_TAG,
                    "## sendEventToDevice() : sendToDevice " + userId +
                        ":" + deviceId + " failed " + e.getMessage(),
                    e
                );
            }

            @Override
            public void onMatrixError(MatrixError e) {
                Log.e(
                    LOG_TAG,
                    "## sendEventToDevice() : sendToDevice " + userId +
                        ":" + deviceId + " failed " + e.getMessage()
                );
            }

            @Override
            public void onUnexpectedError(Exception e) {
                Log.e(
                    LOG_TAG,
                    "## sendEventToDevice() : sendToDevice " + userId +
                        ":" + deviceId + " failed " + e.getMessage(),
                    e
                );
            }
        };
    }

    @Override
    public void setChannelListener(OlmChannelListener listener) {
        this.listener = listener;
    }

    public void onEventReceived(Event event) {
        boolean isSameUser = mSession.getMyUserId().equals(event.getSenderUserId());
        if (listener != null && isSameUser) {
            Map<String, Object> payload = JsonUtils.toMap(event.getContent());
            listener.onEventReceived(event.getSenderDeviceId(), event.getType(), payload);
        }
    }
}
