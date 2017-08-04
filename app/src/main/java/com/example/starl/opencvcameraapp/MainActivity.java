package com.example.starl.opencvcameraapp;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.os.Vibrator;

import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.*;
import org.opencv.core.*;
import org.opencv.imgproc.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

/**
 * @author Tiger Kong
 */

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2{

    private static final String TAG = "OCVSample::Activity";

    private int centreImg = -1;

    GripPipeline gripPipeline;

    public static Vibrator vibe;

    private ArrayList<MatOfPoint> convexhulls;
    //this is the cameraview;
    private CameraBridgeViewBase mOpenCvCameraView;

    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity()
    {
        Log.i(TAG, "Started new "+ this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "Called onCreate");
        super.onCreate(savedInstanceState);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        if (mIsJavaCamera)
        {
            mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);
        }

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        convexhulls = new ArrayList<MatOfPoint>(0);
        gripPipeline = new GripPipeline();

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "Internal OpenCV library not found. Use OpenCV Manager for installation.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        }
        else{
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
        {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0, 0, 0);
        Core.flip(mRgbaF, mRgba, 1);

        centreImg = mRgba.cols()/2;
        //vibe.vibrate(50);
        // rotate 90 degrees
        if (mRgba == null)
        {
            Log.d(TAG, "mRgba is null because this program sucks.");
        }

        try
        {
            gripPipeline.process(mRgba);
        }
        catch (Exception e)
        {
            Log.d(TAG, "mRgba is null");
        }
        try
        {
            convexhulls = gripPipeline.filterContoursOutput();
        }
        catch (Exception e)
        {
            Log.d(TAG, "Final filter is null.");
        }


        List<Point> points;
        ArrayList<Double> centres = new ArrayList<Double>(0);
        System.out.println(convexhulls.size());

        if (convexhulls.size() == 1)
        {
            points = convexhulls.get(0).toList();
            double centre = 0;
            int psize = points.size();
            for (int x = 0; x < points.size(); x++)
            {
                centre += points.get(x).x;
            }
            centre /= psize;

            if (abs(centreImg-centre)/centreImg > 0.5)
            {
                vibe.vibrate(50);
            }
        }
        else if (convexhulls.size() != 0)
        {
            for (int x = 0; x < 2; x++)
            {
                points = convexhulls.get(x).toList();
                double centre = 0;
                int psize = points.size();
                for (int y = 0; y < points.size(); y++)
                {
                    centre += points.get(y).x;
                }
                centre /= psize;
                centres.add(centre);
            }
            //System.out.println("2_contour");
            //System.out.println((centres.get(0)+centres.get(1))/2);

            if (abs(centreImg-((centres.get(0)+centres.get(1))/2))/centreImg > 0.5)
            {
                Log.d(TAG, "Too far away from centre!");
                vibe.vibrate(50);
            }
        }
        else
        {
            Log.d(TAG, "Line contours not found!");
            vibe.vibrate(50);
        }



        return mRgba;
    }
}
