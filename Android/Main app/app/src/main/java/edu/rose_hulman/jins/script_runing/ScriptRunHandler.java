package edu.rose_hulman.jins.script_runing;

import edu.rose_hulman.jins.final_project_main.MainCommandBin;


public class ScriptRunHandler implements Runnable {

    String mString;
    Script mScript;

    MainCommandBin msystem;

    public ScriptRunHandler(String input, MainCommandBin system) {
        mString = input;
        msystem = system;
    }

    public ScriptRunHandler(Script input, MainCommandBin system) {
        mScript = input;
        msystem = system;

        mString = null;
    }

    @Override
    public void run() {
        if (mString != null) {
            msystem.sendCommand(mString);
        }else{
            msystem.sendCommand(mScript.handleNext());

        }
    }
}