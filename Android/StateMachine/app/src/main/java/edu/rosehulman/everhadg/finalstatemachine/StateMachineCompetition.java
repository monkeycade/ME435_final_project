package edu.rosehulman.everhadg.finalstatemachine;

import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import edu.rosehulman.everhadg.finalstatemachine.me435.AccessoryActivity;
import edu.rosehulman.everhadg.finalstatemachine.me435.FieldGps;
import edu.rosehulman.everhadg.finalstatemachine.me435.FieldGpsListener;
import edu.rosehulman.everhadg.finalstatemachine.me435.FieldOrientation;
import edu.rosehulman.everhadg.finalstatemachine.me435.FieldOrientationListener;
import edu.rosehulman.everhadg.finalstatemachine.me435.NavUtils;
import edu.rosehulman.everhadg.finalstatemachine.me435.RobotActivity;

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

public class StateMachineCompetition extends RobotActivity implements FieldGpsListener, FieldOrientationListener {

    public enum State {
        READY_FOR_MISSION,INITIAL_STRAIGHT,NEAR_BALL_MISSION,FAR_BALL_MISSION,HOME_CONE_MISSION,WAITING_FOR_PICKUP,SEEKING_HOME;
    }

    public enum SubState {
        GPS_SEEKING,IMAGE_REC_SEEKING,OPTIONAL_SCRIPT, INACTIVE;
    }

    public int mYellowX = 240, mYellowY = -50, mGreenX = 90, mGreenY = 50, mRedX = 90, mRedY = -50,mBlueX = 240, mBlueY = 50;
    public int mNearX, mNearY, mFarX, mFarY;
    public boolean mIsRedTeam, mInSeekRange = false;
    public boolean mHasRed = true, mHasBlue = true, mHasWhite = true, mWithinRange = false;
    private TextView mCurrentStateTextView,mSubStateTextView,mGPSTextView,mTargetXYTextView,mTargetHeadingTextView,mTurnAmountTextView,mCommandTextView,mRedBallTextView,mWhiteBallTextView,mBlueBallTextView;
    private State mState;
    private SubState mSubState;
    private long mStateStartTime;
    private long mSubStateStartTime;
    private Handler mCommandHandler = new Handler();
    private FieldGps mFieldGps;
    private FieldOrientation mFieldOrientation;
    private Timer mTimer;
    private int mGpsCounter = 0;
    private double mCurrentGpsX, mCurrentGpsY, mCurrentGpsHeading, mTargetHeading,mLeftTurnAmount,mRightTurnAmount, mXTarget,mYTarget;
    private double mCurrentSensorHeading, mCurrentHeading;
    private int mLeftDutyCycle,mRightDutyCycle;
    private static final int LOOP_INTERVAL_MS = 100;
    public static final int LOWEST_DESIRABLE_DUTY_CYCLE = 150;
    public static final int LEFT_PWM_VALUE_FOR_STRAIGHT = 255;
    public static final int RIGHT_PWM_VALUE_FOR_STRAIGHT = 255;

    private double mHeadingError = 0;
    private double mLastHeadingError = 0;
    private double mSumHeadingError = 0;

    private double mDetectionThresh = 0.01;
    private double mImageStopThresh = 0.07;

    protected void upDateTeam(View view){

    }

    protected void upDateGoals(View view){

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state_machine_competition);

        /*mCurrentStateTextView = findViewById(R.id.highStateLabel);
        mSubStateTextView = findViewById(R.id.subStateLabel);
        mGPSTextView = findViewById(R.id.gpsLabel);
        mTargetXYTextView = findViewById(R.id.targetXYLabel);
        mTargetHeadingTextView = findViewById(R.id.headingLabel);
        mTurnAmountTextView = findViewById(R.id.turnAmountLabel);*/
        setState(State.READY_FOR_MISSION);
        setSubState(SubState.INACTIVE);
        //ToggleButton teamToggle = findViewById(R.id.teamToggleButton);
        //teamToggle.setOnCheckedChangeListener(CompoundButton buttonView,);
        mCurrentGpsHeading = 0;
        mCurrentGpsX = 0;
        mCurrentGpsY = 0;
        mGpsCounter = 0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFieldOrientation.registerListener(this);
        mFieldGps.requestLocationUpdates(this);
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loop();
                    }
                });
            }
        }, 0, LOOP_INTERVAL_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFieldOrientation.unregisterListener();
        mFieldGps.removeUpdates();
        mTimer.cancel();
        mTimer = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mFieldGps.requestLocationUpdates(this);
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
            mGPSTextView.setText("---");
            mTargetHeadingTextView.setText("---");
            mTargetXYTextView.setText("---");
            mCommandTextView.setText("---");
            mTurnAmountTextView.setText("---");
        } else {
            mCurrentStateTextView.setText(mState.name() + " " + getStateTimeMS() / 1000);
        }
        if(mSubState == SubState.INACTIVE){
            mSubStateTextView.setText("---");
        } else {
            mSubStateTextView.setText(mSubState.name() + " " + getSubStateTimeMS() / 1000);
        }
        if(mSubState != SubState.GPS_SEEKING){
            mTargetHeadingTextView.setText("---");
            mCommandTextView.setText("---");
            mTurnAmountTextView.setText("---");
        }
    }

    private void loop(){
        updateTimeDisplay();
        switch (mState){
            case INITIAL_STRAIGHT:
                if (getStateTimeMS() > 4000) {
                    setState(State.NEAR_BALL_MISSION);
                }
                break;
            case NEAR_BALL_MISSION:
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(mNearX,mNearY);
                        if (NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,mXTarget,mYTarget)) {
                            mTurnAmountTextView.setText("Left " + getString(R.string.degrees_format, mLeftTurnAmount));
                        } else {
                            mTurnAmountTextView.setText("Right " + getString(R.string.degrees_format, mRightTurnAmount));
                        }
                        mCommandTextView.setText("WHEEL SPEED FORWARD " + mLeftDutyCycle + " FORWARD " + mRightDutyCycle);
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
                        if(mWithinRange){
                            mWithinRange = false;
                            setSubState(SubState.OPTIONAL_SCRIPT);
                        }
                    case OPTIONAL_SCRIPT:
                        if (getSubStateTimeMS() > 4000) {
                            setState(State.FAR_BALL_MISSION);
                            mRedBallTextView.setText("---");
                        }
                    default:
                        break;
                }
                break;
            case FAR_BALL_MISSION:
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(mFarX,mFarY);
                        if (NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,mXTarget,mYTarget)) {
                            mTurnAmountTextView.setText("Left " + getString(R.string.degrees_format, mLeftTurnAmount));
                        } else {
                            mTurnAmountTextView.setText("Right " + getString(R.string.degrees_format, mLeftTurnAmount));
                        }
                        mCommandTextView.setText("WHEEL SPEED FORWARD " + mLeftDutyCycle + " FORWARD " + mRightDutyCycle);
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
                switch (mSubState) {
                    case GPS_SEEKING:
                        seekTargetAt(0,0);
                        if (NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,mXTarget,mYTarget)) {
                            mTurnAmountTextView.setText("Left " + getString(R.string.degrees_format, mLeftTurnAmount));
                        } else {
                            mTurnAmountTextView.setText("Right " + getString(R.string.degrees_format, mLeftTurnAmount));
                        }
                        mCommandTextView.setText("WHEEL SPEED FORWARD " + mLeftDutyCycle + " FORWARD " + mRightDutyCycle);
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
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
                        mCommandTextView.setText("WHEEL SPEED FORWARD " + mLeftDutyCycle + " FORWARD " + mRightDutyCycle);
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if(seeSomething())
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
                break;
            case IMAGE_REC_SEEKING:
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

    private void seekImage(double Target, double imageSize){
        int slowDown = 50;
        mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT-slowDown;
        mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT-slowDown;
        double p_gain = 1;
        double d_gain = .1;
        double i_gain = .05;
        if(imageSize>=mImageStopThresh){
            setSubState(SubState.OPTIONAL_SCRIPT);
            sendCommand("WHEEL SPEED BRAKE 0 BRAKE 0");
        }
        if(Target<0){
            mLastHeadingError = mHeadingError;
            mHeadingError = Math.abs(Target);
            mSumHeadingError += mHeadingError;
            mLeftTurnAmount = p_gain*mHeadingError + d_gain*(mHeadingError-mLastHeadingError) + (i_gain*mSumHeadingError/getSubStateTimeMS());
            mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT - slowDown - (int)mLeftTurnAmount; // Using a VERY simple plan. :)
            if(mLeftDutyCycle>LEFT_PWM_VALUE_FOR_STRAIGHT){
                mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT-slowDown;
            }
            mLeftDutyCycle = Math.max(mLeftDutyCycle, LOWEST_DESIRABLE_DUTY_CYCLE);
        } else {
            mLastHeadingError = mHeadingError;
            mHeadingError = Math.abs(Target);
            mSumHeadingError += mHeadingError;

            mRightTurnAmount = p_gain*mHeadingError + d_gain*(mHeadingError-mLastHeadingError) + (i_gain*mSumHeadingError/getSubStateTimeMS());
            mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT - slowDown - (int)mRightTurnAmount; // Could also scale it.
            if(mRightDutyCycle>RIGHT_PWM_VALUE_FOR_STRAIGHT){
                mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT - slowDown;
            }

            mRightDutyCycle = Math.max(mRightDutyCycle, LOWEST_DESIRABLE_DUTY_CYCLE);
        }
        //LOOP_INTERVAL_MS

        sendCommand("WHEEL SPEED FORWARD " + mRightDutyCycle + " FORWARD " + mLeftDutyCycle);
    }

    private void seekTargetAt(double xTarget, double yTarget) {
        mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT;
        mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT;
        mTargetHeading = NavUtils.getTargetHeading(mCurrentGpsX, mCurrentGpsY, xTarget, yTarget);
        mLeftTurnAmount = NavUtils.getLeftTurnHeadingDelta(mCurrentGpsHeading, mTargetHeading);
        mRightTurnAmount = NavUtils.getRightTurnHeadingDelta(mCurrentGpsHeading, mTargetHeading);

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


        if(NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,xTarget,yTarget)){//if (mLeftTurnAmount < mRightTurnAmount) {
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

    protected void onToggleTeam(View view){

    }

    public void handleSetOrigin(View view){
        mFieldGps.setCurrentLocationAsOrigin();
    }
    public void handleSetXAxis(View view){
        mFieldGps.setCurrentLocationAsLocationOnXAxis();
    }
}
