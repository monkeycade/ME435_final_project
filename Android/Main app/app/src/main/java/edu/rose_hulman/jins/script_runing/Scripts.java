package edu.rose_hulman.jins.script_runing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import edu.rose_hulman.jins.final_project_main.MainCommandBin;
import edu.rose_hulman.jins.final_project_main.R;
import edu.rose_hulman.jins.storage.CSVReading;

public class Scripts {
    private MainCommandBin msystem;

    private HashMap<String, Script> mscripts;

    private Stack<Script> runningSet;

        boolean isinit;
    public Scripts(MainCommandBin s) {
        msystem = s;
        isinit = false;
        runningSet = new Stack<>();

        mscripts = new HashMap<>();

        PositionDirectory tempD = new PositionDirectory(msystem.getResources().openRawResource(R.raw.positiondirect));
        ArrayList<String[]> scripts_name = new CSVReading(msystem.getResources().openRawResource(R.raw.scripts_name)).read();

        for (int i = 0; i < scripts_name.size(); i++) {
            String scriptName = scripts_name.get(i)[0];
            int id = msystem.getResources().getIdentifier(scriptName, "raw", msystem.getPackageName());
            //Check if the file present
            if (id == 0) {
                msystem.system_print("ERROR: The script: " + scriptName + " is not present in the raw folder");
            } else {
                //Check for duplication
                if (mscripts.containsKey(scriptName)) {
                    msystem.system_print("ERROR: The " + scriptName + " is duplicate in the scripts_name");
                } else {
                    Script temp = new Script(msystem.getResources().openRawResource(id), msystem, tempD, this);
                    if (!temp.isBuildSuccess) {
                        msystem.system_print("The script " + scriptName + " is not build successful");
                    }
                    mscripts.put(scriptName.toLowerCase(), temp);
                }
            }
        }


    }

    public boolean runScript(String script_Name) {
//        should be handle on the Arduino side now
        if(!isinit){
            msystem.sendCommand("ATTACH 111111");
            msystem.sendPostDelayCommand("ATTACH 111111",1100);
            isinit = true;
        }
        if (mscripts.containsKey(script_Name.toLowerCase())) {
            if (runningSet.isEmpty()) {
                mscripts.get(script_Name.toLowerCase()).reset();
                runningSet.push(mscripts.get(script_Name.toLowerCase()));
                handleScript(null);
            } else {
                return false;
            }
            return true;
        } else {
            return false;
        }

    }

    protected void handleScript(String script_Name) {
        if (script_Name != null) {
            mscripts.get(script_Name.toLowerCase()).reset();
            runningSet.push(mscripts.get(script_Name.toLowerCase()));
            handleScript(null);
            return;
        }
        Script current = runningSet.peek();
        while (!current.hasNext()) {
            runningSet.pop();
            if (runningSet.isEmpty()) {
                msystem.onScricptsComplete();
                return;
            }
            current = runningSet.peek();
        }
        int delay = current.getcurrest_step_delay();
        String command = current.handleNext();
        if (command != null) {
            msystem.postDelayScript(this, delay, command);

        }
    }

    public boolean hasScript(String script_Name) {
        return mscripts.containsKey(script_Name.toLowerCase());
    }
}
