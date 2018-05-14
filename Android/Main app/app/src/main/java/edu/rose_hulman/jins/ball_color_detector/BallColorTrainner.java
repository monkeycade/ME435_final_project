package edu.rose_hulman.jins.ball_color_detector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import edu.rose_hulman.everhadg.TempSubFSMTemp;
import edu.rose_hulman.jins.final_project_main.R;
import edu.rose_hulman.jins.fsm_main.FSM_System;

public class BallColorTrainner extends TempSubFSMTemp {
    public final static String BALL_DATA_KEY = "Ball data at location ";

    public final static String BALL_COEFFICIENT_KEY = "Ball coefficient at location ";

    /**
     * An array constants (of size 7) that keeps a reference to the different ball color images resources.
     */
    // Note, the order is important and must be the same throughout the app.
    private static final int[] BALL_DRAWABLE_RESOURCES = new int[]{R.drawable.none_ball,R.drawable.black_ball,
            R.drawable.blue_ball, R.drawable.green_ball, R.drawable.red_ball, R.drawable.yellow_ball, R.drawable.white_ball};


    public final static int BALL_NONE = -1;
    public final static int BALL_BLACK = 0;
    public final static int BALL_BLUE = 1;
    public final static int BALL_GREEN = 2;
    public final static int BALL_RED = 3;
    public final static int BALL_YELLOW = 4;
    public final static int BALL_WHITE = 5;

    private BallColorDetector[] ballHandlers;
    private ImageButton[] mBallImageButtons;
    private TextView[] mBallConfidences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBallConfidences = new TextView[]{findViewById(R.id.color_debugger_ball_confidence_at_1),
                findViewById(R.id.color_debugger_ball_confidence_at_2),
                findViewById(R.id.color_debugger_ball_confidence_at_3)};
        mBallImageButtons = new ImageButton[]{findViewById(R.id.color_debugger_image_button_1),
                findViewById(R.id.color_debugger_image_button_2),
                findViewById(R.id.color_debugger_image_button_3)};


        ballHandlers = new BallColorDetector[3];
        for (int location = 1; location < 4; location++) {
            List coef = mStorage.readColorCoeffList(BALL_COEFFICIENT_KEY + location);
            List balldata = mStorage.readInstanceList(BALL_DATA_KEY + location);
            ballHandlers[location - 1] = new BallColorDetector(coef, balldata);
        }
    }

    private void colordebugGetBallColor(int location) {
        sendCommand("CUSTOM Detect Color at " + location);
        system_print("get data for the location " + location);
    }

    private void colordebugHandleChangeBall(final int location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(BallColorTrainner.this);
        builder.setTitle("What was the real color?").setItems(R.array.ball_color_debug_ball_colors,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        BallColorTrainner.this.colordebugHandleChangeBallhelper(location, which);
                    }
                });
        builder.create().show();
    }

    private void colordebugHandleChangeBallhelper(int location, int which) {
        mBallImageButtons[location - 1].setImageResource(BALL_DRAWABLE_RESOURCES[which + 1]);
        mBallConfidences[location - 1].setText("Modified");
        mBallConfidences[location - 1].setTextColor(Color.parseColor("#0000ff"));
    }


    private void colordebugHandleTrainBall(int location) {

    }

    public void color_debug_get_ball_color_at_1(View view) {
        colordebugGetBallColor(1);
    }

    public void color_debug_get_ball_color_at_2(View view) {
        colordebugGetBallColor(2);
    }

    public void color_debug_get_ball_color_at_3(View view) {
        colordebugGetBallColor(3);
    }

    public void color_debug_handle_ball_change_at_1(View view) {
        colordebugHandleChangeBall(1);
    }

    public void color_debug_handle_ball_change_at_2(View view) {
        colordebugHandleChangeBall(2);
    }

    public void color_debug_handle_ball_change_at_3(View view) {
        colordebugHandleChangeBall(3);
    }

    public void color_debug_train_ball_color_at_1(View view) {
        colordebugHandleTrainBall(1);
    }

    public void color_debug_train_ball_color_at_2(View view) {
        colordebugHandleTrainBall(2);
    }

    public void color_debug_train_ball_color_at_3(View view) {
        colordebugHandleTrainBall(3);
    }
}
