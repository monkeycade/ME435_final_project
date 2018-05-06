package edu.rose_hulman.jins.script_runing;

import edu.rose_hulman.jins.final_project_main.MainCommandBin;


public class ScriptRunHandler implements Runnable{

    String ms;
    MainCommandBin msystem;

    public ScriptRunHandler(String s, MainCommandBin system){
        ms = s;
        msystem = system;
    }

    @Override
    public void run() {
        msystem.sendCommand(ms);
    }
}