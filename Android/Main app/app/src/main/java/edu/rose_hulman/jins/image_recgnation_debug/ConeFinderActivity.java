package edu.rose_hulman.jins.image_recgnation_debug;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.List;

import edu.rose_hulman.everhadg.ColorBlobDetector;
import edu.rose_hulman.jins.final_project_main.MainCommandBin;
import edu.rose_hulman.jins.final_project_main.R;

public class ConeFinderActivity extends MainCommandBin implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    /**
     * Records the latest boolean value for the cone found status.  True mean cone is visible.
     */
    public boolean mConeFound;

    /**
     * If mConeFound is true, then the location and size of the cone is described by these fields.
     */
    public double mConeLeftRightLocation, mConeTopBottomLocation, mConeSize;


    /**
     * Constants and variables used by OpenCV4Android. Don't mess with these. ;)
     */
    private ColorBlobDetector mDetector;
    private Scalar CONTOUR_COLOR = new Scalar(0, 0, 255, 255);
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ConeFinderActivity.this);
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    // Set the color to track...
    /**
     * Target color. An inside cone has an orange hue around 5 - 15, full saturation and value. (change as needed when outside)
     */
    public int mConeTargetH = 5;
    public int mConeTargetS = 255;
    public int mConeTargetV = 255;

    /**
     * Range of acceptable colors. (change as needed)
     */
    public int mConeRangeH = 25;
    public int mConeRangeS = 50;
    public int mConeRangeV = 50;

    /**
     * Minimum size needed to consider the target a cone. (change as needed)
     */
    public double min_size_percentage = 0.001;

    /**
     * Screen size variables.
     */
    private double mCameraViewWidth;
    private double mCameraViewHeight;
    private double mCameraViewArea;

    /**
     * References to the UI for image rec parameters for the target and range HSV values.
     */
    private EditText mTargetHEditText, mTargetSEditText, mTargetVEditText, mSizeEditText;
    private SeekBar mRangeHSeekBar, mRangeSSeekBar, mRangeVSeekBar, mSizeSeekBar;
    private TextView mRangeHTextView, mRangeSTextView, mRangeVTextView, mSizeTextView, mResultTextView;
    private Mat mRgba;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = findViewById(R.id.imagerec_debug_color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mTargetHEditText = findViewById(R.id.imagerec_debug_target_h_edittext);
        mTargetSEditText = findViewById(R.id.imagerec_debug_target_s_edittext);
        mTargetVEditText = findViewById(R.id.imagerec_debug_target_v_edittext);
        mSizeEditText = findViewById(R.id.imagerec_debug_size_edittext);
        mRangeHSeekBar = findViewById(R.id.imagerec_debug_range_h_seekbar);
        mRangeSSeekBar = findViewById(R.id.imagerec_debug_range_s_seekbar);
        mRangeVSeekBar = findViewById(R.id.imagerec_debug_range_v_seekbar);
        mSizeSeekBar = findViewById(R.id.imagerec_debug_size_seekbar);
        mRangeHTextView = findViewById(R.id.imagerec_debug_range_h_textview);
        mRangeSTextView = findViewById(R.id.imagerec_debug_range_s_textview);
        mRangeVTextView = findViewById(R.id.imagerec_debug_range_v_textview);
        mSizeTextView = findViewById(R.id.imagerec_debug_size_textview);
        mResultTextView = findViewById(R.id.imagerec_debug_reading_result);

        mRangeHSeekBar.setMax(255);
        mRangeSSeekBar.setMax(255);
        mSizeSeekBar.setMax(300);

        updateUiWidgetsFromParameters();


        // Code from http://developer.android.com/guide/topics/ui/controls/text.html#ActionEvent
        TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    updateImageParameters();
                    return true;
                }
                return false;
            }
        };
        mTargetHEditText.setOnEditorActionListener(editorActionListener);
        mTargetSEditText.setOnEditorActionListener(editorActionListener);
        mTargetVEditText.setOnEditorActionListener(editorActionListener);

        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateImageParameters();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        mRangeHSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        mRangeSSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        mRangeVSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        mSizeSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
//    onImageRecComplete(true, -0.123, 0.456, 0.789);
    }

    private void updateImageParameters() {
        // Part #1
// Grab values from the edit text boxes.
        String targetHText = mTargetHEditText.getText().toString();
        String targetSText = mTargetSEditText.getText().toString();
        String targetVText = mTargetVEditText.getText().toString();
        int size;
        try {
            int h = Integer.parseInt(targetHText);
            int s = Integer.parseInt(targetSText);
            int v = Integer.parseInt(targetVText);
            if (h < 0 || h > 255 || s < 0 || s > 255 || v < 0 || v > 255) {
                system_print("Invalid EditText box value!  Must be 0 to 255!");
                return;
            }
            mConeTargetH = h;
            mConeTargetS = s;
            mConeTargetV = v;
        } catch (NumberFormatException e) {
            system_print("Invalid EditText box!  Must be an int value!");
            return;
        }

        // Part #2
// Grab values from the sliders
        mConeRangeH = mRangeHSeekBar.getProgress();
        mConeRangeS = mRangeSSeekBar.getProgress();
        mConeRangeV = mRangeVSeekBar.getProgress();
        min_size_percentage = mSizeSeekBar.getProgress() / 10000.0;
        applyHsvTargetHsvRangeValues();
        mRangeHTextView.setText("" + mConeRangeH);
        mRangeSTextView.setText("" + mConeRangeS);
        mRangeVTextView.setText("" + mConeRangeV);
        mSizeTextView.setText("" + Math.round(min_size_percentage * 1000) / 10);
    }

    protected void applyHsvTargetHsvRangeValues() {
        if (mDetector != null) {
            // Setup the target color.
            Scalar targetColorHsv = new Scalar(255);
            targetColorHsv.val[0] = mConeTargetH;
            targetColorHsv.val[1] = mConeTargetS;
            targetColorHsv.val[2] = mConeTargetV;
            mDetector.setHsvColor(targetColorHsv);

            // Setup the range of values around the color to accept.
            Scalar colorRangeHsv = new Scalar(255);
            colorRangeHsv.val[0] = mConeRangeH;
            colorRangeHsv.val[1] = mConeRangeS;
            colorRangeHsv.val[2] = mConeRangeV;
            mDetector.setColorRadius(colorRangeHsv);
        }
    }

    private void updateUiWidgetsFromParameters() {
        mTargetHEditText.setText("" + mConeTargetH);
        mTargetSEditText.setText("" + mConeTargetS);
        mTargetVEditText.setText("" + mConeTargetV);
        mRangeHSeekBar.setProgress(mConeRangeH);
        mRangeSSeekBar.setProgress(mConeRangeS);
        mRangeVSeekBar.setProgress(mConeRangeV);
        mRangeHTextView.setText("" + mConeRangeH);
        mRangeSTextView.setText("" + mConeRangeS);
        mRangeVTextView.setText("" + mConeRangeV);
        mSizeTextView.setText("" + Math.round(min_size_percentage * 1000) / 10);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }


    /**
     * Displays the blob target info in the text views.
     */
    public void onImageRecComplete(boolean coneFound, double leftRightLocation, double topBottomLocation, double sizePercentage) {
        mConeFound = coneFound;
        mConeLeftRightLocation = leftRightLocation;
        mConeTopBottomLocation = topBottomLocation;
        mConeSize = sizePercentage;
        if (!mConeFound) {
            mResultTextView.setText("No Cone");
        } else {
            mResultTextView.setText("Left/Right: " + leftRightLocation + "\nTop/Bottom: " + topBottomLocation + "\nsize: " + sizePercentage);
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mDetector = new ColorBlobDetector();
        mRgba = new Mat(height, width, CvType.CV_8UC4);

        applyHsvTargetHsvRangeValues();

        // Record the screen size constants
        mCameraViewWidth = (double) width;
        mCameraViewHeight = (double) height;
        mCameraViewArea = mCameraViewWidth * mCameraViewHeight;
    }

    @Override
    public void onCameraViewStopped() {
        // Intentionally left blank.  Nothing needed here.
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mDetector.process(mRgba);

        List<MatOfPoint> contours = mDetector.getContours(); // For the outline
        Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

        // Now DONE: Add our stuff.
        // Find the center of the cone.
        double[] coneResult = new double[3];
        final boolean coneFound = findCone(contours, min_size_percentage, coneResult);
        final double leftRightLocation = coneResult[0]; // -1 for left ...  1 for right
        final double topBottomLocation = coneResult[1]; // 1 for top ... 0 for bottom
        final double sizePercentage = coneResult[2];
        if (coneFound) {
            // Draw a circle on the screen at the center.
            double coneCenterX = topBottomLocation * mCameraViewWidth;
            double coneCenterY = (leftRightLocation + 1.0) / 2.0 * mCameraViewHeight;
            Imgproc.circle(mRgba, new Point(coneCenterX, coneCenterY), 5, CONTOUR_COLOR, -1);
        }
        runOnUiThread(new Runnable() {
            public void run() {
                onImageRecComplete(coneFound, leftRightLocation, topBottomLocation, sizePercentage);
            }
        });
        return mRgba;
    }


    /**
     * Performs the math to find the leftRightLocation, topBottomLocation, and sizePercentage values.
     *
     * @param contours          List of matrices containing points that match the target color.
     * @param minSizePercentage Minimum size percentage needed to call a blob a match. 0.005 would be 0.5%
     * @param coneResult        Array that will be populated with the results of this math.
     * @return True if a cone is found, False if no cone is found.
     */
    public boolean findCone(List<MatOfPoint> contours, double minSizePercentage, double[] coneResult) {
        // Step #0: Determine if any contour regions were found that match the target color criteria.
        if (contours.size() == 0) {
            return false; // No contours found.
        }

        // Step #1: Use only the largest contour. Other contours (potential other cones) will be ignored.
        MatOfPoint largestContour = contours.get(0);
        double largestArea = Imgproc.contourArea(largestContour);
        for (int i = 1; i < contours.size(); ++i) {
            MatOfPoint currentContour = contours.get(0);
            double currentArea = Imgproc.contourArea(currentContour);
            if (currentArea > largestArea) {
                largestArea = currentArea;
                largestContour = currentContour;
            }
        }

        // Step #2: Determine if this target meets the size requirement.
        double sizePercentage = largestArea / mCameraViewArea;
        if (sizePercentage < minSizePercentage) {
            return false; // No cone found meeting the size requirement.
        }

        // Step #3: Calculate the center of the blob.
//    Moments moments = Imgproc.moments(largestContour, false);
        // yep, the line above fails.  Comment out the line above and uncomment the line below.  For more info visit this page https://github.com/Itseez/opencv/issues/5017
        Moments moments = contourMoments(largestContour);
        double aveX = moments.get_m10() / moments.get_m00();
        double aveY = moments.get_m01() / moments.get_m00();

        // Step #4: Convert the X and Y values into leftRight and topBottom values.
        // X is 0 on the left (which is really the bottom) divide by width to scale the topBottomLocation
        // Y is 0 on the top of the view (object is left of the robot) divide by height to scale
        double leftRightLocation = aveY / (mCameraViewHeight / 2.0) - 1.0;
        double topBottomLocation = aveX / mCameraViewWidth;

        // Step #5: Populate the results array.
        coneResult[0] = leftRightLocation;
        coneResult[1] = topBottomLocation;
        coneResult[2] = sizePercentage;
        return true;
    }

    public Moments contourMoments(MatOfPoint contour) {
        Moments m = new Moments();
        int lpt = contour.checkVector(2);
        boolean is_float = true;//(contour.depth() == CvType.CV_32F);
        Point[] ptsi = contour.toArray();
//PointF[] ptsf = contour.toArray();

        //CV_Assert( contour.depth() == CV_32S || contour.depth() == CV_32F );

        if (lpt == 0)
            return m;

        double a00 = 0, a10 = 0, a01 = 0, a20 = 0, a11 = 0, a02 = 0, a30 = 0, a21 = 0, a12 = 0, a03 = 0;
        double xi, yi, xi2, yi2, xi_1, yi_1, xi_12, yi_12, dxy, xii_1, yii_1;


        {
            xi_1 = ptsi[lpt - 1].x;
            yi_1 = ptsi[lpt - 1].y;
        }

        xi_12 = xi_1 * xi_1;
        yi_12 = yi_1 * yi_1;

        for (int i = 0; i < lpt; i++) {

            {
                xi = ptsi[i].x;
                yi = ptsi[i].y;
            }

            xi2 = xi * xi;
            yi2 = yi * yi;
            dxy = xi_1 * yi - xi * yi_1;
            xii_1 = xi_1 + xi;
            yii_1 = yi_1 + yi;

            a00 += dxy;
            a10 += dxy * xii_1;
            a01 += dxy * yii_1;
            a20 += dxy * (xi_1 * xii_1 + xi2);
            a11 += dxy * (xi_1 * (yii_1 + yi_1) + xi * (yii_1 + yi));
            a02 += dxy * (yi_1 * yii_1 + yi2);
            a30 += dxy * xii_1 * (xi_12 + xi2);
            a03 += dxy * yii_1 * (yi_12 + yi2);
            a21 += dxy * (xi_12 * (3 * yi_1 + yi) + 2 * xi * xi_1 * yii_1 +
                    xi2 * (yi_1 + 3 * yi));
            a12 += dxy * (yi_12 * (3 * xi_1 + xi) + 2 * yi * yi_1 * xii_1 +
                    yi2 * (xi_1 + 3 * xi));
            xi_1 = xi;
            yi_1 = yi;
            xi_12 = xi2;
            yi_12 = yi2;
        }
        float FLT_EPSILON = 1.19209e-07f;
        if (Math.abs(a00) > FLT_EPSILON) {
            double db1_2, db1_6, db1_12, db1_24, db1_20, db1_60;

            if (a00 > 0) {
                db1_2 = 0.5;
                db1_6 = 0.16666666666666666666666666666667;
                db1_12 = 0.083333333333333333333333333333333;
                db1_24 = 0.041666666666666666666666666666667;
                db1_20 = 0.05;
                db1_60 = 0.016666666666666666666666666666667;
            } else {
                db1_2 = -0.5;
                db1_6 = -0.16666666666666666666666666666667;
                db1_12 = -0.083333333333333333333333333333333;
                db1_24 = -0.041666666666666666666666666666667;
                db1_20 = -0.05;
                db1_60 = -0.016666666666666666666666666666667;
            }

            // spatial moments
            m.m00 = a00 * db1_2;
            m.m10 = a10 * db1_6;
            m.m01 = a01 * db1_6;
            m.m20 = a20 * db1_12;
            m.m11 = a11 * db1_24;
            m.m02 = a02 * db1_12;
            m.m30 = a30 * db1_20;
            m.m21 = a21 * db1_60;
            m.m12 = a12 * db1_60;
            m.m03 = a03 * db1_20;

            m.completeState();
        }
        return m;
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (mViewControl.getDisplayedChild() != 3) {
            system_print("Don't listen for touch events if the camera is not visible.");
            return false;
        }
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        system_print("Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        Scalar touchedHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < touchedHsv.val.length; i++)
            touchedHsv.val[i] /= pointCount;

        //Toast.makeText(this, "HSV = " + touchedHsv, Toast.LENGTH_LONG).show();

        // Set and save the color selected.  This is where the real work happens
        mConeTargetH = (int) touchedHsv.val[0];
        mConeTargetS = (int) touchedHsv.val[1];
        mConeTargetV = (int) touchedHsv.val[2];
        updateUiWidgetsFromParameters();
        applyHsvTargetHsvRangeValues();

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

}
