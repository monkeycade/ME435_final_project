package edu.rose_hulman.jins.final_project_main;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ViewFlipper;

import edu.rose_hulman.me435Library.RobotActivity;

public class MainCommandBin extends RobotActivity {

    final static int NUMBER_OF_DEBUGGER_LINE = 3;


    TextView mOutput;

    ViewFlipper mViewControl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set up the main Screen
        setContentView(R.layout.main_display_frame);

        //Set up the debug output console
        mOutput = findViewById(R.id.main_output);

        //Set up the View Flipper
        mViewControl = findViewById(R.id.view_controller);
    }

    public void system_print(String text) {
        String[] temps = mOutput.getText().toString().split("\n");
        if(temps.length == 1 && temps[0].equals("")){
            mOutput.setText(text);
            return;

        }
        StringBuilder output = new StringBuilder();
        for (int i = temps.length - NUMBER_OF_DEBUGGER_LINE + 1 < 0 ? 0 : temps.length - NUMBER_OF_DEBUGGER_LINE + 1; i < temps.length; i++){
            output.append(temps[i]);
            output.append("\n");
        }
        output.append(text);
        mOutput.setText(output.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.toDebug:
                system_print("TODO: add a Debug frame");
                return true;
            case R.id.toFSM:
                mViewControl.setDisplayedChild(1);
                system_print("Flip to FSM");
                return true;
            default:
                system_print("Unidentified menu button");
                return super.onOptionsItemSelected(item);

        }
    }
}
