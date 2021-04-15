
package niu.bdlab.turfflev2;

import android.animation.Animator;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.support.design.widget.FloatingActionButton;

import com.serenegiant.common.BaseActivity;

import com.serenegiant.encoder.MediaMuxerWrapper;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.utils.ViewAnimationHelper;
import com.serenegiant.widget.CameraViewInterface;

import niu.bdlab.turfflev2.SensorDialog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent,SensorDialog.SensorDialogParent{
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "MainActivity";

	/**
	 * set true if you want to record movie using MediaSurfaceEncoder
	 * (writing frame data into Surface camera from MediaCodec
	 *  by almost same way as USBCameratest2)
	 * set false if you want to record movie using MediaVideoEncoder
	 */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static int PREVIEW_WIDTH = 1600;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static int PREVIEW_HEIGHT = 1200;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 1;

	protected static final int SETTINGS_HIDE_DELAY_MS = 2500;

	/**
	 * for accessing USB
	 */
	private USBMonitor mUSBMonitor;
	/**
	 * Handler to execute camera related methods sequentially on private thread
	 */
	private static UVCCameraHandler mCameraHandler;
	/**
	 * for camera preview display
	 */
	private CameraViewInterface mUVCCameraView;
	/**
	 * for open&start / stop&close camera preview
	 */
	private ToggleButton mCameraButton;
	/**
	 * button for start/stop recording
	 */
	private FloatingActionButton mCaptureButton;

	private ImageButton mViewPicButton;

	private View mBrightnessButton, mContrastButton, mResolutionButton;
	private View mResetButton;
	private View mToolsLayout, mValueLayout;
	private SeekBar mSettingSeekbar;
	private ImageView mPicPreview;

	private List<Size> camera_sizes;

	private final static int PHOTO = 99 ;
	private static String svpath;

	private Button connect,F,B,S;
	private TextView text;
	private EditText mrollnumber;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	private final static int REQUEST_ENABLE_BT = 1;
	private BluetoothDevice mDevice;
	private BluetoothSocket mmSocket;
	private InputStream mmInStream;
	private OutputStream mmOutStream;
	boolean status = false;
	private String deviceHardwareAddress;
	private ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "onCreate:");
		setContentView(R.layout.activity_main);
		mCameraButton = (ToggleButton)findViewById(R.id.camera_button);
		mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
		mCaptureButton = (FloatingActionButton)findViewById(R.id.capture_button);
		mCaptureButton.setOnClickListener(mOnClickListener);
		mCaptureButton.setVisibility(View.INVISIBLE);
		mViewPicButton = (ImageButton)findViewById(R.id.viewPIC_Button);
		mViewPicButton.setOnClickListener(mOnClickListener);
		final View view = findViewById(R.id.camera_view);
		view.setOnLongClickListener(mOnLongClickListener);
		mUVCCameraView = (CameraViewInterface)view;
		mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);

		mBrightnessButton = findViewById(R.id.brightness_button);
		mBrightnessButton.setOnClickListener(mOnClickListener);
		mContrastButton = findViewById(R.id.contrast_button);
		mContrastButton.setOnClickListener(mOnClickListener);
		mResolutionButton = findViewById(R.id.resolution_button);
		mResolutionButton.setOnClickListener(mOnClickListener);
		mResetButton = findViewById(R.id.reset_button);
		mResetButton.setOnClickListener(mOnClickListener);
		mSettingSeekbar = (SeekBar)findViewById(R.id.setting_seekbar);
		mSettingSeekbar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		mPicPreview=(ImageView)findViewById(R.id.picPreview);
		mPicPreview.setVisibility(View.INVISIBLE);
		mPicPreview.setOnLongClickListener(mOnLongClickListener);

		mToolsLayout = findViewById(R.id.tools_layout);
		mToolsLayout.setVisibility(View.INVISIBLE);
		mValueLayout = findViewById(R.id.value_layout);
		mValueLayout.setVisibility(View.INVISIBLE);
		mrollnumber = findViewById(R.id.roll_number);
		mrollnumber.setVisibility(View.VISIBLE);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
		mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
			USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
		if (mBluetoothAdapter == null) {
			Toast.makeText(MainActivity.this,"Device does not support Bluetooth",Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(MainActivity.this,"Any valid Bluetooth operations",Toast.LENGTH_SHORT).show();
		}
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		setUp();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.v(TAG, "onStart:");
		mUSBMonitor.register();
		if (mUVCCameraView != null)
			mUVCCameraView.onResume();
	}

	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "onStop:");
		mCameraHandler.close();
		if (mUVCCameraView != null)
			mUVCCameraView.onPause();
		setCameraButton(false);
		super.onStop();
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
        if (mCameraHandler != null) {
	        mCameraHandler.release();
	        mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
	        mUSBMonitor.destroy();
	        mUSBMonitor = null;
        }
        mUVCCameraView = null;
        mCameraButton = null;
        mCaptureButton = null;
		super.onDestroy();
	}

	private final OnTouchListener mOnTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			switch (view.getId()) {
				case R.id.F_Button:
					if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
						push("F");
					} else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
						push("S");
						//Toast.makeText(MainActivity.this,"S",Toast.LENGTH_SHORT).show();
					}break;
				case R.id.B_Button:
					if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
						push("B");
					} else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
						push("S");
						//Toast.makeText(MainActivity.this,"S",Toast.LENGTH_SHORT).show();
					 }break;
			}
				return false;
			}};
	/**
	 * event handler when click camera / capture button
	 */
	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			switch (view.getId()) {
			case R.id.capture_button:
				if (mCameraHandler.isOpened()) {
					if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
						if (!mCameraHandler.isRecording()) {
							//mCaptureButton.setBackgroundColor(0xffff0000);
							mCaptureButton.setColorFilter(0xffff0000);	// turn red
							mCameraHandler.startRecording();
						} else {
							//mCaptureButton.setBackgroundColor(0);
							mCaptureButton.setColorFilter(0);	// return to default color
							mCameraHandler.stopRecording();
						}
					}
				}
				break;
			case R.id.viewPIC_Button:
				AlertDialog.Builder dialog_vi = new AlertDialog.Builder(MainActivity.this);
				String[] chs = {getString(R.string.img),getString(R.string.mov)};
				dialog_vi.setItems(chs, (dialog, which) -> {
					Toast.makeText(MainActivity.this, chs[which], Toast.LENGTH_SHORT).show();
					opensavePath(which);
				}).setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
				}).show();
				break;
			case R.id.brightness_button:
				showSettings(UVCCamera.PU_BRIGHTNESS);
				break;
			case R.id.contrast_button:
				showSettings(UVCCamera.PU_CONTRAST);
				break;
			case R.id.resolution_button:
				AlertDialog.Builder dialog_list = new AlertDialog.Builder(MainActivity.this);
				dialog_list.setTitle(getString(R.string.previewSize));
				String[] resolutions = new String[camera_sizes.size()];
				for(int is=0;is<camera_sizes.size();is++)
					resolutions[is]=camera_sizes.get(is).toString().split("@")[0]+")";
				dialog_list.setItems(resolutions, (dialog, which) -> {
					// TODO Auto-generated method stub
					Toast.makeText(MainActivity.this, resolutions[which], Toast.LENGTH_SHORT).show();
				}).setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
					Toast.makeText(MainActivity.this, getString(R.string.cancel), Toast.LENGTH_SHORT).show();
				});
				dialog_list.show();
				break;
			case R.id.BLE_button:
				if(connect.getText().toString().equals(getResources().getString(R.string.bleconneect))) {
					Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
					for (BluetoothDevice device : pairedDevices) {
						String deviceName = device.getName();
						deviceHardwareAddress = device.getAddress(); // MAC address
						adapter.add(deviceName + "\n" + deviceHardwareAddress);
					}
					AlertDialog.Builder bleList = new AlertDialog.Builder(MainActivity.this);
					bleList.setTitle("Paired Device");
					bleList.setAdapter(adapter, (dialog, which) -> {
						String itemValue = (String) adapter.getItem(which);
						String MAC = itemValue.substring(itemValue.length() - 17);
						mDevice = mBluetoothAdapter.getRemoteDevice(MAC);
						// Initiate a connection request in a separate thread
						ConnectThread t = new ConnectThread(mDevice);
						t.start();
						ConnectedThread cet = new ConnectedThread(mmSocket);
						F.setEnabled(true);
						F.getBackground().setAlpha(255);
						B.setEnabled(true);
						B.getBackground().setAlpha(255);
						S.setEnabled(true);
						S.getBackground().setAlpha(255);
						connect.setText(R.string.cancel);
					});
					bleList.show();
				}else{
					connect.setText(R.string.bleconneect);
					F.setEnabled(false);
					F.getBackground().setAlpha(0);
					B.setEnabled(false);
					B.getBackground().setAlpha(0);
					S.setEnabled(false);
					S.getBackground().setAlpha(0);
				}
				break;
			case R.id.S_Button:
				String roll_number = mrollnumber.getText().toString();
				Toast.makeText(MainActivity.this,String.valueOf(roll_number.charAt(0))+String.valueOf(roll_number.charAt(1))+String.valueOf(roll_number.charAt(2)),Toast.LENGTH_SHORT).show();
				if(Integer.parseInt(roll_number)<256 && Integer.parseInt(roll_number)>0){
					push(String.valueOf(roll_number.charAt(0)));
					push(String.valueOf(roll_number.charAt(1)));
					push(String.valueOf(roll_number.charAt(2)));
				}
				break;
			case R.id.reset_button:
				resetSettings();
				break;
			}
		}
	};

	private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
		= new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
			switch (compoundButton.getId()) {
			case R.id.camera_button:
				if (isChecked && !mCameraHandler.isOpened()) {
					CameraDialog.showDialog(MainActivity.this);
				} else {
					mCameraHandler.close();
					setCameraButton(false);
				}
				break;
			}
		}
	};


	/**
	 * capture still image when you long click on preview image(not on buttons)
	 */
	private final OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick(final View view) {
			switch (view.getId()) {
			case R.id.camera_view:
				if (mCameraHandler.isOpened()) {
					if (checkPermissionWriteExternalStorage()) {
						svpath = String.valueOf(MediaMuxerWrapper.getCaptureFile(Environment.DIRECTORY_DCIM, ".png"));
						mCameraHandler.captureStill(svpath);
						SensorDialog.showlog(MainActivity.this);
					}
					return true;
				}
			case R.id.picPreview:
				mPicPreview.destroyDrawingCache();
				mPicPreview.setVisibility(View.INVISIBLE);
				mCameraButton.setVisibility(View.VISIBLE);
			}
			return false;
		}
	};

	@Override
	public String getSavePath() {
		return svpath;
	}

	private void setCameraButton(final boolean isOn) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mCameraButton != null) {
					try {
						mCameraButton.setOnCheckedChangeListener(null);
						mCameraButton.setChecked(isOn);
					} finally {
						mCameraButton.setOnCheckedChangeListener(mOnCheckedChangeListener);
					}
				}
				if (!isOn && (mCaptureButton != null)) {
					mCaptureButton.setVisibility(View.INVISIBLE);
				}
			}
		}, 0);
		updateItems();
	}

	private void startPreview() {
		final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
		mCameraHandler.startPreview(new Surface(st));
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mCaptureButton.setVisibility(View.VISIBLE);
			}
		});
		updateItems();
	}

	private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
		@Override
		public void onAttach(UsbDevice device) {
			Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
			// this is called when user connect UVC camera and Android detected it.
			// if you want to use camera automatically, request permission here like as follows.
			final int n = mUSBMonitor.getDeviceCount();
			if ((device == null) && (n > 0)) {
				// #onAttach will be called with null argument when USBMonitor detect device connection without intent.
				final List<UsbDevice> devices = mUSBMonitor.getDeviceList();
				// set first one
				device = devices.get(0);
			}
			if (n == 1) {
				// if there is only one camera, request permission.
				// first time you call #requestPermission or app has no permission, Android shows permission dialog.
				// if app already has permission or user give permission on permission dialog, USBMonitor call #onConnect callback method.
				final boolean result = mUSBMonitor.requestPermission(device);
				if (result) {
					// when failed. your device may not support USB.
				}
			} else if (n > 1) {
				// show dialog to select camera
//				CameraDialog.showDialog(this);
				// or if you know your device-id/product-id that you want to use and device is that one, request permission
			}
		}

		@Override
		public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
			if (DEBUG) Log.v(TAG, "onConnect:");
			final UVCCamera camera = new UVCCamera();
			camera.open(ctrlBlock);
			if (DEBUG) Log.i(TAG, "supportedSize:" + camera.getSupportedSize());
			camera_sizes = camera.getSupportedSizeList();
			camera.destroy();
			mCameraHandler.open(ctrlBlock);
			startPreview();
			updateItems();
		}

		@Override
		public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.v(TAG, "onDisconnect:");
			if (mCameraHandler != null) {
				queueEvent(new Runnable() {
					@Override
					public void run() {
						mCameraHandler.close();
					}
				}, 0);
				setCameraButton(false);
				updateItems();
			}

		}
		@Override
		public void onDettach(final UsbDevice device) {
			Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onCancel(final UsbDevice device) {
			setCameraButton(false);
		}
	};

	/**
	 * to access from CameraDialog
	 * @return
	 */
	@Override
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	@Override
	public void onDialogResult(boolean canceled) {
		if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
		if (canceled) {
			setCameraButton(false);
		}
	}

//================================================================================
	private boolean isActive() {
		return mCameraHandler != null && mCameraHandler.isOpened();
	}

	private boolean checkSupportFlag(final int flag) {
		return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
	}

	private int getValue(final int flag) {
		return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
	}

	private int setValue(final int flag, final int value) {
		return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
	}

	private int resetValue(final int flag) {
		return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
	}

	private void updateItems() {
		runOnUiThread(mUpdateItemsOnUITask, 100);
	}

	private final Runnable mUpdateItemsOnUITask = new Runnable() {
		@Override
		public void run() {
			if (isFinishing()) return;
			final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
			mToolsLayout.setVisibility(visible_active);
			mBrightnessButton.setVisibility(
		    	checkSupportFlag(UVCCamera.PU_BRIGHTNESS)
		    	? visible_active : View.INVISIBLE);
			mContrastButton.setVisibility(
		    	checkSupportFlag(UVCCamera.PU_CONTRAST)
		    	? visible_active : View.INVISIBLE);
			mResolutionButton.setVisibility(visible_active);
		}
	};

	private int mSettingMode = -1;
	/**
	 * 設定画面を表示
	 * @param mode
	 */
	private final void showSettings(final int mode) {
		if (DEBUG) Log.v(TAG, String.format("showSettings:%08x", mode));
		hideSetting(false);
		if (isActive()) {
			switch (mode) {
			case UVCCamera.PU_BRIGHTNESS:
			case UVCCamera.PU_CONTRAST:
				mSettingMode = mode;
				mSettingSeekbar.setProgress(getValue(mode));
				ViewAnimationHelper.fadeIn(mValueLayout, -1, 0, mViewAnimationListener);
				break;
			}
		}
	}

	private void resetSettings() {
		if (isActive()) {
			switch (mSettingMode) {
			case UVCCamera.PU_BRIGHTNESS:
			case UVCCamera.PU_CONTRAST:
				mSettingSeekbar.setProgress(resetValue(mSettingMode));
				break;
			}
		}
		mSettingMode = -1;
		ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
	}

	/**
	 * 設定画面を非表示にする
	 * @param fadeOut trueならばフェードアウトさせる, falseなら即座に非表示にする
	 */
	protected final void hideSetting(final boolean fadeOut) {
		removeFromUiThread(mSettingHideTask);
		if (fadeOut) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener);
				}
			}, 0);
		} else {
			try {
				mValueLayout.setVisibility(View.GONE);
			} catch (final Exception e) {
				// ignore
			}
			mSettingMode = -1;
		}
	}

	protected final Runnable mSettingHideTask = new Runnable() {
		@Override
		public void run() {
			hideSetting(true);
		}
	};

	/**
	 * 設定値変更用のシークバーのコールバックリスナー
	 */
	private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
			// 設定が変更された時はシークバーの非表示までの時間を延長する
			if (fromUser) {
				runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
			}
		}

		@Override
		public void onStartTrackingTouch(final SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(final SeekBar seekBar) {
			// シークバーにタッチして値を変更した時はonProgressChangedへ
			// 行かないみたいなのでここでも非表示までの時間を延長する
			runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
			if (isActive() && checkSupportFlag(mSettingMode)) {
				switch (mSettingMode) {
				case UVCCamera.PU_BRIGHTNESS:
				case UVCCamera.PU_CONTRAST:
					setValue(mSettingMode, seekBar.getProgress());
					break;
				}
			}	// if (active)
		}
	};

	private final ViewAnimationHelper.ViewAnimationListener
		mViewAnimationListener = new ViewAnimationHelper.ViewAnimationListener() {
		@Override
		public void onAnimationStart(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
		}

		@Override
		public void onAnimationEnd(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
			final int id = target.getId();
			switch (animationType) {
			case ViewAnimationHelper.ANIMATION_FADE_IN:
			case ViewAnimationHelper.ANIMATION_FADE_OUT:
			{
				final boolean fadeIn = animationType == ViewAnimationHelper.ANIMATION_FADE_IN;
				if (id == R.id.value_layout) {
					if (fadeIn) {
						runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS);
					} else {
						mValueLayout.setVisibility(View.GONE);
						mSettingMode = -1;
					}
				} else if (!fadeIn) {
//					target.setVisibility(View.GONE);
				}
				break;
			}
			}
		}

		@Override
		public void onAnimationCancel(@NonNull final Animator animator, @NonNull final View target, final int animationType) {
//			if (DEBUG) Log.v(TAG, "onAnimationStart:");
		}
	};

	private void opensavePath(int ch){
		String DIR_NAME = "USBCamera";
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.setAction(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		File file;
		switch (ch)
		{
			case 0:
				file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), DIR_NAME);
				if(null==file || !file.exists()){
					return;
				}
				intent.setDataAndType(FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file),"image/*");
				//intent.setType("image/*");
				break;
			case 1:
				//file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), DIR_NAME);
				//if(null==file || !file.exists()){
				//	return;
				//}
				//intent.setDataAndType(FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file),"video/*");
				intent.setType("video/*");
				break;
			default:
				break;
		}
		//Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		//intent.setDataAndType(FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file), "*/*");
		//intent.addCategory(Intent.CATEGORY_OPENABLE);
		//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//startActivity(intent.createChooser(intent,"选择浏览工具"));

		startActivityForResult(intent, PHOTO);
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case PHOTO:
				if (resultCode == RESULT_OK) {
					Uri uri = data.getData();
					assert uri != null;
					String path = getPath(MainActivity.this,uri);

					assert path != null;
					if (path.contains(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)))) {
						BitmapFactory.Options option = new BitmapFactory.Options();
						option.inJustDecodeBounds = true;
						option.inPurgeable = true;
						Bitmap bm = BitmapFactory.decodeFile(path, option);
						int wRatio = (int) Math.ceil(option.outWidth / mPicPreview.getWidth());
						int hRatio = (int) Math.ceil(option.outHeight / mPicPreview.getHeight());
						if (wRatio > 1 && hRatio > 1) {
							if (wRatio > hRatio) {
								option.inSampleSize = wRatio;
							} else {
								option.inSampleSize = hRatio;
							}
						}
						option.inJustDecodeBounds = false;
						bm = BitmapFactory.decodeFile(path, option);
						mPicPreview.setVisibility(View.VISIBLE);
						mCameraButton.setVisibility(View.INVISIBLE);
						mPicPreview.setImageBitmap(bm);
						mPicPreview.bringToFront();
						Toast.makeText(MainActivity.this, path, Toast.LENGTH_SHORT).show();
					}
				};
			default:
				break;
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
	public static String getPath(final Context context, final Uri uri) {

		// check here to KITKAT or new version
		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

			// ExternalStorageProvider
			/*if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/"
							+ split[1];
				}
			}*/
			// DownloadsProvider
			/*else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}*/
			// MediaProvider
			if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] { split[1] };

				return getDataColumn(context, contentUri, selection,
						selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {

			// Return the remote address
			if (isGooglePhotosUri(uri))
				return uri.getLastPathSegment();

			return getDataColumn(context, uri, null, null);
		}
		// File
		/*else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}*/
		return null;
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context
	 *            The context.
	 * @param uri
	 *            The Uri to query.
	 * @param selection
	 *            (Optional) Filter used in the query.
	 * @param selectionArgs
	 *            (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri,
									   String selection, String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection,
					selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {return "com.android.externalstorage.documents".equals(uri.getAuthority());}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {return "com.android.providers.downloads.documents".equals(uri.getAuthority());}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {return "com.android.providers.media.documents".equals(uri.getAuthority());}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(Uri uri) {return "com.google.android.apps.photos.content".equals(uri.getAuthority());}

	private void setUp(){
		connect = (Button)findViewById(R.id.BLE_button);
		connect.setOnClickListener(mOnClickListener);
		F = (Button)findViewById(R.id.F_Button);
		F.setEnabled(false);
		F.getBackground().setAlpha(0);
		F.setOnTouchListener(mOnTouchListener);
		B = (Button)findViewById(R.id.B_Button);
		B.setEnabled(false);
		B.getBackground().setAlpha(0);
		B.setOnTouchListener(mOnTouchListener);
		S = (Button)findViewById(R.id.S_Button);
		S.setEnabled(false);
		S.getBackground().setAlpha(0);
		S.setOnClickListener(mOnClickListener);
		adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
	}

	private class ConnectThread extends Thread {

		private final BluetoothDevice mmDevice;
		private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket tmp = null;
			mmDevice = device;

			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				Log.d(TAG, "Socket's create() sucess");
			} catch (IOException e) {
				Log.e(TAG, "Socket's create() method failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			mBluetoothAdapter.cancelDiscovery();

			try {
				mmSocket.connect();
				status = true;
			} catch (IOException connectException) {
				try {
					mmSocket.close();
				} catch (IOException closeException) {
					Log.e(TAG, "Could not close the client socket", closeException);
				}
				return;
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(status==true){
						Toast.makeText(MainActivity.this,"Sucessfully connected",Toast.LENGTH_SHORT).show();
					}
					else{
						Toast.makeText(MainActivity.this,"connection failed",Toast.LENGTH_SHORT).show();

					}

				}
			});

		}


	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
				Log.d(TAG, "Input Output stream create sucessfully");

			} catch (IOException e) {

				Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

	}

	public void push(String commend) {
		try {
			mmOutStream.write(commend.getBytes());
			Log.d(TAG, "commend"+commend+" send sucessfully");
		} catch (IOException e) {
			Log.e(TAG, "Error occurred when sending data", e);
		}
	}
}
