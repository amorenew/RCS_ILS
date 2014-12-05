package com.orangelabs.rcs.ri.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.orangelabs.rcs.ri.R;

public class DialActivity extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dial);
		
	}

	@Override
	public void onClick(View v) {
        Intent lectureIntent = new Intent(DialActivity.this,LectureActivity.class);
        startActivity(lectureIntent);
        finish();
	}

}
