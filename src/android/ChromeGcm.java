// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ChromeGcm extends CordovaPlugin {
    private static final String LOG_TAG = "ChromeGcm";
    private static final String PAYLOAD_LABEL = "payload";

    private static CordovaWebView webView;
    private static boolean safeToFireMessages = false;
    private static List<String> pendingMessages = new ArrayList<String>();
    private static List<String> pendingDeleteMessages = new ArrayList<String>();
    private static List<String> pendingSendErrors = new ArrayList<String>();
    private ExecutorService executorService;

    AtomicInteger msgId = new AtomicInteger();
    GoogleCloudMessaging gcm;
    
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        safeToFireMessages = false;
        super.initialize(cordova, webView);
        ChromeGcm.webView = webView;
        executorService = cordova.getThreadPool();
        if (cordova.getActivity().getIntent().hasExtra(PAYLOAD_LABEL)) {
            cordova.getActivity().moveTaskToBack(true);
        }
    }

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if ("fireQueuedMessages".equals(action)) {
            fireQueuedMessages(args, callbackContext);
            return true;
        } else if ("register".equals(action)) {
            getRegistrationId(args, callbackContext);
            return true;
        } else if ("send".equals(action)) {
            sendMessage(args, callbackContext);
            return true;
        }
        return false;
    }
    static private void startApp(){
        Context context = Context.getApplicationContext();
        try {
                String activityClass = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES).activities[0].name;
                Intent activityIntent = Intent.makeMainActivity(new ComponentName(context, activityClass));
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activityIntent.putExtra(PAYLOAD_LABEL, "dummy");
//                activityIntent.putExtra(PAYLOAD_LABEL, payloadString);
                context.startActivity(activityIntent);
        } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to make startActivity intent: " + e);
        }
    }

    static public void handleSendError(String payloadString) {
        if (webView == null) {
            startApp();
        } else if (!safeToFireMessages) {
            pendingSendErrors.add(payloadString);
        } else {
            fireOnSendError(payloadString);
        }
    }

    static public void handleDeletedMessages(String payloadString) {
        if (webView == null) {
            startApp();
        } else if (!safeToFireMessages) {
            pendingDeleteMessages.add(payloadString);
        } else {
            fireOnMessagesDeleted(payloadString);
        }
    }

    static public void handleRxMessage(String payloadString) {
        if (webView == null) {
            startApp();
        } else if (!safeToFireMessages) {
            pendingMessages.add(payloadString);
        } else {
            fireOnMessage(payloadString);
        }
    }
    
    static private void fireOnMessage(String payload) {
        webView.sendJavascript("chrome.gcm.onMessage.fire({payload:'" + payload + "'})");
    }

    static private void fireOnMessagesDeleted() {
        webView.sendJavascript("chrome.gcm.onMessagesDeleted.fire()");
    }

    static private void fireOnSendError(String msg, String msgid, String msgdetails) {
        webView.sendJavascript("chrome.gcm.onSendError.fire({errorMessage:'"+msg+"', messageId:'" + msgid+"', details:'"+msgdetails+ "'})");
    }

    private void fireQueuedMessages(final CordovaArgs args, final CallbackContext callbackContext) {
        safeToFireMessages = true;
        for (int i = 0; i < pendingMessages.size(); i++) {
            fireOnMessage(pendingMessages.get(i));
        }
        pendingDeleteMessages.clear();
        for (int i = 0; i < pendingDeleteMessages.size(); i++) {
            fireOnMessagesDeleted(pendingDeleteMessages.get(i));
        }
        pendingDeleteMessages.clear();
        for (int i = 0; i < pendingSendErrors.size(); i++) {
            fireOnSendError(pendingSendErrors.get(i));
        }
        pendingSendErrors.clear();
    }

    private void sendMessage(final CordovaArgs args, final CallbackContext callbackContext) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle data = new Bundle();
                    data.putString("my_message", "Hello World");
                    data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
                    String id = Integer.toString(msgId.incrementAndGet());
                    gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                    callbackContext.success(id);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error sending message", e);
                    callbackContext.error("Error sending message");
                }
            }
        });
    }
   
    private void getRegistrationId(final CordovaArgs args, final CallbackContext callbackContext) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if(gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(cordova.getActivity());
                    }
                    String regid = gcm.register(args.getString(0));
                    callbackContext.success(regid);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not get registration ID", e);
                    callbackContext.error("Could not get registration ID");
                }
            }
        });
    }
}
