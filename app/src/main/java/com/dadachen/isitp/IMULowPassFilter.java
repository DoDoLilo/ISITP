package com.dadachen.isitp;

public class IMULowPassFilter {

    private int N;
    private float[] h;

    public IMULowPassFilter(float[] h) {
        this.h = h;
        this.N = h.length;
        x = new float[N];
    }

    private int n = 0;
    private float[] x;

    public float filter(float x_in)
    {
        float y = 0.0f;

        // Store the current input, overwriting the oldest input
        x[n] = x_in;

        // Multiply the filter coefficients by the previous inputs and sum
        for (int i=0; i<N; i++)
        {
            y += h[i] * x[((N - i) + n) % N];
        }

        // Increment the input buffer index to the next location
        n = (n + 1) % N;

        return y;
    }

    public float[] filter(float[] data){
        float[] out = new float[data.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = filter(data[i]);
        }
        return out;
    }

}
