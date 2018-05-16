package edu.rose_hulman.everhadg;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.opencv.android.CameraBridgeViewBase;

import edu.rose_hulman.jins.ball_color_detector.BallColorDetector;
import edu.rose_hulman.jins.ball_color_detector.BallColorTrainner;
import edu.rose_hulman.jins.final_project_main.R;
import edu.rose_hulman.jins.script_runing.Scripts;
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
 * <p>
 * For Red team locations are:
 * Start: (15, 0)
 * Green: (90, 50)
 * Red:   (90,-50)
 * White: (165+, Y)
 * Blue:  (240, 50)
 * Yellow:(240, -50)
 * Home:  (0, 0);
 * For Blue team locations are:
 * Start: (15, 0)
 * Yellow: (90, 50)
 * Blue:   (90,-50)
 * White: (165+, Y)
 * Red:  (240, 50)
 * Green:(240, -50)
 * Home:  (0, 0);
 */

public class StateMachineCompetition extends BallColorTrainner implements FieldGpsListener, FieldOrientationListener, CameraBridgeViewBase.CvCameraViewListener2 {

    public enum State {
        READY_FOR_MISSION, INITIAL_STRAIGHT, NEAR_BALL_MISSION, FAR_BALL_MISSION, HOME_CONE_MISSION, WAITING_FOR_PICKUP, SEEKING_HOME;
    }

    public enum SubState {
        GPS_SEEKING, IMAGE_REC_SEEKING, ACTION_SCRIPT, INACTIVE;
    }


    private State mState;
    private SubState mSubState;


    public static final int LOWEST_DESIRABLE_DUTY_CYCLE = 150;
    public static final int LEFT_PWM_VALUE_FOR_STRAIGHT = 255;
    public static final int RIGHT_PWM_VALUE_FOR_STRAIGHT = 255;


    private TextView mCurrentStateTextView, mSubStateTextView, mGPSTextView, mTargetXYTextView, mTargetHeadingTextView, mTurnAmountTextView;
    private ToggleButton mTeamToggle;
    private CameraBridgeViewBase mOpenCvCameraView;

    /**
     * Variables for ball locations:
     * mNear and mFar values should be set based on the balls that the robot has
     */
    public int mYellowX = 240, mYellowY = -50, mGreenX = 90, mGreenY = 50, mRedX = 90, mRedY = -50, mBlueX = 240, mBlueY = 50;
    public int mNearX, mNearY, mFarX, mFarY;


    public boolean mIsRedTeam;
    public boolean mHasRed = true, mHasBlue = true, mHasWhite = true, mWithinRange = false, mInSeekRange = false;

    private long mStateStartTime;
    private long mSubStateStartTime;

    private int mGpsCounter = 0;
    private double mTargetHeading, mLeftTurnAmount, mRightTurnAmount, mXTarget, mYTarget, mCurrentHeading;
    private double mLeftRightCone = 0;
    private double mConeSize;
    private boolean mConeFound = false;
    private int mSeekRangebig = 100;
    private int mSeekRangesmall = 20;

    private int mLeftDutyCycle, mRightDutyCycle;

    private double mHeadingError = 0;
    private double mLastHeadingError = 0;
    private double mSumHeadingErrorsum = 0;
    private int mSumHeadingErrorcount = 0;


    protected void upDateTeamGoals() {
        if (mIsRedTeam) {
            mYellowX = 240;
            mYellowY = -50;
            mGreenX = 90;
            mGreenY = 50;
            mRedX = 90;
            mRedY = -50;
            mBlueX = 240;
            mBlueY = 50;
        } else {
            mYellowX = 90;
            mYellowY = 50;
            mGreenX = 240;
            mGreenY = -50;
            mRedX = 240;
            mRedY = 50;
            mBlueX = 90;
            mBlueY = -50;
        }
    }

    Scripts mScript;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFieldOrientation = new FieldOrientation(this);
        mFieldGps = new FieldGps(this, mStorage);

        setState(State.READY_FOR_MISSION);
        setSubState(SubState.INACTIVE);


        //ToggleButton teamToggle = findViewById(R.id.teamToggleButton);
        //teamToggle.setOnCheckedChangeListener(CompoundButton buttonView,);

        mScript = new Scripts(this);

        mCurrentStateTextView = findViewById(R.id.highStateLabel);
        mSubStateTextView = findViewById(R.id.subStateLabel);
        mGPSTextView = findViewById(R.id.gpsLabel);
        mTargetXYTextView = findViewById(R.id.targetXYLabel);
        mTargetHeadingTextView = findViewById(R.id.headingLabel);
        mTurnAmountTextView = findViewById(R.id.turnAmountLabel);

        mTeamToggle = findViewById(R.id.teamToggleButton);
        mTeamToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    system_print("set we are blue");
                    mIsRedTeam = false;
                    upDateTeamGoals();
                    mTeamToggle.setBackgroundColor(Color.BLUE);
                } else {
                    system_print("set we are red");
                    mIsRedTeam = true;
                    upDateTeamGoals();
                    mTeamToggle.setBackgroundColor(Color.RED);
                }
            }
        });
        mTeamToggle.setChecked(true);
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
        if (distToTarget() < mSeekRangesmall) {
            mInSeekRange = true;
        } else {
            mInSeekRange = false;
        }
    }

    @Override
    public void onSensorChanged(double fieldHeading, float[] orientationValues) {
        mCurrentSensorHeading = fieldHeading;
        mCurrentHeading = fieldHeading;
    }

    private void updateTimeDisplay() {
        if (mState == State.READY_FOR_MISSION) {
            mCurrentStateTextView.setText(mState.name() + "");
            mTargetHeadingTextView.setText("---");
            mTargetXYTextView.setText("---");
            mTurnAmountTextView.setText("---");
        } else {
            mCurrentStateTextView.setText(mState.name() + " " + getStateTimeMS() / 1000);
            mTargetHeadingTextView.setText("" + mTargetHeading);
        }
        if (mSubState == SubState.INACTIVE) {
            mSubStateTextView.setText("---");
        } else {
            mSubStateTextView.setText(mSubState.name() + " " + getSubStateTimeMS() / 1000);
        }
        if (mSubState != SubState.GPS_SEEKING) {
            mTargetHeadingTextView.setText("---");
            mTurnAmountTextView.setText("---");
        }
    }

    @Override
    public void loop() {
        super.loop();
        updateTimeDisplay();
        switch (mState) {
            case INITIAL_STRAIGHT:
                if (getStateTimeMS() > 4000) {
                    setState(State.NEAR_BALL_MISSION);
                }
                break;
            case NEAR_BALL_MISSION:
                mTargetXYTextView.setText("" + getString(R.string.xy_format, (double) mNearX, (double) mNearY));
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(mNearX, mNearY);
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if (distToTarget() < mSeekRangebig && mConeFound) {
                            setSubState(SubState.IMAGE_REC_SEEKING);
                        } else if (distToTarget() < mSeekRangesmall) {
                            setSubState(SubState.ACTION_SCRIPT);
                        }
                        break;
                    case IMAGE_REC_SEEKING:
                        if (distToTarget() > mSeekRangebig) {
                            setSubState(SubState.GPS_SEEKING);
                        } else if (distToTarget() < mSeekRangesmall && seekImage()) {
                            setSubState(SubState.ACTION_SCRIPT);
                        }
                    default:
                        break;
                }
                break;
            case FAR_BALL_MISSION:
                mTargetXYTextView.setText("" + getString(R.string.xy_format, (double) mFarX, (double) mFarY));
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(mFarX, mFarY);
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if (distToTarget() < mSeekRangebig && mConeFound) {
                            setSubState(SubState.IMAGE_REC_SEEKING);
                        } else if (distToTarget() < mSeekRangesmall - 10) {
                            setSubState(SubState.ACTION_SCRIPT);
                        }
                        break;
                    case IMAGE_REC_SEEKING:
                        if (distToTarget() > mSeekRangebig) {
                            setSubState(SubState.GPS_SEEKING);
                        } else if (distToTarget() < mSeekRangesmall && seekImage()) {
                            setSubState(SubState.ACTION_SCRIPT);
                        }
                        break;
                    default:
                        break;
                }
                break;
            case HOME_CONE_MISSION:
                mTargetXYTextView.setText("" + getString(R.string.xy_format, 0.0, 0.0));
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(0, 0);
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if (distToTarget() < mSeekRangebig && mConeFound) {
                            setSubState(SubState.IMAGE_REC_SEEKING);
                        } else if (distToTarget() < mSeekRangesmall) {
                            setSubState(SubState.ACTION_SCRIPT);
                        }
                        break;

                    case IMAGE_REC_SEEKING:
                        if (distToTarget() > mSeekRangebig) {
                            setSubState(SubState.GPS_SEEKING);
                        } else if (distToTarget() < mSeekRangesmall && seekImage()) {
                            setSubState(SubState.ACTION_SCRIPT);
                        }
                        break;
                    case ACTION_SCRIPT:
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
                        seekTargetAt(0, 0);
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if (distToTarget() < mSeekRangebig && mConeFound) {
                            setSubState(SubState.IMAGE_REC_SEEKING);
                        } else if (distToTarget() < mSeekRangesmall) {
                            setState(State.WAITING_FOR_PICKUP);
                        }
                        break;

                    case IMAGE_REC_SEEKING:
                        if (distToTarget() > mSeekRangebig) {
                            setSubState(SubState.GPS_SEEKING);
                        } else if (distToTarget() < mSeekRangesmall && seekImage()) {
                            setState(State.WAITING_FOR_PICKUP);
                        }
                        break;
                    default:
                        break;
                }
                if (getStateTimeMS() > 4000) {
                    setState(State.WAITING_FOR_PICKUP);
                }
            default:
                break;
        }
    }

    public void checkColor(View view) {
        system_print("We are" + (mIsRedTeam ? "Red" : "Blue") + "Team");
        for (int i = 0; i < 3; i++) {
            if (containinCombo(getCombo(BallColorDetector.BALL_RED), ballcolors[i].getColor())) {
                if (ballcolors[i].getColor() == BallColorDetector.BALL_GREEN) {
                    if (mIsRedTeam) {
                        mNearX = mGreenX;
                        mNearY = mGreenY;
                    } else {
                        mFarX = mGreenX;
                        mFarY = mGreenY;
                    }
                } else {
                    if (mIsRedTeam) {
                        mNearX = mRedX;
                        mNearY = mRedY;
                    } else {
                        mFarX = mRedX;
                        mFarY = mRedY;
                    }
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            if (containinCombo(getCombo(BallColorDetector.BALL_BLUE), ballcolors[i].getColor())) {
                if (ballcolors[i].getColor() == BallColorDetector.BALL_YELLOW) {
                    if (!mIsRedTeam) {
                        mNearX = mYellowX;
                        mNearY = mYellowY;
                    } else {
                        mFarX = mYellowX;
                        mFarY = mYellowY;
                    }
                } else {
                    if (!mIsRedTeam) {
                        mNearX = mBlueX;
                        mNearY = mBlueY;
                    } else {
                        mFarX = mBlueX;
                        mFarY = mBlueY;
                    }
                }
            }
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
                sendCommand("WHEEL SPEED FORWARD 250 FORWARD 250");
                break;
            case NEAR_BALL_MISSION:
                speak("Near Ball Mission");
                setSubState(SubState.GPS_SEEKING);
                break;
            case FAR_BALL_MISSION:
                speak("Far Ball Mission");
                setSubState(SubState.GPS_SEEKING);
                break;
            case HOME_CONE_MISSION:
                speak("Home Cone Mission");
                boolean findwhite = false;
                for (int i = 0; i < 3; i++) {
                    if (containinCombo(getCombo(BallColorDetector.BALL_WHITE), ballcolors[i].getColor())) {
                        if (ballcolors[i].getColor() == BallColorDetector.BALL_WHITE) {
                            findwhite = true;
                            mScript.runScript(i == 0 ? "right_ball_script" : (i == 1 ? "middle_ball_script" : "left_ball_script"));
                            ballcolors[i].next();
                        }
                    }
                }
                if (findwhite) {
                    setSubState(SubState.INACTIVE);
                } else {
                    setSubState(SubState.GPS_SEEKING);
                }
                break;
            case WAITING_FOR_PICKUP:
                setSubState(SubState.INACTIVE);
                sendWheelSpeed(0, 0);
                break;
            case SEEKING_HOME:
                setSubState(SubState.GPS_SEEKING);
                break;
        }
        mState = newState;
    }

    private void setSubState(SubState newSubState) {
        // Write down the current time
        mSubStateStartTime = System.currentTimeMillis();

        switch (newSubState) {
            case GPS_SEEKING:
                mSumHeadingErrorsum = 0;
                mSumHeadingErrorcount = 0;
                mLastHeadingError = 0;
                mHeadingError = 0;
                break;
            case IMAGE_REC_SEEKING:
                break;
            case ACTION_SCRIPT:
                sendWheelSpeed(0, 0);
                if (mState == State.NEAR_BALL_MISSION) {
                    for (int i = 0; i < 3; i++) {
                        if (containinCombo(getCombo(mIsRedTeam ? BallColorDetector.BALL_RED : BallColorDetector.BALL_BLUE), ballcolors[i].getColor())) {
                            mScript.runScript(i == 0 ? "right_ball_script" : (i == 1 ? "middle_ball_script" : "left_ball_script"));
                        }
                    }
                }
                if (mState == State.FAR_BALL_MISSION) {
                    for (int i = 0; i < 3; i++) {
                        if (containinCombo(getCombo(!mIsRedTeam ? BallColorDetector.BALL_RED : BallColorDetector.BALL_BLUE), ballcolors[i].getColor())) {
                            mScript.runScript(i == 0 ? "right_ball_script" : (i == 1 ? "middle_ball_script" : "left_ball_script"));
                        }
                    }
                }
                break;
            case INACTIVE:
                break;
        }
        mSubState = newSubState;
    }

    @Override
    public void onScricptsComplete() {
        super.onScricptsComplete();
        if (mState == State.FAR_BALL_MISSION) {
            setState(State.HOME_CONE_MISSION);
        } else if (mState == State.NEAR_BALL_MISSION) {
            setState(State.FAR_BALL_MISSION);
        } else if (mState == State.HOME_CONE_MISSION) {
            setSubState(SubState.GPS_SEEKING);
        }

    }


    private boolean seekImage() {
        if (mConeFound) {
            double Target = 20 * mLeftRightCone * (max_size_percentage - mConeSize);

            if (mConeSize >= max_size_percentage) {
                mWithinRange = true;

                sendCommand("WHEEL SPEED BRAKE 0 BRAKE 0");
                return true;
            }
            pid_Controler(Target, LEFT_PWM_VALUE_FOR_STRAIGHT - 100);
            return false;
        } else {
            return distToTarget() < mSeekRangesmall;
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

        pid_Controler(mLeftTurnAmount > 180 ? mLeftTurnAmount - 360 : mLeftTurnAmount, LEFT_PWM_VALUE_FOR_STRAIGHT);
    }


    private void pid_Controler(double error, int max_speed) {

        double p_gain = 3;
        double i_gain = .0001;
        double d_gain = .1;

        mLastHeadingError = mHeadingError;
        mHeadingError = error;
        mSumHeadingErrorsum += mHeadingError;
        mSumHeadingErrorcount++;

        mLeftTurnAmount = p_gain * mHeadingError + d_gain * (mHeadingError - mLastHeadingError) + (i_gain * mSumHeadingErrorsum / mSumHeadingErrorcount);

        int modifier = Math.abs(Math.round((float) mLeftTurnAmount));
        int slow_speed = max_speed - modifier;
        int fast_speed = max_speed + modifier;
        slow_speed = slow_speed < 125 ? 125 : slow_speed;
        fast_speed = (fast_speed > LEFT_PWM_VALUE_FOR_STRAIGHT) ? LEFT_PWM_VALUE_FOR_STRAIGHT : fast_speed;
        if (mLeftTurnAmount < 0) {
            mLeftDutyCycle = fast_speed;
            mRightDutyCycle = slow_speed;
            mTurnAmountTextView.setText("error: " + error + "\nRIGHT:" + modifier);
        } else {
            mLeftDutyCycle = slow_speed;
            mRightDutyCycle = fast_speed;
            mTurnAmountTextView.setText("error: " + error + "\nLEFT:" + modifier);

        }
        sendWheelSpeed(mRightDutyCycle, mLeftDutyCycle);
    }

    public double distToTarget() {
        double xDist = mXTarget - mCurrentGpsX;
        double yDist = mYTarget - mCurrentGpsY;
        double number = Math.sqrt((xDist * xDist) + (yDist * yDist));
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
        mSumHeadingErrorsum = 0;
        mSumHeadingErrorcount = 0;
        mCommandHandler.removeCallbacksAndMessages(null);
        sendCommand("WHEEL SPEED BRAKE 0 BRAKE 0");
        mTeamToggle.setChecked(false);
    }

    public void go(View view) {
        if (mState == State.READY_FOR_MISSION) {
            setState(State.INITIAL_STRAIGHT);
            //mTargetXYTextView.setText("(90, -50)");
        }
    }

//    public void notSeen(View view) {
//        if (mSubState == SubState.IMAGE_REC_SEEKING) {
//            setSubState(SubState.GPS_SEEKING);
//        }
//    }
//
//    public void seenSmall(View view) {
//        if (mSubState == SubState.GPS_SEEKING) {
//            setSubState(SubState.IMAGE_REC_SEEKING);
//        }
//    }
//
//    public void seenBig(View view) {
//        if (mSubState == SubState.IMAGE_REC_SEEKING) {
//            setSubState(SubState.OPTIONAL_SCRIPT);
//        }
//    }

    public void missionComplete(View view) {
        if (mState == State.WAITING_FOR_PICKUP) {
            setState(State.READY_FOR_MISSION);
        }
    }

    public void handleSetOrigin(View view) {
        mFieldGps.setCurrentLocationAsOrigin(mStorage);
    }

    public void handleSetXAxis(View view) {
        mFieldGps.setCurrentLocationAsLocationOnXAxis(mStorage);
    }

    /**
     * Displays the blob target info in the text views.
     */
    @Override
    public void onImageRecComplete(boolean coneFound, double leftRightLocation,
                                   double topBottomLocation, double sizePercentage) {
        super.onImageRecComplete(coneFound, leftRightLocation, topBottomLocation, sizePercentage);
        mConeFound = coneFound;
        if (coneFound) {
            mLeftRightCone = leftRightLocation;
            mConeSize = sizePercentage;
//            mLeftRightLocationTextView.setText(String.format("%.3f", leftRightLocation));
//            mTopBottomLocationTextView.setText(String.format("%.3f", topBottomLocation));
//            mSizePercentageTextView.setText(String.format("%.5f", sizePercentage));
            findViewById(R.id.displayTextLayout).setBackgroundColor(Color.rgb(255, 0, 0));
        } else {
            mLeftRightCone = 0;
            mConeSize = 0;
            findViewById(R.id.displayTextLayout).setBackgroundColor(Color.rgb(255, 255, 255));
//            mLeftRightLocationTextView.setText("---");
//            mTopBottomLocationTextView.setText("---");
//            mSizePercentageTextView.setText("---");
        }
    }


}
