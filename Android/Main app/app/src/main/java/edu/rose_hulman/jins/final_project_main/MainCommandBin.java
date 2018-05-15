package edu.rose_hulman.jins.final_project_main;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.HashMap;

import edu.rose_hulman.jins.script_runing.Script;
import edu.rose_hulman.jins.script_runing.ScriptRunHandler;
import edu.rose_hulman.jins.script_runing.Scripts;
import edu.rose_hulman.jins.storage.InstanceStorage;
import edu.rose_hulman.me435Library.RobotActivity;

public class MainCommandBin extends RobotActivity {

    final static int NUMBER_OF_DEBUGGER_LINE = 30;
    final static int HEIGHT_WHEN_AT_MAIN_SCREEN = 750;


    private TextView mOutput;
    protected InstanceStorage mStorage;
    public ViewFlipper mViewControl;
    private ScrollView mScrollView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set up the main Screen
        setContentView(R.layout.main_display_frame);

        //Set up the debug output console
        mOutput = findViewById(R.id.main_output);

        //Set up the View Flipper
        mViewControl = findViewById(R.id.view_controller);

        //Set up the Scroll for debug view
        mScrollView = findViewById(R.id.debug_output_scrollable);

        mStorage = new InstanceStorage(this,"main");

        ViewGroup.LayoutParams para = mScrollView.getLayoutParams();
        para.height = HEIGHT_WHEN_AT_MAIN_SCREEN;
        mScrollView.setLayoutParams(para);

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
        scrollDown();
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


        ViewGroup.LayoutParams para = mScrollView.getLayoutParams();
        para.height = 50;
        switch (item.getItemId()) {
            case R.id.toDebug:
                para.height = HEIGHT_WHEN_AT_MAIN_SCREEN;
                mViewControl.setDisplayedChild(0);
                system_print("Flip to Main Activity");
                break;
            case R.id.toFSM:
                mViewControl.setDisplayedChild(1);
                system_print("Flip to FSM");
                break;
            case R.id.toBallColorTrainner:
                mViewControl.setDisplayedChild(2);
                system_print("Flip to Ball Color Trainer");
                break;
            case R.id.toImagRecTrainner:
                mViewControl.setDisplayedChild(3);
                system_print("Flip to Image Rec Trainer");
                break;
            default:
                system_print("Unidentified menu button");
                return super.onOptionsItemSelected(item);

        }
        mScrollView.setLayoutParams(para);
        scrollDown();
        return true;
    }

    @Override
    public void sendCommand(String commandString) {
        if(commandString!= null) {
            super.sendCommand(commandString);
            system_print(commandString);
        }
    }

    @Override
    protected void onCommandReceived(String receivedCommand) {
        super.onCommandReceived(receivedCommand);
        system_print(receivedCommand);
    }

    public void postDelayScript(Scripts script, int time,String command) {
        mCommandHandler.postDelayed(new ScriptRunHandler(command, script,this), time);
    }
    public void scrollDown() {
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(mScrollView.FOCUS_DOWN);
            }
        },50);
    }

    public void onScricptsComplete() {
        system_print("The script all complete");
    }
}

