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

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingLog;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List image sharings from the content provider 
 *   
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingList extends Activity {
	/**
	 * List view
	 */
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_list);
        
        // Set title
        setTitle(R.string.menu_image_sharing_log);

        // Set list adapter
        listView = (ListView)findViewById(android.R.id.list);
        TextView emptyView = (TextView)findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);
    }
    
	@Override
	protected void onResume() {
		super.onResume();

		// Refresh view
		listView.setAdapter(createListAdapter());
	}    
    
	/**
	 * Create list adapter
	 */
	private ImageSharingListAdapter createListAdapter() {
		Uri uri = ImageSharingLog.CONTENT_URI;
        String[] projection = new String[] {
    		ImageSharingLog.ID,
    		ImageSharingLog.CONTACT_NUMBER,
    		ImageSharingLog.FILENAME,
    		ImageSharingLog.FILESIZE,
    		ImageSharingLog.STATE,
    		ImageSharingLog.DIRECTION,
    		ImageSharingLog.TIMESTAMP
    		};
        String sortOrder = ImageSharingLog.TIMESTAMP + " DESC ";
		Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
		}
		return new ImageSharingListAdapter(this, cursor);
	}
	
    /**
     * List adapter
     */
    private class ImageSharingListAdapter extends CursorAdapter {
    	/**
    	 * Constructor
    	 * 
    	 * @param context Context
    	 * @param c Cursor
    	 */
		public ImageSharingListAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.image_sharing_list_item, parent, false);
            
            ImageSharingItemCache cache = new ImageSharingItemCache();
    		cache.number = cursor.getString(1);
    		cache.filename = cursor.getString(2);
    		cache.filesize = cursor.getLong(3);
    		cache.state = cursor.getInt(4);
    		cache.direction = cursor.getInt(5);
    		cache.date = cursor.getLong(6);
            view.setTag(cache);
            
            return view;
        }
        
    	@Override
    	public void bindView(View view, Context context, Cursor cursor) {
    		ImageSharingItemCache cache = (ImageSharingItemCache)view.getTag();
    		TextView numberView = (TextView)view.findViewById(R.id.number);
    		numberView.setText(getString(R.string.label_contact, cache.number));
    		TextView filenameView = (TextView)view.findViewById(R.id.filename);
    		filenameView.setText(getString(R.string.label_filename, cache.filename));
    		TextView filesizeView = (TextView)view.findViewById(R.id.filesize);
    		filesizeView.setText(getString(R.string.label_filesize, cache.filesize));
    		TextView stateView = (TextView)view.findViewById(R.id.state);
    		stateView.setText(getString(R.string.label_session_state, decodeState(cache.state)));
    		TextView directionView = (TextView)view.findViewById(R.id.direction);
    		directionView.setText(getString(R.string.label_direction, decodeDirection(cache.direction)));
    		TextView dateView = (TextView)view.findViewById(R.id.date);
    		dateView.setText(getString(R.string.label_session_date, decodeDate(cache.date)));
    	}
    }

    /**
     * Image sharing item in cache
     */
	private class ImageSharingItemCache {
		public String number;
		public String filename;
		public long filesize;
		public int direction;
		public int state;
		public long date;
	}    
 
	/**
	 * Decode state
	 * 
	 * @param state State
	 * @return String
	 */
	private String decodeState(int state) {
		if (state == ImageSharing.State.ABORTED) {
			return getString(R.string.label_state_aborted);
		} else
		if (state == ImageSharing.State.FAILED) {
			return getString(R.string.label_state_failed);
		} else
		if (state == ImageSharing.State.INITIATED) {
			return getString(R.string.label_state_initiated);
		} else
		if (state == ImageSharing.State.INVITED) {
			return getString(R.string.label_state_invited);
		} else
		if (state == ImageSharing.State.STARTED) {
			return getString(R.string.label_state_started);
		} else
		if (state == ImageSharing.State.TRANSFERRED) {
			return getString(R.string.label_state_transferred);
		} else {
			return getString(R.string.label_state_unknown);
		}
	}

	/**
	 * Decode direction
	 * 
	 * @param direction Direction
	 * @return String
	 */
	private String decodeDirection(int direction) {
		if (direction == ImageSharing.Direction.INCOMING) {
			return getString(R.string.label_incoming);
		} else {
			return getString(R.string.label_outgoing);
		}
	}

	/**
	 * Decode date
	 * 
	 * @param date Date
	 * @return String
	 */
	private String decodeDate(long date) {
		return DateFormat.getInstance().format(new Date(date));
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_log, menu);

		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_clear_log:
				// Delete all
				getContentResolver().delete(ImageSharingLog.CONTENT_URI, null, null);
				
				// Refresh view
		        listView.setAdapter(createListAdapter());	
		        break;
		}
		return true;
	}
}
