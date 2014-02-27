# Chrome Google Cloud Messaging V2 Plugin

This plugin allows Android apps to send/receive push messages.

## Status

Supported on Android

## Reference

The API reference is [here](http://developer.chrome.com/extensions/gcm).

## Notes

* Currently only a single registration is permitted per application, and a registration can only be for a single sender id
* On Android, chrome.identity must be modified so that `getAuthToken` uses the javascript flow (getAuthTokenJS) instead of the native exec flow.  Otherwise `getChannelId` will fail to obtain a `channelId` (but will still obtain a `registrationId`).
* You must install an appropriate auth2 section in your manifest.json with suitable client_id and scopes. Push Messaging requires the scopes: 
https://www.googleapis.com/auth/gcm_for_chrome
and
https://www.googleapis.com/auth/gcm_for_chrome.readonly
