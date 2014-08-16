package com.example.musicmood;

import android.util.Log;

/**
 * Created by tangkk on 28/7/14.
 */
public class Show {
    public static void show(double[] input, String title) {
        for (int i = 0; i < input.length; i++) {
            Log.i("SHOW", title + " " + i + "=" + input[i]);
        }
    }

    public static void show(float[] input, String title) {
        for (int i = 0; i < input.length; i++) {
            Log.i("SHOW", title + " " + i + "=" +input[i]);
        }
    }

    public static void show(int[] input, String title) {
        for (int i = 0; i < input.length; i++) {
            Log.i("SHOW", title + " " + i + "=" +input[i]);
        }
    }

    public static void show(String title) {
        Log.i("SHOW", title);
    }

    public static void show(double input, String title) {
        Log.i("SHOW", title + "=" + input);
    }
}
