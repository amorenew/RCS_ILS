package com.orangelabs.rcs.ils.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.orangelabs.rcs.ri.R;

public class DialActivity extends Activity {
	Button btnLecture;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dial);
		btnLecture = (Button) findViewById(R.id.btnLecture);
		btnLecture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent lectureIntent = new Intent(DialActivity.this,
						LectureActivity.class);
				startActivity(lectureIntent);
				finish();
			}
		});
	}

}
