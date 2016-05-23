package com.example.opencv_open_camera_vedio;

import java.util.Arrays;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.RotatedRect;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

import org.opencv.samples.colorblobdetect.R;
import org.opencv.video.Video;
import org.opencv.video.BackgroundSubtractorMOG;

public class MainActivity extends Activity implements OnTouchListener,
		CvCameraViewListener2 {
	private static final String TAG = "OCVSample::Activity";

	private boolean mIsColorSelected = false;
	private Mat mRgba;
	private CameraBridgeViewBase mOpenCvCameraView;
	private MatOfFloat mRanges;
	private Mat mMat0;
	private Mat mHist;
	private MatOfInt mChannels;
	private MatOfInt mHistSize;
	private int mHistSizeNum = 255;
	private Mat mProbImage;
	private RotatedRect mRect;
	private Scalar mColor;
	private Mat mHue;
	private Rect touchedRect;
	private Mat WindowHsv;
	private Mat WindowRgba;
	private Rect mTrackWindow;
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
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

	public MainActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		mRgba = inputFrame.rgba();

		if (mIsColorSelected) {
			MatOfInt fromto = new MatOfInt(0, 0);

			mHue = new Mat(WindowHsv.size(), WindowHsv.depth());
			Imgproc.cvtColor(mRgba, mHue, Imgproc.COLOR_RGB2HSV_FULL);

			Core.mixChannels(Arrays.asList(WindowHsv), Arrays.asList(mHue),
					fromto);
			Imgproc.calcHist(Arrays.asList(mHue), mChannels, mMat0, mHist,
					mHistSize, mRanges);
			Core.normalize(mHist, mHist, 0, 255, Core.NORM_MINMAX);
			Imgproc.calcBackProject(Arrays.asList(mHue), mChannels, mHist,
					mProbImage, mRanges, 1);

			mRect = Video.CamShift(mProbImage, mTrackWindow, new TermCriteria(
					TermCriteria.COUNT, 500, 1));

			Core.ellipse(mRgba, mRect, mColor);
		}
		if (mRect != null) {
			int y = (int) mRect.center.y - 50;
			int x = (int) mRect.center.x - 50;
			mTrackWindow.x = x;
			mTrackWindow.y = y;
			mTrackWindow.width = 100;
			mTrackWindow.height = 100;
		}
		return mRgba;

	}

	public void onCameraViewStarted(int width, int height) {
		mHist = new Mat();
		mColor = new Scalar(255, 0, 0);
		// mRect = new RotatedRect();
		// mTermCriteria =new TermCriteria(1,100,0.1);
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mChannels = new MatOfInt(0);
		mHistSize = new MatOfInt(mHistSizeNum);
		mRanges = new MatOfFloat(0f, 256f);
		mProbImage = new Mat();
		mMat0 = new Mat();
		WindowHsv = new Mat();
		WindowRgba = new Mat();
		mTrackWindow = new Rect();
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
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
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

	public boolean onTouch(View v, MotionEvent event) {
		int cols = mRgba.cols();
		int rows = mRgba.rows();

		int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
		int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

		int x = (int) event.getX() - xOffset;
		int y = (int) event.getY() - yOffset;
		Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

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

};
