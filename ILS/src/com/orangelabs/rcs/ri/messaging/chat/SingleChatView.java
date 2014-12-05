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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;

import com.orangelabs.rcs.ri.utils.LogUtils;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.chat.Chat;
import com.gsma.services.rcs.chat.ChatIntent;
import com.gsma.services.rcs.chat.ChatListener;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Single chat view
 */
public class SingleChatView extends ChatView {
	/**
	 * View modes
	 */
	public final static int MODE_INCOMING = 0;
	public final static int MODE_OUTGOING = 1;
	public final static int MODE_OPEN = 2;

	/**
	 * Intent parameters
	 */
	public final static String EXTRA_MODE = "mode";
	public final static String EXTRA_CONTACT = "contact";

	/**
	 * Activity displayed status
	 */
	private static boolean activityDisplayed = false;
	
	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(SingleChatView.class.getSimpleName());
	
	/**
	 * Remote contact
	 */
	private String contact = null;
    
    /**
     * Chat 
     */
	private Chat chat = null;

    /**
     * Chat listener
     */
    private MyChatListener chatListener = new MyChatListener();	
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int mode = getIntent().getIntExtra(SingleChatView.EXTRA_MODE, -1);
		if ((mode == SingleChatView.MODE_OPEN) || (mode == SingleChatView.MODE_OUTGOING)) {
			// Open chat
			contact = getIntent().getStringExtra(SingleChatView.EXTRA_CONTACT);				
		} else {
			// Incoming chat from its Intent
			contact = getIntent().getStringExtra(ChatIntent.EXTRA_CONTACT);
		}
		
		// Set title
		setTitle(getString(R.string.title_chat) + " " +	contact);	
		
		// Load history
		loadHistory();
		
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate contact=" + contact);
		}
    }
    
    @Override
	protected void onResume() {
        super.onResume();
        
        activityDisplayed = true;
    }

    @Override
	protected void onPause() {
        super.onStart();
        
        activityDisplayed = false;
    }

    /**
     * Return true if the activity is currently displayed or not
     *   
     * @return Boolean
     */
    public static boolean isDisplayed() {
    	return activityDisplayed;
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	try {
    		// Set chat settings
            isDeliveryDisplayed = chatApi.getConfiguration().isDisplayedDeliveryReport();

			// Set max label length
			int maxMsgLength = chatApi.getConfiguration().getSingleChatMessageMaxLength();
			if (maxMsgLength > 0) {
		        // Set the message composer max length
				InputFilter[] filterArray = new InputFilter[1];
				filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
				composeText.setFilters(filterArray);
			}
			
			// Open chat
    		chat = chatApi.openSingleChat(contact, chatListener);
							
			// Instanciate the composing manager
			composingManager = new IsComposingManager(chatApi.getConfiguration().getIsComposingTimeout() * 1000);
			
			// Update displayed report
			updateDisplayedReport();
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_failed));
		}
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_disabled));
    }    
    
    /**
     * Load history
     */
    private void loadHistory() {
		try {
			Uri uri = Uri.withAppendedPath(ChatLog.Message.CONTENT_CHAT_URI, contact);		
	    	Cursor cursor = getContentResolver().query(uri, 
	    			new String[] {
	    				ChatLog.Message.DIRECTION,
	    				ChatLog.Message.CONTACT_NUMBER,
	    				ChatLog.Message.BODY,
	    				ChatLog.Message.MIME_TYPE,
	    				ChatLog.Message.MESSAGE_STATUS,
	    				ChatLog.Message.MESSAGE_TYPE,
	    				ChatLog.Message.MESSAGE_ID
	    				},
    				null, 
	    			null, 
	    			ChatLog.Message.TIMESTAMP + " ASC");
	    	while(cursor.moveToNext()) {
	    		int direction = cursor.getInt(0);
	    		String contact = cursor.getString(1);
	    		byte[] content = cursor.getBlob(2);
	    		String contentType = cursor.getString(3);
	    		int status = cursor.getInt(4);
	    		int type = cursor.getInt(5);
	    		String msgId = cursor.getString(6);

	    		// Add only messages to the history
	    		if (type != ChatLog.Message.Type.SYSTEM) {
	        		addMessageHistory(direction, contact, content, contentType);
	    		}	    			
	    		
	    		// Send displayed report for older messages
		        if (isDeliveryDisplayed && (status == ChatLog.Message.Status.Content.UNREAD_REPORT)) {
		        	sendDisplayedReport(msgId);
		        }	    		
	    	}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}

    /**
     * Send a text message
     * 
     * @param msg Message
     * @return Message ID
     */
    protected String sendTextMessage(String msg) {
    	try {
			// Send the text to remote
			String msgId = chat.sendMessage(msg);
			
	        // Warn the composing manager that the message was sent
			composingManager.messageWasSent();

			return msgId;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
    }
    
    /**
     * Send geoloc message
     * 
     * @param geoloc Geoloc
     * @return Message ID
     */
    protected String sendGeolocMessage(Geoloc geoloc) {
        try {
			// Send the text to remote
        	String msgId = chat.sendGeoloc(geoloc);
	    	
	        // Warn the composing manager that the message was sent
	    	composingManager.messageWasSent();

	    	return msgId;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return null;
	    }
    }

    /**
     * Update displayed report
     */
    private void updateDisplayedReport() {
    	Thread t = new Thread() {
    		public void run() {
				try {
			    	Cursor cursor = getContentResolver().query(ChatLog.Message.CONTENT_URI, 
			    			new String[] {
			    				ChatLog.Message.MESSAGE_STATUS,
			    				ChatLog.Message.MESSAGE_ID
			    				},
			    			ChatLog.Message.CHAT_ID + "='" + contact + "'", 
			    			null, 
			    			ChatLog.Message.TIMESTAMP + " ASC");
			    	while(cursor.moveToNext()) {
			    		int status = cursor.getInt(0);
			    		String msgId = cursor.getString(1);
		
			    		// Send displayed report for older messages
				        if (isDeliveryDisplayed && (status == ChatLog.Message.Status.Content.UNREAD_REPORT)) {
				        	sendDisplayedReport(msgId);
				        }	    		
			    	}
		    	} catch(Exception e) {
		    		e.printStackTrace();
		    	}
    		}
    	};
    	t.start();
    }
    
    /**
     * Quit the session
     */
    protected void quitSession() {
		// Remove listener
    	try {
            if (chat != null) {
        		chat.removeEventListener(chatListener);
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	chat = null;
        
        // Exit activity
		finish();        
    }        	
        
    /**
     * Update the is composing status
     * 
     * @param isTyping Is compoing status
     */
    protected void setTypingStatus(boolean isTyping) {
		try {
			if (chat != null) {
				chat.sendIsComposingEvent(isTyping);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}    
    
    /**
     * Send a displayed report
     * 
     * @param msgId Message ID
     */
    private void sendDisplayedReport(String msgId) {
        try {
			if (chat != null) {
				chat.sendDisplayedDeliveryReport(msgId);
			}
        } catch(Exception e) {
			e.printStackTrace();
        }
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_chat, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_insert_smiley:
				Smileys.showSmileyDialog(
						this, 
						composeText, 
						getResources(), 
						getString(R.string.menu_insert_smiley));
				break;
	
			case R.id.menu_quicktext:
				addQuickText();
				break;

			case R.id.menu_send_geoloc:
				getGeoLoc();
				break;	
							
			case R.id.menu_showus_map:
				try {
					showUsInMap(chat.getRemoteContact());
			    } catch(JoynServiceException e) {
			    	e.printStackTrace();
					Utils.showMessageAndExit(SingleChatView.this, getString(R.string.label_api_failed));
				}
				break;	

			case R.id.menu_clear_log:
				// Delete conversation
				String where = ChatLog.Message.CHAT_ID + " = '" + contact + "'"; 
				getContentResolver().delete(ChatLog.Message.CONTENT_URI, where, null);
				
				// Refresh view
		        msgListAdapter = new MessageListAdapter(this);
		        setListAdapter(msgListAdapter);
				break;
		}
		return true;
	}
        
    /**
     * Chat event listener
     */
    private class MyChatListener extends ChatListener {
    	// Callback called when a new message has been received
    	public void onNewMessage(final ChatMessage message) {
			handler.post(new Runnable() { 
				public void run() {
					// Send a displayed delivery report
			        if (isDeliveryDisplayed && message.isDisplayedReportRequested()) {
			        	sendDisplayedReport(message.getId());
			        }

			        // Display the received message
					displayReceivedMessage(message);
				}
			});
    	}

    	// Callback called when a new geoloc has been received
    	public void onNewGeoloc(final GeolocMessage message) {
			handler.post(new Runnable() { 
				public void run() {
					// Send a displayed delivery report
			        if (isDeliveryDisplayed && message.isDisplayedReportRequested()) {
			        	sendDisplayedReport(message.getId());
			        }

			        // Display the received geoloc
			        displayReceivedGeoloc(message);
				}
			});
    	}

    	// Callback called when a message has been delivered to the remote
    	public void onReportMessageDelivered(String msgId) {
			handler.post(new Runnable(){
				public void run(){
					// Display a notification
					addNotifHistory(getString(R.string.label_receive_delivery_status_delivered));
				}
			});
    	}

    	// Callback called when a message has been displayed by the remote
    	public void onReportMessageDisplayed(String msgId) {
			handler.post(new Runnable(){
				public void run(){
					// Display a notification
					addNotifHistory(getString(R.string.label_receive_delivery_status_displayed));
				}
			});
    	}

    	// Callback called when a message has failed to be delivered to the remote
    	public void onReportMessageFailed(String msgId) {
			if (LogUtils.isActive) {
				Log.w(LOGTAG, "onReportMessageFailed msgId=" + msgId);
			}
			handler.post(new Runnable(){
				public void run(){
					// Display a notification
					addNotifHistory(getString(R.string.label_receive_delivery_status_failed));
				}
			});
    	}

    	// Callback called when an Is-composing event has been received
    	public void onComposingEvent(final boolean status) {
			handler.post(new Runnable() {
				public void run(){
					TextView view = (TextView)findViewById(R.id.isComposingText);
					if (status) {
						// Display is-composing notification
						view.setText(contact + " " + getString(R.string.label_contact_is_composing));
						view.setVisibility(View.VISIBLE);
					} else {
						// Hide is-composing notification
						view.setVisibility(View.GONE);
					}
				}
			});
    	}
    }
}
