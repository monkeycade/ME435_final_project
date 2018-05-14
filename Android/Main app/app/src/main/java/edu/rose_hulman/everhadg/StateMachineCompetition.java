package edu.rose_hulman.everhadg;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.List;

import edu.rose_hulman.jins.final_project_main.MainCommandBin;
import edu.rose_hulman.jins.final_project_main.R;
import edu.rose_hulman.me435Library.FieldGps;
import edu.rose_hulman.me435Library.FieldGpsListener;
import edu.rose_hulman.me435Library.FieldOrientation;
import edu.rose_hulman.me435Library.FieldOrientationListener;
import edu.rose_hulman.me435Library.NavUtils;

/**
 * Currently some of the buttons don't have listeners and the texts aren't updated
 */

/**
 * Ball Locations:
 * You possess either a (red or green), (blue or yellow), (white or black)
 *
 * For Red team locations are:
 *      Start: (15, 0)
 *      Green: (90, 50)
 *      Red:   (90,-50)
 *      White: (165+, Y)
 *      Blue:  (240, 50)
 *      Yellow:(240, -50)
 *      Home:  (0, 0);
 * For Blue team locations are:
 *      Start: (15, 0)
 *      Yellow: (90, 50)
 *      Blue:   (90,-50)
 *      White: (165+, Y)
 *      Red:  (240, 50)
 *      Green:(240, -50)
 *      Home:  (0, 0);
 *
 */

public class StateMachineCompetition extends MainCommandBin implements FieldGpsListener, FieldOrientationListener, CameraBridgeViewBase.CvCameraViewListener2{

    public enum State {
        READY_FOR_MISSION,INITIAL_STRAIGHT,NEAR_BALL_MISSION,FAR_BALL_MISSION,HOME_CONE_MISSION,WAITING_FOR_PICKUP,SEEKING_HOME;
    }

    public enum SubState {
        GPS_SEEKING,IMAGE_REC_SEEKING,OPTIONAL_SCRIPT, INACTIVE;
    }

    private State mState;
    private SubState mSubState;


    public static final int LOWEST_DESIRABLE_DUTY_CYCLE = 150;
    public static final int LEFT_PWM_VALUE_FOR_STRAIGHT = 255;
    public static final int RIGHT_PWM_VALUE_FOR_STRAIGHT = 255;

    /**
     * Target color. An inside cone has an orange hue around 5 - 15, full saturation and value. (change as needed when outside)
     */
    private static final int TARGET_COLOR_HUE = 10;
    private static final int TARGET_COLOR_SATURATION = 255;
    private static final int TARGET_COLOR_VALUE = 255;

    /**
     * Range of acceptable colors. (change as needed)
     */
    private static final int TARGET_COLOR_HUE_RANGE = 25;
    private static final int TARGET_COLOR_SATURATION_RANGE = 50;
    private static final int TARGET_COLOR_VALUE_RANGE = 50;

    /**
     * Minimum size needed to consider the target a cone. (change as needed)
     */
    private static final double MIN_SIZE_PERCENTAGE = 0.001;



    private TextView mCurrentStateTextView,mSubStateTextView,mGPSTextView,mTargetXYTextView,mTargetHeadingTextView,mTurnAmountTextView;
    private ToggleButton mTeamToggle;
    private CameraBridgeViewBase mOpenCvCameraView;

    /**
     * Variables for ball locations:
     *      mNear and mFar values should be set based on the balls that the robot has
     */
    public int mYellowX = 240, mYellowY = -50, mGreenX = 90, mGreenY = 50, mRedX = 90, mRedY = -50,mBlueX = 240, mBlueY = 50;
    public int mNearX, mNearY, mFarX, mFarY;


    public boolean mIsRedTeam, mInSeekRange = false;
    public boolean mHasRed = true, mHasBlue = true, mHasWhite = true, mWithinRange = false;

    private long mStateStartTime;
    private long mSubStateStartTime;

    private int mGpsCounter = 0;
    private double mTargetHeading,mLeftTurnAmount,mRightTurnAmount, mXTarget,mYTarget,mCurrentHeading;
    private double mLeftRightCone = 0;
    private double mConeSize;
    private boolean mConeFound = false;

    private int mLeftDutyCycle,mRightDutyCycle;

    private double mHeadingError = 0;
    private double mLastHeadingError = 0;
    private double mSumHeadingError = 0;

    //private double mDetectionThresh = 0.01;
    private double mImageStopThresh = 0.07;

    /**
     * Screen size variables.
     */
    private double mCameraViewWidth;
    private double mCameraViewHeight;
    private double mCameraViewArea;

    /**
     * References to the UI widgets used in this demo app.
     */
    private TextView mLeftRightLocationTextView, mTopBottomLocationTextView, mSizePercentageTextView;

    /**
     * Constants and variables used by OpenCV4Android. Don't mess with these. ;)
     */
    private ColorBlobDetector mDetector;
    private Scalar CONTOUR_COLOR = new Scalar(0, 0, 255, 255);
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    protected void upDateTeamGoals(){
        if(mIsRedTeam){
            mYellowX = 240; mYellowY = -50;
            mGreenX = 90;   mGreenY = 50;
            mRedX = 90;     mRedY = -50;
            mBlueX = 240;   mBlueY = 50;
        } else {
            mYellowX = 90;  mYellowY = 50;
            mGreenX = 240;  mGreenY = -50;
            mRedX = 240;    mRedY = 50;
            mBlueX = 90;    mBlueY = -50;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFieldOrientation = new FieldOrientation(this);
        mFieldGps = new FieldGps(this);

        setState(State.READY_FOR_MISSION);
        setSubState(SubState.INACTIVE);

        mOpenCvCameraView = findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //ToggleButton teamToggle = findViewById(R.id.teamToggleButton);
        //teamToggle.setOnCheckedChangeListener(CompoundButton buttonView,);


        mCurrentStateTextView = findViewById(R.id.highStateLabel);
        mSubStateTextView = findViewById(R.id.subStateLabel);
        mGPSTextView = findViewById(R.id.gpsLabel);
        mTargetXYTextView = findViewById(R.id.targetXYLabel);
        mTargetHeadingTextView = findViewById(R.id.headingLabel);
        mTurnAmountTextView = findViewById(R.id.turnAmountLabel);

        setState(State.READY_FOR_MISSION);
        setSubState(SubState.INACTIVE);
        mFieldOrientation = new FieldOrientation(this);
        mFieldGps = new FieldGps(this);
        mTeamToggle = findViewById(R.id.teamToggleButton);
        mTeamToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mIsRedTeam = false;
                    upDateTeamGoals();
                    mTeamToggle.setBackgroundColor(Color.BLUE);
                } else {
                    mIsRedTeam = true;
                    upDateTeamGoals();
                    mTeamToggle.setBackgroundColor(Color.RED);
                }
            }
        });
        mTeamToggle.setChecked(false);
        mCurrentGpsHeading = 0;
        mCurrentGpsX = 0;
        mCurrentGpsY = 0;
        mGpsCounter = 0;
        mCurrentHeading = 0;


    }

    @Override
    protected void onStart() {
        super.onStart();
        //GPS stuff is handled at Robot Activity
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFieldOrientation.unregisterListener();
        mFieldGps.removeUpdates();
        //GPS stuff is handled at Robot Activity
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


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //handled at Robot Activity
    }

    @Override
    public void onLocationChanged(double x, double y, double heading, Location location) {
        mGpsCounter++;
        mCurrentGpsX = x;
        mCurrentGpsY = y;
        mCurrentGpsHeading = heading;
        String gpsInfo = getString(R.string.xy_format, x, y);
        if (heading <= 180.0 && heading > -180.0) {
            gpsInfo += " " + getString(R.string.degrees_format, heading);
            //mSensorOrientationTextView.setText("" + (int)heading);
            mCurrentGpsHeading = heading;
            mCurrentHeading = heading;
            mFieldOrientation.setCurrentFieldHeading(heading);
        } else {
            gpsInfo += "?̊";
            //mSensorOrientationTextView.setText("---");
        }
        gpsInfo += "   " + mGpsCounter;
        mGPSTextView.setText(gpsInfo);
        if(distToTarget()<30){
            mInSeekRange = true;
        } else {
            mInSeekRange = false;
        }
    }

    @Override
    public void onSensorChanged(double fieldHeading, float[] orientationValues){
        mCurrentSensorHeading = fieldHeading;
        mCurrentHeading = fieldHeading;
    }

    private void updateTimeDisplay(){
        if(mState == State.READY_FOR_MISSION){
            mCurrentStateTextView.setText(mState.name() + "");
            mTargetHeadingTextView.setText("---");
            mTargetXYTextView.setText("---");
            mTurnAmountTextView.setText("---");
        } else {
            mCurrentStateTextView.setText(mState.name() + " " + getStateTimeMS() / 1000);
            mTargetHeadingTextView.setText("" + mTargetHeading);
        }
        if(mSubState == SubState.INACTIVE){
            mSubStateTextView.setText("---");
        } else {
            mSubStateTextView.setText(mSubState.name() + " " + getSubStateTimeMS() / 1000);
        }
        if(mSubState != SubState.GPS_SEEKING){
            mTargetHeadingTextView.setText("---");
            mTurnAmountTextView.setText("---");
        }
    }

    @Override
    public void loop(){
        super.loop();
        updateTimeDisplay();
        switch (mState){
            case INITIAL_STRAIGHT:
                if (getStateTimeMS() > 4000) {
                    setState(State.NEAR_BALL_MISSION);
                }
                break;
            case NEAR_BALL_MISSION:
                mTargetXYTextView.setText("" + getString(R.string.xy_format, mNearX, mNearY));
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(mNearX,mNearY);
                        if (NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,mXTarget,mYTarget)) {
                            mTurnAmountTextView.setText("Left " + getString(R.string.degrees_format, mLeftTurnAmount));
                        } else {
                            mTurnAmountTextView.setText("Right " + getString(R.string.degrees_format, mRightTurnAmount));
                        }
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if(mInSeekRange && seeSomething()){
                            setSubState(SubState.IMAGE_REC_SEEKING);
                        }
                        if (mWithinRange) {
                            mWithinRange = false;
                            setSubState(SubState.OPTIONAL_SCRIPT);
                        }
                        break;
                    case IMAGE_REC_SEEKING:
                        seekImage();
                        //TODO fix it
                        if(mWithinRange){
                            mWithinRange = false;
                            setSubState(SubState.OPTIONAL_SCRIPT);
                        }
                    case OPTIONAL_SCRIPT:
                        if (getSubStateTimeMS() > 4000) {
                            setState(State.FAR_BALL_MISSION);
                        }
                    default:
                        break;
                }
                break;
            case FAR_BALL_MISSION:
                mTargetXYTextView.setText("" + getString(R.string.xy_format, mFarX, mFarY));
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(mFarX,mFarY);
                        if (NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,mXTarget,mYTarget)) {
                            mTurnAmountTextView.setText("Left " + getString(R.string.degrees_format, mLeftTurnAmount));
                        } else {
                            mTurnAmountTextView.setText("Right " + getString(R.string.degrees_format, mLeftTurnAmount));
                        }
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if(mConeFound && mWithinRange){
                            if(mConeSize>MIN_SIZE_PERCENTAGE){
                                setSubState(SubState.IMAGE_REC_SEEKING);
                            }
                        }
                        if (mWithinRange) {
                            mWithinRange = false;
                            setSubState(SubState.OPTIONAL_SCRIPT);
                        }
                        break;
                    case IMAGE_REC_SEEKING:
                        seekImage();
                        if(mWithinRange){
                            mWithinRange = false;
                            setSubState(SubState.OPTIONAL_SCRIPT);
                        }
                        break;
                    case OPTIONAL_SCRIPT:
                        if (getSubStateTimeMS() > 4000) {
                            //mBlueBallTextView.setText("---");
                            if (getSubStateTimeMS() > 8000){
                                setState(State.HOME_CONE_MISSION);
                                //mWhiteBallTextView.setText("---");
                            }
                        }
                    default:
                        break;
                }
                break;
            case HOME_CONE_MISSION:
                mTargetXYTextView.setText("" + getString(R.string.xy_format, 0, 0));
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(0,0);
                        if (NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,mXTarget,mYTarget)) {
                            mTurnAmountTextView.setText("Left " + getString(R.string.degrees_format, mLeftTurnAmount));
                        } else {
                            mTurnAmountTextView.setText("Right " + getString(R.string.degrees_format, mLeftTurnAmount));
                        }
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if(mConeFound && mWithinRange){
                            if(mConeSize>MIN_SIZE_PERCENTAGE){
                                setSubState(SubState.IMAGE_REC_SEEKING);
                            }
                        }
                        if (getSubStateTimeMS() > 15000) {
                            setSubState(SubState.OPTIONAL_SCRIPT);
                        }
                        break;
                    case OPTIONAL_SCRIPT:
                        if (getSubStateTimeMS() > 1000) {
                            setState(State.WAITING_FOR_PICKUP);
                        }
                    default:
                        break;
                }
                break;
            case WAITING_FOR_PICKUP:
                if (getStateTimeMS() > 4000) {
                    setState(State.SEEKING_HOME);
                }
                break;
            case SEEKING_HOME:
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(0,0);
                        if (NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,mXTarget,mYTarget)) {
                            mTurnAmountTextView.setText("Left " + getString(R.string.degrees_format, mLeftTurnAmount));
                        } else {
                            mTurnAmountTextView.setText("Right " + getString(R.string.degrees_format, mLeftTurnAmount));
                        }
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if (getSubStateTimeMS() > 5000) {
                            setState(State.WAITING_FOR_PICKUP);
                        }
                        break;
                    case OPTIONAL_SCRIPT:
                        if (getSubStateTimeMS() > 1000) {
                            setState(State.WAITING_FOR_PICKUP);
                        }
                    default:
                        break;
                }
            default:
                break;
        }
    }

    private long getStateTimeMS() {
        return System.currentTimeMillis() - mStateStartTime;
    }

    private long getSubStateTimeMS() {
        return System.currentTimeMillis() - mSubStateStartTime;
    }

    private void setState(State newState) {

        // Write down (in a variable) the current time
        mStateStartTime = System.currentTimeMillis();

        switch (newState) {
            case READY_FOR_MISSION:
                setSubState(SubState.INACTIVE);
                break;
            case INITIAL_STRAIGHT:
                break;
            case NEAR_BALL_MISSION:
                setSubState(SubState.GPS_SEEKING);
                break;
            case FAR_BALL_MISSION:
                setSubState(SubState.GPS_SEEKING);
                break;
            case HOME_CONE_MISSION:
                setSubState(SubState.GPS_SEEKING);
                break;
            case WAITING_FOR_PICKUP:
                setSubState(SubState.INACTIVE);
                break;
            case SEEKING_HOME:
                setSubState(SubState.GPS_SEEKING);
                break;
        }
        mState = newState;
    }

    private void setSubState(SubState newSubState){
        // Write down the current time
        mSubStateStartTime = System.currentTimeMillis();

        switch (newSubState) {
            case GPS_SEEKING:
                mSumHeadingError = 0;
                mLastHeadingError = 0;
                mHeadingError = 0;
                break;
            case IMAGE_REC_SEEKING:
                mSumHeadingError = 0;
                mLastHeadingError = 0;
                mHeadingError = 0;
                break;
            case OPTIONAL_SCRIPT:
                break;
            case INACTIVE:
                break;
        }
        mSubState = newSubState;
    }

    private boolean seeSomething(){
        // TODO implement this
        return false;
    }

    private boolean closeEnough(){
        return false;
    }

    private void seekImage(){
        if(mConeFound) {
            double Target = mLeftRightCone;
            int slowDown = 40;
            mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT - slowDown;
            mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT - slowDown;
            double p_gain = 5;
            double d_gain = 1;
            double i_gain = .5;
            if (mConeSize >= mImageStopThresh) {
                mWithinRange = true;
                setSubState(SubState.OPTIONAL_SCRIPT);
                sendCommand("WHEEL SPEED BRAKE 0 BRAKE 0");
            }
            if (Target < 0) {
                mLastHeadingError = mHeadingError;
                mHeadingError = Math.abs(Target);
                mSumHeadingError += mHeadingError;
                mLeftTurnAmount = p_gain * mHeadingError + d_gain * (mHeadingError - mLastHeadingError) + (i_gain * mSumHeadingError / getSubStateTimeMS());
                mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT - slowDown - (int) mLeftTurnAmount; // Using a VERY simple plan. :)
                if (mLeftDutyCycle > LEFT_PWM_VALUE_FOR_STRAIGHT) {
                    mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT - slowDown;
                }
                mLeftDutyCycle = Math.max(mLeftDutyCycle, LOWEST_DESIRABLE_DUTY_CYCLE);
            } else {
                mLastHeadingError = mHeadingError;
                mHeadingError = Math.abs(Target);
                mSumHeadingError += mHeadingError;

                mRightTurnAmount = p_gain * mHeadingError + d_gain * (mHeadingError - mLastHeadingError) + (i_gain * mSumHeadingError / getSubStateTimeMS());
                mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT - slowDown - (int) mRightTurnAmount; // Could also scale it.
                if (mRightDutyCycle > RIGHT_PWM_VALUE_FOR_STRAIGHT) {
                    mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT - slowDown;
                }

                mRightDutyCycle = Math.max(mRightDutyCycle, LOWEST_DESIRABLE_DUTY_CYCLE);
            }

            sendCommand("WHEEL SPEED FORWARD " + mRightDutyCycle + " FORWARD " + mLeftDutyCycle);
        }
    }

    private void seekTargetAt(double xTarget, double yTarget) {
        mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT;
        mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT;
        mTargetHeading = NavUtils.getTargetHeading(mCurrentGpsX, mCurrentGpsY, xTarget, yTarget);
        mLeftTurnAmount = NavUtils.getLeftTurnHeadingDelta(mCurrentHeading, mTargetHeading);
        mRightTurnAmount = NavUtils.getRightTurnHeadingDelta(mCurrentHeading, mTargetHeading);

        mXTarget = xTarget;
        mYTarget = yTarget;

        /*if(((mXTarget-mCurrentGpsX)*(mXTarget-mCurrentGpsX)+(mYTarget-mCurrentGpsY)*(mYTarget-mCurrentGpsY))<(5*5)){
            //Add option for not seeing any cones
            if(mSubState == SubState.GPS_SEEKING) {
                setSubState(SubState.IMAGE_REC_SEEKING);
                mWithinRange = true;
            }
            return;
        }*/

        double p_gain = 1;
        double d_gain = .1;
        double i_gain = .05;


        if(NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentHeading,xTarget,yTarget)){//if (mLeftTurnAmount < mRightTurnAmount) {
            mLastHeadingError = mHeadingError;
            mHeadingError = mLeftTurnAmount;
            mSumHeadingError += mHeadingError;

            mLeftTurnAmount = p_gain*mHeadingError + d_gain*(mHeadingError-mLastHeadingError) + (i_gain*mSumHeadingError/getSubStateTimeMS());
            mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT - (int)mLeftTurnAmount; // Using a VERY simple plan. :)
            if(mLeftDutyCycle>LEFT_PWM_VALUE_FOR_STRAIGHT){
                mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT;
            }
            mLeftDutyCycle = Math.max(mLeftDutyCycle, LOWEST_DESIRABLE_DUTY_CYCLE);
        } else {
            mLastHeadingError = mHeadingError;
            mHeadingError = mRightTurnAmount;
            mSumHeadingError += mHeadingError;

            mRightTurnAmount = p_gain*mHeadingError + d_gain*(mHeadingError-mLastHeadingError) + (i_gain*mSumHeadingError/getSubStateTimeMS());
            mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT - (int)mRightTurnAmount; // Could also scale it.
            if(mRightDutyCycle>RIGHT_PWM_VALUE_FOR_STRAIGHT){
                mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT;
            }

            mRightDutyCycle = Math.max(mRightDutyCycle, LOWEST_DESIRABLE_DUTY_CYCLE);
        }
        //LOOP_INTERVAL_MS

        sendCommand("WHEEL SPEED FORWARD " + mRightDutyCycle + " FORWARD " + mLeftDutyCycle);
    }

    public double distToTarget(){
        double xDist = mXTarget - mCurrentGpsX;
        double yDist = mYTarget - mCurrentGpsY;
        double number = Math.sqrt((xDist*xDist)+(yDist*yDist));
        return number;
    }


    public void reset(View view) {
        setState(State.READY_FOR_MISSION);
        mGpsCounter = 0;
        mCurrentGpsHeading = 0;
        mCurrentGpsX = 0;
        mCurrentGpsY = 0;
        mGpsCounter = 0;
        mTargetHeading = 0;
        mWithinRange = false;
        mConeFound = false;
        mHeadingError = 0;
        mLastHeadingError = 0;
        mSumHeadingError = 0;
        mTeamToggle.setChecked(false);
    }

    public void go(View view) {
        if (mState == State.READY_FOR_MISSION) {
            setState(State.INITIAL_STRAIGHT);
            //mTargetXYTextView.setText("(90, -50)");
        }
    }

    public void notSeen(View view) {
        if (mSubState == SubState.IMAGE_REC_SEEKING) {
            setSubState(SubState.GPS_SEEKING);
        }
    }

    public void seenSmall(View view) {
        if (mSubState == SubState.GPS_SEEKING) {
            setSubState(SubState.IMAGE_REC_SEEKING);
        }
    }

    public void seenBig(View view) {
        if (mSubState == SubState.IMAGE_REC_SEEKING) {
            setSubState(SubState.OPTIONAL_SCRIPT);
        }
    }

    public void missionComplete(View view) {
        if (mState == State.WAITING_FOR_PICKUP) {
            setState(State.READY_FOR_MISSION);
        }
    }

    public void handleSetOrigin(View view){
        mFieldGps.setCurrentLocationAsOrigin();
    }
    public void handleSetXAxis(View view){
        mFieldGps.setCurrentLocationAsLocationOnXAxis();
    }

    /**
     * Displays the blob target info in the text views.
     */
    public void onImageRecComplete(boolean coneFound, double leftRightLocation, double topBottomLocation, double sizePercentage) {
        mConeFound = coneFound;
        if (coneFound) {
            mLeftRightCone = leftRightLocation;
            mConeSize = sizePercentage;
//            mLeftRightLocationTextView.setText(String.format("%.3f", leftRightLocation));
//            mTopBottomLocationTextView.setText(String.format("%.3f", topBottomLocation));
//            mSizePercentageTextView.setText(String.format("%.5f", sizePercentage));
        } else {
            mLeftRightCone = 0;
            mConeSize = 0;
//            mLeftRightLocationTextView.setText("---");
//            mTopBottomLocationTextView.setText("---");
//            mSizePercentageTextView.setText("---");
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mDetector = new ColorBlobDetector();
        Scalar targetColorHsv = new Scalar(255);
        targetColorHsv.val[0] = TARGET_COLOR_HUE;
        targetColorHsv.val[1] = TARGET_COLOR_SATURATION;
        targetColorHsv.val[2] = TARGET_COLOR_VALUE;
        mDetector.setHsvColor(targetColorHsv);

        // Setup the range of values around the color to accept.
        Scalar colorRangeHsv = new Scalar(255);
        colorRangeHsv.val[0] = TARGET_COLOR_HUE_RANGE;
        colorRangeHsv.val[1] = TARGET_COLOR_SATURATION_RANGE;
        colorRangeHsv.val[2] = TARGET_COLOR_VALUE_RANGE;
        mDetector.setColorRadius(colorRangeHsv);

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
        Mat rgba = inputFrame.rgba();
        mDetector.process(rgba);

        List<MatOfPoint> contours = mDetector.getContours(); // For the outline
        Imgproc.drawContours(rgba, contours, -1, CONTOUR_COLOR);
        double[] coneResult = new double[3];
        final boolean coneFound = findCone(contours, MIN_SIZE_PERCENTAGE, coneResult);
        final double leftRightLocation = coneResult[0]; // -1 for left ...  1 for right
        final double topBottomLocation = coneResult[1]; // 1 for top ... 0 for bottom
        final double sizePercentage = coneResult[2];
        if (coneFound) {
            // Draw a circle on the screen at the center.
            double coneCenterX = topBottomLocation * mCameraViewWidth;
            double coneCenterY = (leftRightLocation + 1.0) / 2.0 * mCameraViewHeight;
            Imgproc.circle(rgba, new Point(coneCenterX, coneCenterY), 5, CONTOUR_COLOR, -1);
        }
        runOnUiThread(new Runnable() {
            public void run() {
                onImageRecComplete(coneFound, leftRightLocation, topBottomLocation, sizePercentage);
            }
        });


        return rgba;
    }

    /**
     * Performs the math to find the leftRightLocation, topBottomLocation, and sizePercentage values.
     *
     * @param contours          List of matrices containing points that match the target color.
     * @param minSizePercentage Minimum size percentage needed to call a blob a match. 0.005 would be 0.5%
     * @param coneResult        Array that will be populated with the results of this math.
     * @return True if a cone is found, False if no cone is found.
     */
    private boolean findCone(List<MatOfPoint> contours, double minSizePercentage, double[] coneResult) {
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
//        Moments moments = Imgproc.moments(largestContour, false);
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

    public Moments contourMoments( MatOfPoint contour )
    {
        Moments m = new Moments();
        int lpt = contour.checkVector(2);
        boolean is_float = true;//(contour.depth() == CvType.CV_32F);
        Point[] ptsi = contour.toArray();
//PointF[] ptsf = contour.toArray();

        //CV_Assert( contour.depth() == CV_32S || contour.depth() == CV_32F );

        if( lpt == 0 )
            return m;

        double a00 = 0, a10 = 0, a01 = 0, a20 = 0, a11 = 0, a02 = 0, a30 = 0, a21 = 0, a12 = 0, a03 = 0;
        double xi, yi, xi2, yi2, xi_1, yi_1, xi_12, yi_12, dxy, xii_1, yii_1;


        {
            xi_1 = ptsi[lpt-1].x;
            yi_1 = ptsi[lpt-1].y;
        }

        xi_12 = xi_1 * xi_1;
        yi_12 = yi_1 * yi_1;

        for( int i = 0; i < lpt; i++ )
        {

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
        if( Math.abs(a00) > FLT_EPSILON )
        {
            double db1_2, db1_6, db1_12, db1_24, db1_20, db1_60;

            if( a00 > 0 )
            {
                db1_2 = 0.5;
                db1_6 = 0.16666666666666666666666666666667;
                db1_12 = 0.083333333333333333333333333333333;
                db1_24 = 0.041666666666666666666666666666667;
                db1_20 = 0.05;
                db1_60 = 0.016666666666666666666666666666667;
            }
            else
            {
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
}
