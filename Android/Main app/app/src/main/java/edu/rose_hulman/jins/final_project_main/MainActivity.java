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
import android.widget.ViewFlipper;

import edu.rose_hulman.jins.fsm_main.FSM_System;
import edu.rose_hulman.me435Library.RobotActivity;

public class MainActivity extends FSM_System {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Set up for the initial frame
        super.onCreate(savedInstanceState);

        //Set up the tool bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Setup the reset button
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
        system_print("App finish initialize");
    }




}
