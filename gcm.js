// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var GCM_STORAGE_PREFIX = 'gcm-';
var GCM_SIDKEY = GCM_STORAGE_PREFIX+'SenderID';
var GCM_REGKEY_PREFIX = GCM_STORAGE_PREFIX+'RegID';

var Event = require('org.chromium.common.events');
var exec = require('cordova/exec');
var channel = require('cordova/channel');
var _senderId;
var _registrationId;

exports.MAX_MESSAGE_SIZE = 4096;

exports.send = function(message, callback){

}

// need to store the sender ids in localstorage and
exports.register = function(senderid, callback) {
  var outstandingCallbacks = 2;
  var result = {};
  setSenderID(senderid);

  checkToIssueCallback = function() {
    outstandingCallbacks--;
    if (outstandingCallbacks == 0) {
      callback(result);
    }
  }
  
  chrome.identity.getAuthToken({interactive:true}, function(token) {
    var channelApiUrl = 'https://www.googleapis.com/gcm_for_chrome/v1/channels/id';
    var req = new XMLHttpRequest();
    req.onreadystatechange = function() {
      if (req.readyState == 4) {
        if (req.status == 200) {
          var response = JSON.parse(req.responseText);
          result['channelId'] = response.id + '/' + chrome.runtime.id;
        } else {
          console.error('Error sending channel ID request, server returned with status ' + req.status);
        }
        checkToIssueCallback();
      }
    }
    req.open('GET', channelApiUrl + '?access_token=' + token, true);
    req.send(null);
  });

  var win = function(registrationId) {
    if(require('cordova/platform').id == "android"){
      result['registrationId'] = registrationId;
      setRegistrationID(registrationid, senderid);
    }
    checkToIssueCallback();
  }
  checkRegistrationID(function(regid) {
    if(!regid) {
       exec(win, checkToIssueCallback, 'ChromeGcm', 'getRegistrationId', [ senderid ]);
    } else {
      result['registrationId'] = regid;
      checkToIssueCallback();
    }
}


exports.onMessage = new Event('onMessage');
exports.onMessagesDeleted = new Event('onMessagesDeleted');
exports.onSendError = new Event('onSendError');

function setSenderID(senderid) {
  var sidObject ={};
  sidObject[GCM_SIDKEY]=senderid;
  chrome.storage.internal.set(sidObject);
  _senderId = senderid;
}

function getSenderID(callback) {
   if(!_senderId) {
      chrome.storage.internal.get(GCM_SIDKEY, function(items){
         if(items[GCM_SIDKEY]) {
            _senderId=items[GCM_SIDKEY];
            callback(_senderId);
         } else {
            callback(null);
         }
      });
   }
   callback(_senderId);
}

function computeRegidKey(regid, senderid) {
   return GCM_REGKEY_PREFIX+senderid;
}

function setRegistrationID(regid,senderid) {
  var regidObject ={};
  regidObject[computeRegidKey(regid,senderid)]=regid;
  chrome.storage.internal.set(regidObject);
  _registrationId = regid;
}
function getRegistrationID(senderid, callback) {
   if(!_registrationId) {
      var regKey = computeRegidKey(senderid);
      chrome.storage.internal.get(regKey,function(items){
         if(items[regKey]) {
            _registrationId=items[regKey];
            callback(_registrationId);
         } else {
            callback(null);
         }
      });
   }
   callback(_registrationId);
}
function checkRegistrationID(callback) {
    getSenderID(function(sid){
       getRegistrationID(sid, function(regid){
          callback(regid);
       });
    });
}

function fireQueuedMessages() {
    exec(undefined, undefined, 'ChromeGcm', 'fireQueuedMessages', []);
}

require('org.chromium.common.helpers').runAtStartUp(fireQueuedMessages);
