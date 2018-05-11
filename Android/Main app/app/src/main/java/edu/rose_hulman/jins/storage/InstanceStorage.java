package edu.rose_hulman.jins.storage;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;

import com.google.gson.Gson;

import java.security.Key;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import edu.rose_hulman.jins.final_project_main.MainCommandBin;

public class InstanceStorage {
    SharedPreferences mstorage;

    public InstanceStorage(MainCommandBin system, String name) {
        mstorage = system.getSharedPreferences("com.rose-hulman.jins." + name, Context.MODE_PRIVATE);
    }

    public void store(String key, int value) {
        SharedPreferences.Editor editor = mstorage.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void store(String key, float value) {
        SharedPreferences.Editor editor = mstorage.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    public void store(String key, String value) {
        SharedPreferences.Editor editor = mstorage.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void store(String key, boolean value) {
        SharedPreferences.Editor editor = mstorage.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public void store(String key, long value) {
        SharedPreferences.Editor editor = mstorage.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public void store(String key, HashMap map) {
//        //Try serializable
//        StringBuilder tostore = new StringBuilder();
//        Iterator forCheck = map.keySet().iterator();
//
//        tostore.append("HashMap ");
//        if(!forCheck.hasNext()){
//            error("The HashMap Don't Contain any Key");
//            return;
//        }else {
//            Object k = forCheck.next();
//            if (!(k instanceof String)) {
//                error("The HashMap Key is not the String, this method only works for String Key");
//                return;
//            }
//
//            if (map.get(k) instanceof String) {
//                tostore.append("String ");
//            }else if(map.get(k) instanceof Integer) {
//                tostore.append("Integer ");
//            }else{
//                error("The HashMap Value is not the String or Integer, this method only works for those Value");
//                return;
//            }
//
//        }
//        for (Object k : map.keySet()) {
//            tostore.append(k);
//            tostore.append(" ");
//            tostore.append(map.get(k));
//            tostore.append(" ");
//        }
//        store(key, tostore.toString());
        Gson test = new Gson();
        store(key,test.toJson(map));

    }

    public int readint(String key) {
        return mstorage.getInt(key, 0);
    }

    public float readfloat(String key) {
        return mstorage.getFloat(key, 0);
    }

    public String readString(String key) {
        return mstorage.getString(key, "");
    }

    public boolean readboolean(String key) {
        return mstorage.getBoolean(key, false);
    }

    public long readlong(String key) {
        return mstorage.getLong(key, 0);
    }

    public HashMap readIntergerMap(String key){
        Gson gson = new Gson();
        String jsonText = readString(key);
        if(jsonText == ""){
            return null;
        }
        HashMap<String,Integer> toReturn = (HashMap<String,Integer>) gson.fromJson(jsonText, HashMap.class);
        return toReturn;
    }

    public HashMap readStringMap(String key){
        Gson gson = new Gson();
        String jsonText = readString(key);
        if(jsonText == ""){
            return null;
        }
        HashMap<String,String> toReturn = (HashMap<String,String>) gson.fromJson(jsonText, HashMap.class);
        return toReturn;
    }

    private void error(String toReport) {

    }

}
