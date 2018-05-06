package edu.rose_hulman.jins.final_project_main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import edu.rose_hulman.me435Library.RobotActivity;

public class MainActivity extends RobotActivity {

    final static int NUMBER_OF_DEBUGGER_LINE = 3;
    TextView mOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Set up for the initial frame
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_display_frame);

        //Set up the tool bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Setup the reset button
        mOutput = findViewById(R.id.main_output);
        system_print("App start to initialize");
        FloatingActionButton e_stop = findViewById(R.id.e_stop);
        e_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                system_print("TODO Add reset button");
            }
        });

        //Check for the camera status
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            system_print("Camera is ready for use");
        } else {
            system_print("Request for the camera permission");
            String[] CAMERA_PERMISSONS = {
                    Manifest.permission.CAMERA
            };
            ActivityCompat.requestPermissions(this, CAMERA_PERMISSONS, 0);
        }
    }

    public void system_print(String text) {
        String[] temps = mOutput.getText().toString().split("\n");
        if(temps.length == 1 && temps[0] == ""){
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
                system_print("TODO: add a FSM frame");
                return true;
            default:
                system_print("Unidentified menu button");
                return super.onOptionsItemSelected(item);

        }


    }
}
