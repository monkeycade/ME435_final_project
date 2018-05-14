package edu.rose_hulman.jins.script_runing;

import edu.rose_hulman.jins.final_project_main.MainCommandBin;


public class ScriptRunHandler implements Runnable {

    String mString;
    Scripts mScript;

    MainCommandBin msystem;

    public ScriptRunHandler(String input, MainCommandBin system) {
        mString = input;
        msystem = system;
        mScript = null;
    }

    public ScriptRunHandler(String command, Scripts input, MainCommandBin system) {
        mScript = input;
        msystem = system;

        mString = command;
    }

    @Override
    public void run() {
        msystem.sendCommand(mString);
        if (mScript != null) {
            mScript.handleScript(null);
        }
    }
}