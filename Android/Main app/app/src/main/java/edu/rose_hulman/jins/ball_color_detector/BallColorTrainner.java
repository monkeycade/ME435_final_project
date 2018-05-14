package edu.rose_hulman.jins.ball_color_detector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import edu.rose_hulman.everhadg.StateMachineCompetition;
import edu.rose_hulman.everhadg.TempSubFSMTemp;
import edu.rose_hulman.jins.final_project_main.R;
import edu.rose_hulman.jins.image_recgnation_debug.ConeFinderActivity;

public class BallColorTrainner extends ConeFinderActivity {
    public final static String BALL_DATA_KEY = "Ball data at location ";

    public final static String BALL_COEFFICIENT_KEY = "Ball coefficient at location ";

    /**
     * An array constants (of size 7) that keeps a reference to the different ball color images resources.
     */
    // Note, the order is important and must be the same throughout the app.
    private static final int[] BALL_DRAWABLE_RESOURCES = new int[]{R.drawable.none_ball, R.drawable.black_ball,
            R.drawable.blue_ball, R.drawable.green_ball, R.drawable.red_ball, R.drawable.yellow_ball, R.drawable.white_ball};

    private BallColorDetector.BallResult[] ballcolors;

    private BallColorDetector[] ballHandlers;
    private ImageButton[] mBallImageButtons;
    private TextView[] mBallConfidences, mBallOtherResult;

    private ToggleButton mValidation;
    private boolean checkall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBallConfidences = new TextView[]{findViewById(R.id.color_debugger_ball_confidence_at_1),
                findViewById(R.id.color_debugger_ball_confidence_at_2),
                findViewById(R.id.color_debugger_ball_confidence_at_3)};
        mBallOtherResult = new TextView[]{findViewById(R.id.color_debugger_ball_other_data_at_1),
                findViewById(R.id.color_debugger_ball_other_data_at_2),
                findViewById(R.id.color_debugger_ball_other_data_at_3)};
        mBallImageButtons = new ImageButton[]{findViewById(R.id.color_debugger_image_button_1),
                findViewById(R.id.color_debugger_image_button_2),
                findViewById(R.id.color_debugger_image_button_3)};


        ballHandlers = new BallColorDetector[3];
        for (int location = 1; location < 4; location++) {
            List coef = mStorage.readColorCoeffList(BALL_COEFFICIENT_KEY + location);
            List balldata = mStorage.readInstanceList(BALL_DATA_KEY + location);
            ballHandlers[location - 1] = new BallColorDetector(coef, balldata);
            String errorReport = ballHandlers[location - 1].getErrorData();
            if (!errorReport.equals("")) {
                system_print("At location " + location + "\n" + errorReport);
            }
        }
        ballcolors = new BallColorDetector.BallResult[3];
        mValidation = findViewById(R.id.color_debugger_check_validation);
        checkall = false;
    }

    public void identifyColor(){
        mValidation.setChecked(true);
        color_debug_get_all_ball_color(null);

    }
    private void colordebugGetBallColorSender(int location) {
        sendCommand("CUSTOM Detect Color at " + location);
    }

    @Override
    protected void onCommandReceived(String receivedCommand) {
        super.onCommandReceived(receivedCommand);
        if (receivedCommand.substring(0, 8).equalsIgnoreCase("location")) {
            String[] data = receivedCommand.split(" ");
            int location = Integer.parseInt(data[1]);
            if (data.length < 4) {
                colordebugHandleChangeBallhelper(location, -1, false);
            } else {
                int[] x = new int[5];
                for (int i = 2; i < data.length; i++) {
                    x[i - 2] = Integer.parseInt(data[i]);
                }
                detectBallColor(location, x);
            }
        }
    }

    private void detectBallColor(int location, int[] x) {
        int index = location - 1;
        ballcolors[location - 1] = ballHandlers[location - 1].guessBallColor(x);
        colordebugHandleChangeBallhelper(location, ballcolors[location - 1].getColor(), false);
        mBallOtherResult[index].setText(ballcolors[location - 1].toString());
        double ballconf = Math.round(ballcolors[location - 1].result().mconf * 10000) / 100.0;
        mBallConfidences[index].setText(ballconf + "%");
        if (ballconf > 90) {
            mBallConfidences[location - 1].setTextColor(Color.parseColor("#00ff00"));
        } else if (ballconf > 75) {
            mBallConfidences[location - 1].setTextColor(Color.parseColor("#0f0f00"));
        } else if (ballconf > 65) {
            mBallConfidences[location - 1].setTextColor(Color.parseColor("#f00f00"));
        } else {
            mBallConfidences[location - 1].setTextColor(Color.parseColor("#ff0000"));
        }
//        system_print("" + ballHandlers[location - 1].classify(x, BALL_BLUE));
        if (checkall) {
            if (location < 3) {
                colordebugGetBallColorSender(location + 1);
            } else {
                checkall = false;
                if (mValidation.isChecked()) {
                    PriorityQueue<BallColorDetector.BallResult> temp = new PriorityQueue<>();
                    for (int i = 0; i < 3; i++) {
                        temp.add(ballcolors[i]);
                    }
                    while (temp.size() > 0) {
                        BallColorDetector.BallResult current = temp.poll();
                        int[] combo = getCombo(current.getColor());
                        if (combo == null) {
                            system_print("CHECK THE BALL, YOU DID MESS IT UP A LOT");
                            break;
                        }
                        while (true) {
                            boolean ischecked = true;
                            for (int i = 0; i < 3; i++) {
                                if (current != ballcolors[i]) {
                                    if (containinCombo(combo, ballcolors[i].getColor())) {
                                        ballcolors[i].next();
                                        colordebugHandleChangeBallhelper(i + 1, ballcolors[i].getColor(), false);
                                        ischecked = false;
                                    }
                                }
                            }
                            if (ischecked) {
                                break;
                            }
                        }
                    }
                }
            }
        }

    }

    private boolean containinCombo(int[] combo, int color) {
        for (int i = 0; i < combo.length; i++) {
            if (combo[i] == color) {
                return true;
            }
        }
        return false;
    }

    private int[] getCombo(int color) {
        switch (color) {
            case 0:
                return new int[]{0, 5};
            case 1:
                return new int[]{1, 4};
            case 2:
                return new int[]{2, 3};
            case 3:
                return new int[]{2, 3};
            case 4:
                return new int[]{1, 4};
            case 5:
                return new int[]{0, 5};
            default:
                return null;
        }

    }

    private void colordebugHandleChangeBall(final int location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(BallColorTrainner.this);
        builder.setTitle("What was the real color?").setItems(R.array.ball_color_debug_ball_colors,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        BallColorTrainner.this.colordebugHandleChangeBallhelper(location, which, true);
                    }
                });
        builder.create().show();
    }

    private void colordebugHandleChangeBallhelper(final int location, int which, boolean askTrain) {
        mBallImageButtons[location - 1].setImageResource(BALL_DRAWABLE_RESOURCES[which + 1]);
        mBallConfidences[location - 1].setText("Modified");
        mBallConfidences[location - 1].setTextColor(Color.parseColor("#0000ff"));
        if ((ballcolors[location - 1] == null || ballcolors[location - 1].getColor() != which) && askTrain) {
            ballcolors[location - 1].setColor(which);
            AlertDialog.Builder builder = new AlertDialog.Builder(BallColorTrainner.this);
            builder.setMessage("Train this Data?").
                    setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            balltrainnerhelper(location, ballcolors[location - 1]);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Do Nothing
                        }
                    });
            builder.create().show();

        }
    }

    private void balltrainnerhelper(int location, BallColorDetector.BallResult result) {
        int index = location - 1;
        ballHandlers[location - 1].addNewData(result);
        detectBallColor(location, result.reading);
        mStorage.store(BALL_COEFFICIENT_KEY + location, ballHandlers[location - 1].gettostoreCoefficient());
        mStorage.store(BALL_DATA_KEY + location, ballHandlers[location - 1].gettostoreInstance());
    }

    private void colordebugHandleTrainBall(final int location) {
        if (ballcolors[location - 1] != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(BallColorTrainner.this);
            builder.setMessage("Train this Data?").
                    setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            balltrainnerhelper(location, ballcolors[location - 1]);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Do Nothing
                        }
                    });
            builder.create().show();
        }
    }


    public void color_debug_get_all_ball_color(View view) {
        checkall = true;
        colordebugGetBallColorSender(1);

    }

    public void color_debug_get_ball_color_at_1(View view) {
        colordebugGetBallColorSender(1);
    }

    public void color_debug_get_ball_color_at_2(View view) {
        colordebugGetBallColorSender(2);
    }

    public void color_debug_get_ball_color_at_3(View view) {
        colordebugGetBallColorSender(3);
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
