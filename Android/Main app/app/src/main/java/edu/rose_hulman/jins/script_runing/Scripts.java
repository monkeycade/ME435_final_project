package edu.rose_hulman.jins.script_runing;

import java.util.ArrayList;
import java.util.HashMap;

import edu.rose_hulman.jins.final_project_main.MainCommandBin;
import edu.rose_hulman.jins.final_project_main.R;
import edu.rose_hulman.jins.storage.CSVReading;

public class Scripts {
    MainCommandBin msystem;

    HashMap<String,Script> mscripts;
    boolean isinit;
    public Scripts(MainCommandBin s){
        msystem = s;
        isinit = false;

        mscripts = new HashMap<>();

        PositionDirectory tempD = new PositionDirectory(msystem.getResources().openRawResource(R.raw.positiondirect));
        ArrayList<String[]> scripts_name = new CSVReading(msystem.getResources().openRawResource(R.raw.scripts_name)).read();

        for(int i = 0; i < scripts_name.size(); i++){
            String scriptName = scripts_name.get(i)[0];
            int id = msystem.getResources().getIdentifier(scriptName,"raw",msystem.getPackageName());
            //Check if the file present
            if(id == 0){
                msystem.system_print("ERROR: The script: " + scriptName + " is not present in the raw folder");
            }else {
                //Check for duplication
                if (mscripts.containsKey(scriptName)) {
                    msystem.system_print("ERROR: The " + scriptName + " is duplicate in the scripts_name");
                } else {
                    Script temp = new Script(msystem.getResources().openRawResource(id),msystem,tempD,this);
                    if (!temp.isBuildSuccess){
                        msystem.system_print("The script " + scriptName + " is not build successful");
                    }
                    mscripts.put(scriptName.toLowerCase(), temp);
                }
            }
        }


    }

    public boolean runScript(String script_Name){
        if(!isinit){
            msystem.sendCommand("ATTACH 111111");
            msystem.sendPostDelayCommand("ATTACH 111111",1100);
            isinit = true;
        }
        if (mscripts.containsKey(script_Name.toLowerCase())){
            mscripts.get(script_Name.toLowerCase()).run();
            return true;
        }else{
            return false;
        }
    }

    public boolean hasScript(String script_Name) {
        return mscripts.containsKey(script_Name.toLowerCase());
    }
}
