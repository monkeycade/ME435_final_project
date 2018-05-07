package edu.rosehulman.everhadg.TempSubstatePackage;

import android.location.Location;
import android.os.Handler;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import edu.rosehulman.me435.AccessoryActivity;
import edu.rosehulman.me435.FieldGps;
import edu.rosehulman.me435.FieldGpsListener;
import edu.rosehulman.me435.FieldOrientation;
import edu.rosehulman.me435.FieldOrientationListener;
import edu.rosehulman.me435.NavUtils;

public class TempSubFSMTemp extends AccessoryActivity implements FieldGpsListener, FieldOrientationListener {

    /*public enum State {
        READY_FOR_MISSION,INITIAL_STRAIGHT,NEAR_BALL_MISSION,FAR_BALL_MISSION,HOME_CONE_MISSION,WAITING_FOR_PICKUP,SEEKING_HOME;
    }*/

    public enum SubState {
        READY_FOR_MISSION, INITIAL_STRAIGHT,GPS_SEEKING,BALL_REMOVAL_SCRIPT, DEAD_STATE;
    }

    private TextView mCurrentStateTextView,mSubStateTextView,mGPSTextView,mTargetXYTextView,mTargetHeadingTextView,mTurnAmountTextView,mCommandTextView,mRedBallTextView,mWhiteBallTextView,mBlueBallTextView;
    //private State mState;
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
    public static final int LOWEST_DESIRABLE_DUTY_CYCLE = 50;
    public static final int LEFT_PWM_VALUE_FOR_STRAIGHT = 255;
    public static final int RIGHT_PWM_VALUE_FOR_STRAIGHT = 255;
    private boolean mWithinTollerance = false;

    private double mHeadingError = 0;
    private double mLastHeadingError = 0;
    private double mSumHeadingError = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRedBallTextView =  findViewById(R.id.redBallLabel);
        mBlueBallTextView = findViewById(R.id.blueBallLabel);
        mWhiteBallTextView = findViewById(R.id.whiteBallLabel);
        mCommandTextView = findViewById(R.id.commandLabel);
        mCurrentStateTextView = findViewById(R.id.highStateLabel);
        mSubStateTextView = findViewById(R.id.subStateLabel);
        mGPSTextView = findViewById(R.id.gpsLabel);
        mTargetXYTextView = findViewById(R.id.targetXYLabel);
        mTargetHeadingTextView = findViewById(R.id.headingLabel);
        mTurnAmountTextView = findViewById(R.id.turnAmountLabel);
        //setState(State.READY_FOR_MISSION);
        setSubState(SubState.READY_FOR_MISSION);

        /**
         * Check this, might cause a but on imports
         */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mFieldGps = new FieldGps(this);
        mFieldOrientation = new FieldOrientation(this);

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

        String gpsInfo = getString(R.string.xy_format, x, y);
        if (heading <= 180.0 && heading > -180.0) {
            gpsInfo += " " + getString(R.string.degrees_format, heading);
            //mSensorOrientationTextView.setText("" + (int)heading);
            mCurrentGpsHeading = heading;
            mFieldOrientation.setCurrentFieldHeading(heading);
            mCurrentHeading = heading;
        } else {
            gpsInfo += "?̊";
            //mSensorOrientationTextView.setText("---");
        }
        gpsInfo += "   " + mGpsCounter;
        mGPSTextView.setText(gpsInfo);
    }

    @Override
    public void onSensorChanged(double fieldHeading, float[] orientationValues){
        mCurrentSensorHeading = fieldHeading;
        mCurrentHeading = fieldHeading;
    }

    private void updateTimeDisplay(){
        if(mSubState == SubState.READY_FOR_MISSION){
            mCurrentStateTextView.setText(mSubState.name() + "");
            mRedBallTextView.setText("Red\nBall");
            mBlueBallTextView.setText("Blue\nBall");
            mWhiteBallTextView.setText("White\nBall");
            mGPSTextView.setText("---");
            mTargetHeadingTextView.setText("---");
            mTargetXYTextView.setText("---");
            mCommandTextView.setText("---");
            mTurnAmountTextView.setText("---");
        } else {
            mCurrentStateTextView.setText(mSubState.name() + " " + getSubStateTimeMS() / 1000);
        }

        mSubStateTextView.setText(mSubState.name() + " " + getSubStateTimeMS() / 1000);

        if(mSubState != SubState.GPS_SEEKING){
            mTargetHeadingTextView.setText("---");
            mCommandTextView.setText("---");
            mTurnAmountTextView.setText("---");
        }
    }

    private void loop(){
        updateTimeDisplay();
        switch (mSubState){
            //case READY_FOR_MISSION:
            //    break;
            case INITIAL_STRAIGHT:
                if (getSubStateTimeMS() > 4000) {
                    setSubState(SubState.GPS_SEEKING);
                    sendCommand("WHEEL SPEED BRAKE 0 BRAKE 0");
                }
                break;
            case GPS_SEEKING:

                        seekTargetAt(50,-50);
                        if (NavUtils.targetIsOnLeft(mCurrentGpsX,mCurrentGpsY,mCurrentGpsHeading,mXTarget,mYTarget)) {
                            mTurnAmountTextView.setText("Left " + getString(R.string.degrees_format, mLeftTurnAmount));
                        } else {
                            mTurnAmountTextView.setText("Right " + getString(R.string.degrees_format, mRightTurnAmount));
                        }
                        mCommandTextView.setText("WHEEL SPEED FORWARD " + mLeftDutyCycle + " FORWARD " + mRightDutyCycle);
                        mTargetHeadingTextView.setText("" + getString(R.string.degrees_format, mTargetHeading));
                        if (getSubStateTimeMS() > 60000) {
                            setSubState(SubState.BALL_REMOVAL_SCRIPT);
                        }
                        if (mWithinTollerance) {
                            setSubState(SubState.BALL_REMOVAL_SCRIPT);
                        }
                break;
            case BALL_REMOVAL_SCRIPT:
                    superSpecialBallRemoval();
                mRedBallTextView.setText("---");
            default:
                break;
        }
    }

    /*private long getStateTimeMS() {
        return System.currentTimeMillis() - mStateStartTime;
    }*/

    private long getSubStateTimeMS() {
        return System.currentTimeMillis() - mSubStateStartTime;
    }


    /**
     * For this lab SetState isn't needed, so we are just using the substate
     *
     */
    /*private void setState(State newState) {

        // Write down (in a variable) the current time
        mStateStartTime = System.currentTimeMillis();

        // Update the UI with the name of the current state
        // mCurrentStateTextView.setText(newState.name());

        // Opportunity to do stuff when a new state is set.
        switch (newState) {
            case READY_FOR_MISSION:
                setSubState(SubState.INACTIVE);
                break;
            case INITIAL_STRAIGHT:
                break;
            case NEAR_BALL_MISSION:
                onLocationChanged(60,-25,-30,null);
                setSubState(SubState.GPS_SEEKING);
                break;
            case FAR_BALL_MISSION:
                setSubState(SubState.GPS_SEEKING);
                mTargetXYTextView.setText("(240, 50)");
                onLocationChanged(90,-50,-46,null);
                break;
            case HOME_CONE_MISSION:
                setSubState(SubState.GPS_SEEKING);
                mTargetXYTextView.setText("(0, 0)");
                onLocationChanged(240,50,45,null);
                break;
            case WAITING_FOR_PICKUP:
                setSubState(SubState.INACTIVE);
                break;
            case SEEKING_HOME:
                setSubState(SubState.GPS_SEEKING);
                break;
        }

        mState = newState;
    }*/

    private void setSubState(SubState newSubState){
        // Write down the current time
        mSubStateStartTime = System.currentTimeMillis();

        switch (newSubState) {
            case GPS_SEEKING:
                break;
            case INITIAL_STRAIGHT:
                sendCommand("WHEEL SPEED FORWARD " + RIGHT_PWM_VALUE_FOR_STRAIGHT + " FORWARD " + LEFT_PWM_VALUE_FOR_STRAIGHT);
                break;
            case BALL_REMOVAL_SCRIPT:
                superSpecialBallRemoval();
                break;
            case DEAD_STATE:
                sendCommand("WHEEL SPEED BRAKE 0 BRAKE 0");
            default:
                break;
        }

        mSubState = newSubState;
    }

    private void seekTargetAt(double xTarget, double yTarget) {
        mLeftDutyCycle = LEFT_PWM_VALUE_FOR_STRAIGHT;
        mRightDutyCycle = RIGHT_PWM_VALUE_FOR_STRAIGHT;
        mTargetHeading = NavUtils.getTargetHeading(mCurrentGpsX, mCurrentGpsY, xTarget, yTarget);
        mLeftTurnAmount = NavUtils.getLeftTurnHeadingDelta(mCurrentGpsHeading, mTargetHeading);
        mRightTurnAmount = NavUtils.getRightTurnHeadingDelta(mCurrentGpsHeading, mTargetHeading);

        mXTarget = xTarget;
        mYTarget = yTarget;

        if(((mXTarget-mCurrentGpsX)*(mXTarget-mCurrentGpsX)+(mYTarget-mCurrentGpsY)*(mYTarget-mCurrentGpsY))<40){
            if(mSubState == SubState.GPS_SEEKING) {
                setSubState(SubState.BALL_REMOVAL_SCRIPT);
                mWithinTollerance = true;
            }
            return;
        }

        double p_gain = 5;
        double d_gain = 2;
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


    public void reset(View view) {
        setSubState(SubState.READY_FOR_MISSION);
        mGpsCounter = 0;
        mCurrentGpsHeading = 0;
        mCurrentGpsX = 0;
        mCurrentGpsY = 0;
        mGpsCounter = 0;
        mWithinTollerance = false;
        sendCommand("WHEEL SPEED BRAKE 0 BRAKE 0");
    }

    public void go(View view) {
        if (mSubState == SubState.READY_FOR_MISSION) {
            setSubState(SubState.INITIAL_STRAIGHT);
            mTargetXYTextView.setText("(90, -50)");
        }
    }

    public void notSeen(View view) {
        /*if (mSubState == SubState.IMAGE_REC_SEEKING) {
            setSubState(SubState.GPS_SEEKING);
        }*/
    }

    public void seenSmall(View view) {
        /*if (mSubState == SubState.GPS_SEEKING) {
            setSubState(SubState.IMAGE_REC_SEEKING);
        }*/
    }

    public void seenBig(View view) {
        /*if (mSubState == SubState.IMAGE_REC_SEEKING) {
            setSubState(SubState.OPTIONAL_SCRIPT);
        }*/
    }

    public void missionComplete(View view) {
        if (mSubState == SubState.BALL_REMOVAL_SCRIPT) {
            setSubState(SubState.READY_FOR_MISSION);
        }
    }
    public void superSpecialBallRemoval(){
        /**
         * Cade's Ultimate Secret move that teleports golf balls
         */
        sendCommand("WHEEL SPEED BRAKE 0 BRAKE 0");
    }

    // TODO Need buttons to set the origin of the robot's location
    public void handleSetOrigin(View view){
        mFieldGps.setCurrentLocationAsOrigin();
    }
    public void handleSetXAxis(View view){
        mFieldGps.setCurrentLocationAsLocationOnXAxis();
    }
}
