/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.messaging.chat;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatListener;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Restart a group chat session
 */
public class RestartChat {
    /**
     * UI handler
     */
    private Handler handler = new Handler();

    /**
     * Progress dialog
     */
    private Dialog progressDialog = null;

	/**
     * Activity
     */
    private Activity activity;
    
    /**
	 * Chat API
	 */
    private ChatService chatApi;
    
    /**
	 * Chat ID to restart
	 */
	private String chatId;

	/**
	 * Restarted chat session
	 */
	private GroupChat groupChat = null; 

    /**
     * Group chat listener
     */
    private MyGroupChatListener chatListener = new MyGroupChatListener();	
	
	/**
     * Constructor
     * 
     * @param context Context
     * @param chatApi Chat API
     * @param chatId Chat ID
     */
	public RestartChat(Activity activity, ChatService chatApi, String chatId) {
		this.activity = activity;
		this.chatApi = chatApi;
		this.chatId = chatId;
	}
    
    /**
     * Start restart session
     */
    public synchronized void start() {
    	if (groupChat != null) {
    		return;
    	}
    	
    	// Initiate the session in background
    	try {
    		groupChat = chatApi.restartGroupChat(chatId);
    		groupChat.addEventListener(chatListener);
    	} catch(Exception e) {
    		e.printStackTrace();
    		
			// Hide progress dialog
			hideProgressDialog();

			// Show error
			Utils.showMessage(activity, activity.getString(R.string.label_restart_chat_exception));		
    	}
        
        // Display a progress dialog
        progressDialog = Utils.showProgressDialog(activity, activity.getString(R.string.label_command_in_progress));
        progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				// Stop session
				stop();

				// Hide progress dialog
				hideProgressDialog();

				// Display feedback info
				Toast.makeText(activity,activity.getString(R.string.label_chat_restart_canceled), Toast.LENGTH_SHORT).show();
			}
		});
    }    

    /**
     * Stop restart session
     */
    public synchronized void stop() {
    	if (groupChat == null) {
    		return;
    	}

    	// Stop session
		try {
			groupChat.removeEventListener(chatListener);
			groupChat.quitConversation();
		} catch(Exception e) {
			e.printStackTrace();
		}
		groupChat = null;
    }    
    
    /**
     * Group chat event listener
     */
    private class MyGroupChatListener extends GroupChatListener {
    	// Session started
    	public void onSessionStarted() {
			handler.post(new Runnable() { 
				public void run() {
					try {
	                    // Hide progress dialog
						hideProgressDialog();
	
						// Remove listener now
                		groupChat.removeEventListener(chatListener);

						// Display chat view
                		Intent intent = new Intent(activity, GroupChatView.class);
			        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		            	intent.putExtra(GroupChatView.EXTRA_MODE, GroupChatView.MODE_OPEN);
			    		intent.putExtra(GroupChatView.EXTRA_CHAT_ID, groupChat.getChatId());
			    		activity.startActivity(intent);
				    } catch(JoynServiceNotAvailableException e) {
				    	e.printStackTrace();
						Utils.showMessageAndExit(activity, activity.getString(R.string.label_api_disabled));
				    } catch(JoynServiceException e) {
				    	e.printStackTrace();
						Utils.showMessageAndExit(activity, activity.getString(R.string.label_api_failed));
					}
				}
			});
    	}
    	
    	// Session aborted
    	public void onSessionAborted() {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
					
					// Session aborted
					Utils.showMessageAndExit(activity, activity.getString(R.string.label_chat_aborted));
				}
			});
    	}        	
    	
    	// Session error
    	public void onSessionError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					// Hide progress dialog
					hideProgressDialog();

					// Display error
					if (error == GroupChat.Error.INVITATION_DECLINED) {
						Utils.showMessageAndExit(activity, activity.getString(R.string.label_chat_declined));
					} else {
						Utils.showMessage(activity, activity.getString(R.string.label_restart_chat_failed, error));
					}					
				}
			});
    	}
    	
    	// New message has been received
    	public void onNewMessage(ChatMessage message) {
    		// Not used here
    	}
    	
    	// New geoloc has been received
    	public void onNewGeoloc(GeolocMessage message) {
    		// Not used here
    	}
    	
    	// A message has been delivered to the remote
    	public void onReportMessageDelivered(String msgId) {
    		// Not used here
    	}

    	// A message has been displayed by the remote
    	public void onReportMessageDisplayed(String msgId) {
    		// Not used here
    	}

    	// A message has failed to be delivered to the remote
    	public void onReportMessageFailed(String msgId) {
    		// Not used here
    	}
    	
    	// Is-composing event has been received
    	public void onComposingEvent(String contact, boolean status) {
    		// Not used here
    	}

    	// A new participant has joined the group chat
    	public void onParticipantJoined(String contact, String contactDisplayname) {
    		// Not used here
    	}
    	
    	// A participant has left voluntary the group chat
    	public void onParticipantLeft(String contact) {
    		// Not used here
    	}

    	// A participant is disconnected from the group chat
    	public void onParticipantDisconnected(String contact) {
    		// Not used here
    	}    	
    };

	/**
	 * Hide progress dialog
	 */
    public void hideProgressDialog() {
    	if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			progressDialog = null;
		}
    }
}
