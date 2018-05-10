package edu.rose_hulman.jins.script_runing;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import edu.rose_hulman.jins.storage.CSVReading;

public class PositionDirectory {

    private HashMap<String, String> directory;

    public PositionDirectory(InputStream myStream){
        CSVReading reader = new CSVReading(myStream);
        ArrayList<String[]> rawData = reader.read();
        directory = new HashMap<>();
        for(String[] row : rawData){
            StringBuilder special = new StringBuilder();
            special.append("POSITION");
            for(int i=1; i<row.length; i++){
                special.append(" ");
                special.append(row[i]);
            }
            directory.put(row[0],special.toString());
        }
    }

    public boolean isCommand(String command){
        return directory.containsKey(command);
    }

    public String translate(String command){
        return directory.get(command);
    }
}
