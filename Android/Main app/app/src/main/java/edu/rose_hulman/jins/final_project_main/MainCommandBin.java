package edu.rose_hulman.jins.final_project_main;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ViewFlipper;

import edu.rose_hulman.jins.script_runing.Script;
import edu.rose_hulman.jins.script_runing.ScriptRunHandler;
import edu.rose_hulman.jins.script_runing.Scripts;
import edu.rose_hulman.me435Library.RobotActivity;

public class MainCommandBin extends RobotActivity {

    final static int NUMBER_OF_DEBUGGER_LINE = 20;


    TextView mOutput;

    ViewFlipper mViewControl;
    Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set up the main Screen
        setContentView(R.layout.main_display_frame);

        //Set up the debug output console
        mOutput = findViewById(R.id.main_output);

        //Set up the View Flipper
        mViewControl = findViewById(R.id.view_controller);

        //Set up handler for delay process;
        mHandler = new Handler();
    }

    public void system_print(String text) {
        Log.d("logout", text);
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

    public void sendPostDelayCommand(String Command, long time){
        mCommandHandler.postDelayed(new ScriptRunHandler(Command,this), time);
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
                new Scripts(this).runScript("randomscript");
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

    @Override
    public void sendCommand(String commandString) {
        super.sendCommand(commandString);
        Log.d("Script", commandString);
        system_print(commandString);
    }

    public void postDelayScript(Script script, int time) {
        mCommandHandler.postDelayed(new ScriptRunHandler(script,this), time);
    }
}

