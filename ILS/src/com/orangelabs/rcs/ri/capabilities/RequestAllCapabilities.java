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
package com.orangelabs.rcs.ri.capabilities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Request capabilities of all contacts
 * 
 * @author Jean-Marc AUFFRET
 */
public class RequestAllCapabilities extends Activity implements JoynServiceListener {
    /**
	 * Capability API
	 */
    private CapabilityService capabilityApi;
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_refresh);
        
        // Set title
        setTitle(R.string.menu_refresh_capabilities);
        
		// Set buttons callback
        Button refreshBtn = (Button)findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(btnSyncListener);        
        refreshBtn.setEnabled(false);
        
        // Instanciate API
        capabilityApi = new CapabilityService(getApplicationContext(), this);
                
        // Connect API
        capabilityApi.connect();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
        // Disconnect API
    	capabilityApi.disconnect();
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        Button refreshBtn = (Button)findViewById(R.id.refresh_btn);
        refreshBtn.setEnabled(true);
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(RequestAllCapabilities.this, getString(R.string.label_api_disabled));
	}    

    /**
     * Publish button listener
     */
    private OnClickListener btnSyncListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
        	boolean registered = false;
        	try {
        		if ((capabilityApi != null) && capabilityApi.isServiceRegistered()) {
        			registered = true;
        		}
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
            if (!registered) {
    	    	Utils.showMessage(RequestAllCapabilities.this, getString(R.string.label_service_not_available));
    	    	return;
            }        	
        	
        	try {
    			// Refresh all contacts
                capabilityApi.requestAllContactsCapabilities();
                
        		// Display message
    			Utils.displayLongToast(RequestAllCapabilities.this, getString(R.string.label_refresh_success));
        	} catch(Exception e) {
    	    	e.printStackTrace();
        		Utils.showMessage(RequestAllCapabilities.this, getString(R.string.label_refresh_failed));
        	}
        }
    };
}
