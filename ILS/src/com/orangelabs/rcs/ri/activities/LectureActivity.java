package com.orangelabs.rcs.ri.activities;

import java.lang.reflect.Method;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingListener;
import com.gsma.services.rcs.vsh.VideoSharingService;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.sharing.video.InitiateVideoSharing;
import com.orangelabs.rcs.ri.sharing.video.media.MyVideoPlayer;
import com.orangelabs.rcs.ri.sharing.video.media.VideoSurfaceView;
import com.orangelabs.rcs.ri.utils.Utils;

public class LectureActivity extends Activity implements JoynServiceListener,
		SurfaceHolder.Callback {
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

	/**
	 * Video sharing API
	 */
	private VideoSharingService vshApi;

	/**
	 * Video sharing
	 */
	private VideoSharing videoSharing = null;

	/**
	 * Video sharing listener
	 */
	private MyVideoSharingListener vshListener = new MyVideoSharingListener();

	/**
	 * Video player
	 */
	private MyVideoPlayer videoPlayer = null;

	/**
	 * Camera of the device
	 */
	private Camera camera = null;

	/**
	 * Opened camera id
	 */
	private CameraOptions openedCameraId = CameraOptions.FRONT;

	/**
	 * Camera preview started flag
	 */
	private boolean cameraPreviewRunning = false;

	/**
	 * Video width
	 */
	private int videoWidth = H264Config.QCIF_WIDTH;

	/**
	 * Video height
	 */
	private int videoHeight = H264Config.QCIF_HEIGHT;

	/**
	 * Number of cameras
	 */
	private int numberOfCameras = 1;

	/**
	 * Live video preview
	 */
	private VideoSurfaceView videoView;

	/**
	 * Video surface holder
	 */
	private SurfaceHolder surface;

	/**
	 * Progress dialog
	 */
	private Dialog progressDialog = null;
	String remoteContact;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lecture);

		// Always on window
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		// Set title
//		setTitle(R.string.menu_initiate_video_sharing);
		setTitle("Lecture Physics Lesson 3 : Laser and Optics");

		
		shareVideo();
		
	}

	private void shareVideo() {
		// Get camera info
		numberOfCameras = getNumberOfCameras();

		// Create the live video view
		videoView = (VideoSurfaceView) findViewById(R.id.video_preview);
		videoView.setAspectRatio(videoWidth, videoHeight);
		videoView.setVisibility(View.GONE);
		surface = videoView.getHolder();
		surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surface.setKeepScreenOn(true);
		surface.addCallback(this);

		// Instanciate API
		vshApi = new VideoSharingService(getApplicationContext(), this);

		// Connect API
		vshApi.connect();
		remoteContact = "Rcs Karim";
		invite(remoteContact);
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
	public void onDestroy() {
		super.onDestroy();

		// Remove file transfer listener
		if (videoSharing != null) {
			try {
				videoSharing.removeEventListener(vshListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Disconnect API
		vshApi.disconnect();
	}

	/**
	 * Callback called when service is connected. This method is called when the
	 * service is well connected to the RCS service (binding procedure
	 * successfull): this means the methods of the API may be used.
	 */
	public void onServiceConnected() {
		// Disable button if no contact available
//		Spinner spinner = (Spinner) findViewById(R.id.contact);
//		Button dialBtn = (Button) findViewById(R.id.dial_btn);
//		if (spinner.getAdapter().getCount() != 0) {
//			dialBtn.setEnabled(true);
//		}
	}

	/**
	 * Callback called when service has been disconnected. This method is called
	 * when the service is disconnected from the RCS service (e.g. service
	 * deactivated).
	 * 
	 * @param error
	 *            Error
	 * @see JoynService.Error
	 */
	public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(LectureActivity.this,
				getString(R.string.label_api_disabled));
	}

	/**
	 * Dial button listener
	 */
	private OnClickListener btnDialListener = new OnClickListener() {
		public void onClick(View v) {
			// Get the remote contact
			Spinner spinner = (Spinner) findViewById(R.id.contact);
			MatrixCursor cursor = (MatrixCursor) spinner.getSelectedItem();
			String remote = cursor.getString(1);

			// Initiate a GSM call before to be able to share content
			Intent intent = new Intent(Intent.ACTION_CALL);
			intent.setData(Uri.parse("tel:" + remote));
			startActivity(intent);
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

	/**
	 * Video sharing event listener
	 */
	private class MyVideoSharingListener extends VideoSharingListener {
		// Sharing started
		public void onSharingStarted() {
			handler.post(new Runnable() {
				public void run() {
					// Hide progress dialog
					hideProgressDialog();
				}
			});
		}

		// Sharing aborted
		public void onSharingAborted() {
			handler.post(new Runnable() {
				public void run() {
					// Release the camera
					closeCamera();

					// Hide progress dialog
					hideProgressDialog();

					// Display session status
					Utils.showMessageAndExit(LectureActivity.this,
							getString(R.string.label_sharing_aborted));
				}
			});
		}

		// Sharing error
		public void onSharingError(final int error) {
			handler.post(new Runnable() {
				public void run() {
					// Release the camera
					closeCamera();

					// Hide progress dialog
					hideProgressDialog();

					// Display error
					if (error == VideoSharing.Error.INVITATION_DECLINED) {
						Utils.showMessageAndExit(LectureActivity.this,
								getString(R.string.label_sharing_declined));
					} else {
						Utils.showMessageAndExit(LectureActivity.this,
								getString(R.string.label_sharing_failed, error));
					}
				}
			});
		}
	};

	/**
	 * Quit the session
	 */
	private void quitSession() {
		// Release the camera
		closeCamera();

		// Stop the sharing
		try {
			if (videoSharing != null) {
				videoSharing.removeEventListener(vshListener);
				videoSharing.abortSharing();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		videoSharing = null;

		// Exit activity
		finish();
	}

	/*-------------------------- Camera methods ------------------*/

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO
	}

	/*-------------------------- Camera methods ------------------*/

	/**
	 * Open the camera
	 */
	private synchronized void openCamera() {
		if (camera == null) {
			// Open camera
			openCamera(openedCameraId);
			videoView.setAspectRatio(videoWidth, videoHeight);

			// Start camera
			camera.setPreviewCallback(videoPlayer);
			startCameraPreview();
		}
	}

	/**
	 * Close the camera
	 */
	private synchronized void closeCamera() {
		if (camera != null) {
			camera.setPreviewCallback(null);
			if (cameraPreviewRunning) {
				cameraPreviewRunning = false;
				camera.stopPreview();
			}
			camera.release();
			camera = null;
		}
	}

	/**
	 * Start the camera preview
	 */
	private void startCameraPreview() {
		if (camera != null) {
			// Camera settings
			Camera.Parameters p = camera.getParameters();
			p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

			// Orientation
			p.setRotation(90);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
				Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
						.getDefaultDisplay();
				switch (display.getRotation()) {
				case Surface.ROTATION_0:
					if (openedCameraId == CameraOptions.FRONT) {
						videoPlayer.setOrientation(Orientation.ROTATE_90_CCW);
					} else {
						videoPlayer.setOrientation(Orientation.ROTATE_90_CW);
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
						camera.setDisplayOrientation(90);
					} else {
						p.setRotation(90);
					}
					break;
				case Surface.ROTATION_90:
					videoPlayer.setOrientation(Orientation.NONE);
					break;
				case Surface.ROTATION_180:
					if (openedCameraId == CameraOptions.FRONT) {
						videoPlayer.setOrientation(Orientation.ROTATE_90_CW);
					} else {
						videoPlayer.setOrientation(Orientation.ROTATE_90_CCW);
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
						camera.setDisplayOrientation(270);
					} else {
						p.setRotation(270);
					}
					break;
				case Surface.ROTATION_270:
					if (openedCameraId == CameraOptions.FRONT) {
						videoPlayer.setOrientation(Orientation.ROTATE_180);
					} else {
						videoPlayer.setOrientation(Orientation.ROTATE_180);
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
						camera.setDisplayOrientation(180);
					} else {
						p.setRotation(180);
					}
					break;
				}
			} else {
				// getRotation not managed under Froyo
				videoPlayer.setOrientation(Orientation.NONE);
			}

			// Use the existing size without resizing
			p.setPreviewSize(videoWidth, videoHeight);
			videoPlayer.activateResizing(videoWidth, videoHeight);

			// Set camera parameters
			camera.setParameters(p);
			try {
				camera.setPreviewDisplay(videoView.getHolder());
				camera.startPreview();
				cameraPreviewRunning = true;
			} catch (Exception e) {
				camera = null;
			}
		}
	}

	/**
	 * Get Camera "open" Method
	 * 
	 * @return Method
	 */
	private Method getCameraOpenMethod() {
		ClassLoader classLoader = InitiateVideoSharing.class.getClassLoader();
		Class cameraClass = null;
		try {
			cameraClass = classLoader.loadClass("android.hardware.Camera");
			try {
				return cameraClass.getMethod("open", new Class[] { int.class });
			} catch (NoSuchMethodException e) {
				return null;
			}
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * Open the camera
	 * 
	 * @param cameraId
	 *            Camera ID
	 */
	private void openCamera(CameraOptions cameraId) {
		Method method = getCameraOpenMethod();
		if (numberOfCameras > 1 && method != null) {
			try {
				camera = (Camera) method.invoke(camera,
						new Object[] { cameraId.getValue() });
				openedCameraId = cameraId;
			} catch (Exception e) {
				camera = Camera.open();
				openedCameraId = CameraOptions.BACK;
			}
		} else {
			camera = Camera.open();
		}
		if (videoPlayer != null) {
			videoPlayer.setCameraId(openedCameraId.getValue());
		}
	}

	/**
	 * Get Camera "numberOfCameras" Method
	 * 
	 * @return Method
	 */
	private Method getCameraNumberOfCamerasMethod() {
		ClassLoader classLoader = InitiateVideoSharing.class.getClassLoader();
		Class cameraClass = null;
		try {
			cameraClass = classLoader.loadClass("android.hardware.Camera");
			try {
				return cameraClass.getMethod("getNumberOfCameras",
						(Class[]) null);
			} catch (NoSuchMethodException e) {
				return null;
			}
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * Get number of cameras
	 * 
	 * @return number of cameras
	 */
	private int getNumberOfCameras() {
		Method method = getCameraNumberOfCamerasMethod();
		if (method != null) {
			try {
				Integer ret = (Integer) method.invoke(null, (Object[]) null);
				return ret.intValue();
			} catch (Exception e) {
				return 1;
			}
		} else {
			return 1;
		}
	}

	private void invite(final String remoteContact) {
		// Check if the service is available
		boolean registered = false;
		try {
			if ((vshApi != null) && vshApi.isServiceRegistered()) {
				registered = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!registered) {
			Utils.showMessage(LectureActivity.this,
					getString(R.string.label_service_not_available));
			return;
		}

		// Get the remote contact
		// Spinner spinner = (Spinner)findViewById(R.id.contact);
		// MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
		// final String remote = cursor.getString(1);

		Thread thread = new Thread() {
			public void run() {
				try {
					// Create the video player
					videoPlayer = new MyVideoPlayer();

					// Start the camera
					openCamera();

					// Initiate sharing
					videoSharing = vshApi.shareVideo(remoteContact,
							videoPlayer, vshListener);
				} catch (Exception e) {
					e.printStackTrace();
					handler.post(new Runnable() {
						public void run() {
							hideProgressDialog();
							Utils.showMessageAndExit(LectureActivity.this,
									getString(R.string.label_invitation_failed));
						}
					});
				}
			}
		};
		thread.start();

		// Display a progress dialog
		progressDialog = Utils.showProgressDialog(LectureActivity.this,
				getString(R.string.label_command_in_progress));
		progressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Toast.makeText(LectureActivity.this,
						getString(R.string.label_sharing_cancelled),
						Toast.LENGTH_SHORT).show();
				quitSession();
			}
		});

		// Disable UI
		// spinner.setEnabled(false);

		// Display video view
		videoView.setVisibility(View.VISIBLE);

		// Hide buttons
		Button inviteBtn = (Button) findViewById(R.id.invite_btn);
		inviteBtn.setVisibility(View.GONE);
		Button dialBtn = (Button) findViewById(R.id.dial_btn);
		dialBtn.setVisibility(View.GONE);
	}

}
