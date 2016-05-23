package com.example.opencv_open_camera_vedio;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.samples.colorblobdetect.R;
import org.opencv.video.Video;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity implements OnTouchListener,
		CvCameraViewListener2 {
	private boolean mIsColorSelected = false;
	private Mat mRgba;
	private CameraBridgeViewBase mOpenCvCameraView;
	private Mat mHist;
	private Mat mProbImage;
	private RotatedRect mRect;
	private Scalar mColor;
	private Mat mHue;
	private Rect touchedRect;
	private Rect mTrackWindow;
	CAMShiftDetection camshift;
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.setOnTouchListener(MainActivity.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		mRgba = inputFrame.rgba();

		if (mIsColorSelected) {
			mHue = camshift.getHue(mRgba);// 获取hue分量
			mHist = camshift.getImageHistogram(mHue, mHue.size(), 10, 0, 180);
			Core.normalize(mHist, mHist, 0, 255, Core.NORM_MINMAX);
			mProbImage = camshift.getBackProjection(mHue, mHist, 0, 180, 1.0);

			mRect = Video.CamShift(mProbImage, mTrackWindow, new TermCriteria(
					TermCriteria.COUNT, 500, 1));

			Core.ellipse(mRgba, mRect, mColor);
		}
		if (mRect != null) {
			int y = (int) mRect.center.y - 100;
			int x = (int) mRect.center.x - 100;
			mTrackWindow.x = x;
			mTrackWindow.y = y;
			mTrackWindow.width = 200;
			mTrackWindow.height = 200;
		}
		return mRgba;
	}

	public void onCameraViewStarted(int width, int height) {
		mColor = new Scalar(0, 0, 255);
		mTrackWindow = new Rect();
		camshift = new CAMShiftDetection();
	}

	public boolean onTouch(View v, MotionEvent event) {
		int cols = mRgba.cols();
		int rows = mRgba.rows();

		int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
		int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

		int x = (int) event.getX() - xOffset;
		int y = (int) event.getY() - yOffset;

		if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
			return false;

		touchedRect = new Rect();

		touchedRect.x = x - 100;
		touchedRect.y = y - 100;

		touchedRect.width = 200;
		touchedRect.height = 200;

		mTrackWindow = touchedRect;

		mIsColorSelected = true;

		return false; // don't need subsequent touch events
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.color_blob_detection_surface_view);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStopped() {
		mRgba.release();
	}

};
