package com.assignment.locationnotifier;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesConfig {
    private final SharedPreferences sharedPreferences;
    private final Context context;

    public SharedPreferencesConfig(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("com.assignment.locationnotifier.Data_preferences", Context.MODE_PRIVATE);
    }

    public void writeLocation(String latlng) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("location", latlng);
        editor.apply();
    }

    public String readLocation() {
        return sharedPreferences.getString("location",null);
    }

    public void writeRadius(String radius){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("radius",radius);
        editor.apply();
    }

    public String readRadius(){
        return sharedPreferences.getString("radius","null");
    }
}