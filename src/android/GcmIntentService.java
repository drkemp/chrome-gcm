// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GcmIntentService extends IntentService {

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        JSONObject payload = new JSONObject();
        try {
            for (String key : intent.getExtras().keySet()) {
                payload.put(key, intent.getStringExtra(key));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error parsing GCM payload: " + e);
            return;
        }
        String payloadString = payload.toString();

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            ChromeGcm.handleSendError( payload);
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            ChromeGcm.handleDeletedMessages( payload);
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            ChromeGcm.handleRxMessage( payload);
        }
        GcmReceiver.completeWakefulIntent(intent);
    }
}
