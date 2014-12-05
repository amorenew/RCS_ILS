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

package com.orangelabs.rcs.ri.sharing.image;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingIntent;
import com.gsma.services.rcs.ish.ImageSharingListener;
import com.gsma.services.rcs.ish.ImageSharingService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive image sharing
 * 
 * @author Jean-Marc AUFFRET
 */
public class ReceiveImageSharing extends Activity implements JoynServiceListener {
    /**
     * UI handler
     */
    private final Handler handler = new Handler();
    
	/**
	 * Sharing ID
	 */
    private String sharingId;
    
    /**
     * Remote Contact
     */
    private String remoteContact;
    
    /**
     * Image size
     */
    private long imageSize;
    
    /**
	 * Image sharing API
	 */
    private ImageSharingService ishApi;
    
	/**
     * Image sharing
     */
    private ImageSharing imageSharing = null;
    
    /**
     * Image sharing listener
     */
    private MyImageSharingListener ishListener = new MyImageSharingListener();    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	// Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_receive);
        
        // Set title
		setTitle(R.string.title_image_sharing);

        // Get invitation info
        sharingId = getIntent().getStringExtra(ImageSharingIntent.EXTRA_SHARING_ID);
		remoteContact = getIntent().getStringExtra(ImageSharingIntent.EXTRA_CONTACT);
		imageSize = getIntent().getLongExtra(ImageSharingIntent.EXTRA_FILESIZE, -1);

        // Instanciate API
		ishApi = new ImageSharingService(getApplicationContext(), this);
		
		// Connect API
		ishApi.connect();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove file transfer listener
        if (imageSharing != null) {
        	try {
        		imageSharing.removeEventListener(ishListener);
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }

        // Disconnect API
        ishApi.disconnect();
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try{
			// Get the image sharing
			imageSharing = ishApi.getImageSharing(sharingId);
			if (imageSharing == null) {
				// Session not found or expired
				Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_session_not_found));
				return;
			}
			imageSharing.addEventListener(ishListener);
			
			String size;
	    	if (imageSize != -1) {
	    		size = getString(R.string.label_file_size, " " + (imageSize/1024), " Kb");
	    	} else {
	    		size = getString(R.string.label_file_size_unknown);
	    	}
			
	    	// Display sharing infos
    		TextView from = (TextView)findViewById(R.id.from);
	        from.setText(getString(R.string.label_from) + " " + remoteContact);
	    	TextView sizeTxt = (TextView)findViewById(R.id.image_size);
	    	sizeTxt.setText(size);
	    	
			// Display accept/reject dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_image_sharing);
			builder.setMessage(getString(R.string.label_from) +	" " + remoteContact + "\n" + size);
			builder.setCancelable(false);
			builder.setIcon(R.drawable.ri_notif_csh_icon);
			builder.setPositiveButton(getString(R.string.label_accept), acceptBtnListener);
			builder.setNegativeButton(getString(R.string.label_decline), declineBtnListener);
			builder.show();
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_failed));
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
		Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_api_disabled));
    }    
    
	/**
	 * Accept invitation
	 */
	private void acceptInvitation() {
    	try {
    		// Accept the invitation
    		imageSharing.acceptInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
    		Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_invitation_failed));
    	}
	}
	
	/**
	 * Reject invitation
	 */
	private void rejectInvitation() {
    	try {
    		// Reject the invitation
    		imageSharing.removeEventListener(ishListener);
    		imageSharing.rejectInvitation();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}	
	
	/**
     * Accept button listener
     */
    private OnClickListener acceptBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {     
        	// Accept invitation
        	acceptInvitation();
        }
    };

    /**
     * Reject button listener
     */    
    private OnClickListener declineBtnListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        	// Reject invitation
        	rejectInvitation();
        	
            // Exit activity
			finish();
        }
    };    
    
    /**
     * Image sharing event listener
     */
    private class MyImageSharingListener extends ImageSharingListener {
    	// Sharing started
    	public void onSharingStarted() {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("started");
				}
			});
    	}
    	
    	// Sharing aborted
    	public void onSharingAborted() {
			handler.post(new Runnable() { 
				public void run() {
					Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_sharing_aborted));
				}
			});
    	}

    	// Sharing error
    	public void onSharingError(final int error) {
			handler.post(new Runnable() { 
				public void run() {
					Utils.showMessageAndExit(ReceiveImageSharing.this, getString(R.string.label_transfer_failed, error));
				}
			});
    	}
    	
    	// Sharing progress
    	public void onSharingProgress(final long currentSize, final long totalSize) {
			handler.post(new Runnable() { 
    			public void run() {
    				updateProgressBar(currentSize, totalSize);
    			}
    		});
    	}

    	// Image shared
    	public void onImageShared(final String filename) {
			handler.post(new Runnable() { 
				public void run() {
					TextView statusView = (TextView)findViewById(R.id.progress_status);
					statusView.setText("transferred");
					
					// Make sure progress bar is at the end
			        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
			        progressBar.setProgress(progressBar.getMax());
					
			        // Show the shared image
			        Utils.showPictureAndExit(ReceiveImageSharing.this, filename);			        
				}
			});
    	}
    };

    /**
     * Show the transfer progress
     * 
     * @param currentSize Current size transferred
     * @param totalSize Total size to be transferred
     */
    private void updateProgressBar(long currentSize, long totalSize) {
    	TextView statusView = (TextView)findViewById(R.id.progress_status);
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress_bar);
    	
		String value = "" + (currentSize/1024);
		if (totalSize != 0) {
			value += "/" + (totalSize/1024);
		}
		value += " Kb";
		statusView.setText(value);
	    
	    if (currentSize != 0) {
	    	double position = ((double)currentSize / (double)totalSize)*100.0;
	    	progressBar.setProgress((int)position);
	    } else {
	    	progressBar.setProgress(0);
	    }
    }

    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
    	try {
            if (imageSharing != null) {
            	imageSharing.removeEventListener(ishListener);
        		imageSharing.abortSharing();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	imageSharing = null;
		
	    // Exit activity
		finish();
    }    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            	// Quit the session
            	quitSession();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_image_sharing, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_close_session:
				// Quit the session
				quitSession();
				break;
		}
		return true;
	} 
}
