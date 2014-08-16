package com.example.musicmood;

/**
 * Created by tangkk on 28/7/14.
 */
import java.util.Arrays;

public class MedianFilter {

    public float[] filter(float[] input, int N) {
        if (N % 2 == 0)
            N = N + 1; // make sure N(length of filter) is odd to simplify

        float[] output = new float[input.length];

        float[] window = new float[N];
        int numberofElements;
        int halfN = (N-1)/2;
        for (int i = 0; i < input.length; i++) {
            //System.out.println("i "+i);
            for (int j = 0; j < N; j++) {
                //System.out.println("j... "+j);
                if (i + j - halfN < 0)
                    window[j] = 0;
                else {
                    if (i + j - halfN >= input.length)
                        window[j] = 0;
                    else
                        window[j] = input[i+j-halfN];
                }
            }
            Arrays.sort(window);
            //show(window, "window...");
            output[i] = (float)window[(N - 1)/2];
        }

        return output;
    }

    public double[] filter(double[] input, int N) {
        if (N % 2 == 0)
            N = N + 1; // make sure N(length of filter) is odd to simplify

        double[] output = new double[input.length];

        double[] window = new double[N];
        int numberofElements;
        int halfN = (N-1)/2;
        for (int i = 0; i < input.length; i++) {
            //System.out.println("i "+i);
            for (int j = 0; j < N; j++) {
                //System.out.println("j... "+j);
                if (i + j - halfN < 0)
                    window[j] = 0;
                else {
                    if (i + j - halfN >= input.length)
                        window[j] = 0;
                    else
                        window[j] = input[i+j-halfN];
                }
            }
            Arrays.sort(window);
            //show(window, "window...");
            output[i] = window[(N - 1)/2];
        }

        return output;
    }

    private static void show (double[]input, String title) {
        System.out.println(title);
        for(int i=0; i<input.length; i++)
            System.out.println(input[i] + " ");
    }

    public static void main (String[] args) {
        int L = Integer.parseInt(args[0]);
        int N = Integer.parseInt(args[1]);
        double[] input = new double[L];
        for (int i = 0; i < L; i++) {
            input[i] = Math.random();
        }
        show(input, "input");

        MedianFilter MF = new MedianFilter();

        double[] output = new double[L];
        output = MF.filter(input, N);
        show(output, "output");
    }

}

