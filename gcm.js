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
  var win = function(msg) {
    callback(msg);
  }
  var fail = function(msg) {
    console.log('Send failed: '+msg);
    callback(msg);
  }
  getSenderID(function(sid) {
    console.log('sending to '+sid);
    exec(win, fail, 'ChromeGcm', 'send',  sid  );
  });
}

exports.clearCache = function(callback) {
  getSenderID(function(sid) {
    console.log('Clearing registration for '+sid);
    setRegistrationID('',sid);
    callback();
  });
}

// Senderid and registration should be cached in localstorage. 
// If its not there, then registration is required
exports.register = function(senderid, callback) {
  setSenderID(senderid);

  var win = function(registrationId) {
    setRegistrationID(registrationId, senderid);
    callback(registrationId);
  }
  var fail = function(msg) {
    console.log('Registration failed: '+msg);
    callback(null);
  }
  console.log('starting registration check');
  checkRegistrationID(function(regid) {
    if(!regid) {
       console.log('Registering');
       exec(win, fail, 'ChromeGcm', 'getRegistrationId',  senderid );
    } else {
      console.log('Using cached  registrationId:' +regid);
      callback(regid);
    }
  });
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
   } else {
     callback(_senderId);
   }
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
   } else {
     callback(_registrationId);
   }
}
function checkRegistrationID(callback) {
    getSenderID(function(sid){
       getRegistrationID(sid, function(regid){
          callback(regid);
       });
    });
}

function fireQueuedMessages() {
   console.log('firing queued messages');
   exec(undefined, undefined, 'ChromeGcm', 'fireQueuedMessages', []);
}

require('org.chromium.common.helpers').runAtStartUp(fireQueuedMessages);
