package edu.rose_hulman.jins.final_project_main;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import edu.rose_hulman.me435Library.RobotActivity;


public class MainActivity extends RobotActivity {

    TextView mOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_display_frame);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mOutput = findViewById(R.id.main_output);
        FloatingActionButton fab = findViewById(R.id.e_stop);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                system_print("TODO Add reset button");
            }
        });
    }

    public void system_print(String text) {
        mOutput.setText(text);
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
