package edu.rose_hulman.jins.storage;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Parcel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import edu.rose_hulman.jins.ball_color_detector.BallColorDetector;
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
        Gson test = new Gson();
        store(key,test.toJson(map));

    }

    public void store(String key, Location location) {
        Gson test = new Gson();
        store(key,test.toJson(location));
    }

    public void store(String key, List dataList) {
        Gson test = new Gson();
        store(key,test.toJson(dataList));
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


    public Location readLocation(String key) {
        Gson gson = new Gson();
        String jsonText = readString(key);
        if(jsonText == ""){
            return null;
        }
        Location toReturn = gson.fromJson(jsonText, Location.class);
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

    public ArrayList<BallColorDetector.Instance> readInstanceList(String key){
        Gson gson = new Gson();
        String jsonText = readString(key);
        if(jsonText == ""){
            return null;
        }
        ArrayList<BallColorDetector.Instance> toReturn = gson.fromJson(jsonText, new TypeToken<ArrayList<BallColorDetector.Instance>>(){}.getType());
        return toReturn;
    }

    public ArrayList<Double> readColorCoeffList(String key){
        Gson gson = new Gson();
        String jsonText = readString(key);
        if(jsonText == ""){
            return null;
        }
        ArrayList<Double> toReturn = gson.fromJson(jsonText, new TypeToken<ArrayList<Double>>(){}.getType());
        return toReturn;
    }
}
