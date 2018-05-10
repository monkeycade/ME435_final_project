package edu.rose_hulman.jins.script_runing;

import java.io.InputStream;
import java.util.ArrayList;

import edu.rose_hulman.jins.final_project_main.MainCommandBin;
import edu.rose_hulman.jins.final_project_main.R;
import edu.rose_hulman.jins.storage.CSVReading;

public class Script {

    private PositionDirectory mDirectory;
    private MainCommandBin msystem;
    private Scripts mSet;
    private String[] mCommands;
    private int[] mDelays;
    private int current_step;
    protected boolean isBuildSuccess;
    int command_length;

    public Script(InputStream inStream, MainCommandBin inActivity, PositionDirectory inDirectory,Scripts inScripts) {
        mDirectory = inDirectory;
        msystem = inActivity;
        mSet = inScripts;
        CSVReading reader = new CSVReading(inStream);
        ArrayList<String[]> temp = reader.read();
        mCommands = new String[temp.size()];
        mDelays = new int[temp.size()];

        isBuildSuccess =  true;
        command_length = 0;
        for (int c = 0; c < temp.size(); c++) {
            String command = temp.get(c)[0];
            StringBuilder tem = new StringBuilder();
            if (isValidCommand(command,tem)) {
                mCommands[command_length] = command;
                String valueString = temp.get(c)[1];
                int basic = 10000;
                try {
                    basic = Integer.parseInt(valueString);
                } catch (NumberFormatException e) {
                    msystem.system_print("ERROR: Failure to read time value at line " + (c + 1) + "\n The string we get is " + valueString);
                } catch (NullPointerException e) {
                    msystem.system_print("ERROR: There is no time value at line " + (c + 1));
                }
                mDelays[command_length] = basic;
                command_length++;
            }else{
                isBuildSuccess = false;
                msystem.system_print(tem.toString() + "at line " + (c + 1));
            }
        }
    }

    private boolean isValidCommand(String mCommand,StringBuilder tem) {
        String[] commands = seperateCommand(mCommand);
        String identify = commands[0];
        String detail = commands[1];
        if (identify == null){
            tem.append("ERROR: The command is not exist ");
            return false;
        }
        if (identify.equalsIgnoreCase("position")){
            tem.append("ERROR: it is not a possible position command ");
            return mDirectory.isCommand(detail);
        }else if(identify.equalsIgnoreCase("script")) {
            tem.append("ERROR: Not have the script " + detail);
            return mSet.hasScript(detail);
        }else if(identify.equalsIgnoreCase("gripper")){
            tem.append("ERROR: The Gripper value is unreasonable");
            int t;
            try {
               t =  Integer.parseInt(detail);
            }catch (Exception e){
                return false;
            }
            return t > 20 && t < 60;
        }else{
            tem.append("ERROR: it is not a reasonable command ");
            return false;

        }

    }

    private String[] seperateCommand(String commandLine) {
        String[] output = new String[2];
        String[] input = commandLine.split(" ");
        if (input.length > 1) {
            output[0] = input[0];
            StringBuilder temp = new StringBuilder();
            for (int i = 1; i < input.length; i++){
                temp.append(input[i]);
                temp.append(" ");
            }
            output[1] = temp.substring(0,temp.length() - 1).toString();
        }
        return output;
    }

    public void run(){
        this.reset();
        msystem.postDelayScript(this,mDelays[current_step]);
    }

    public void reset() {
        current_step = 0;
    }

    public String handleNext() {
        current_step++;
        String[] temp = seperateCommand(mCommands[current_step - 1]);
        String toSDKCommand;

        if(temp[0].equalsIgnoreCase("position")){
            toSDKCommand = mDirectory.translate(temp[1]);
        }else if (temp[0].equalsIgnoreCase("script")){
            mSet.runScript(temp[1]);
            return null;
        }else if (temp[0].equalsIgnoreCase("gripper")){

            toSDKCommand = msystem.getResources().getString(R.string.gripper_command,Integer.parseInt(temp[1]));

        }else{
            toSDKCommand = null;

        }
        if (current_step < command_length) {
            msystem.postDelayScript(this, mDelays[current_step]);
        }
        return toSDKCommand;
    }
}