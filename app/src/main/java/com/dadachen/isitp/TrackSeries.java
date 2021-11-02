package com.dadachen.isitp;

import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;

public class TrackSeries implements XYSeries {
    private ArrayList<Number> dataX;
    private ArrayList<Number> dataY;
    private String title;

    public TrackSeries(String title) {
        dataX = new ArrayList<>();
        dataY = new ArrayList<>();
        this.title = title;
    }
    public void appendData(Float x,Float y){
        dataX.add(x);
        dataY.add(y);
    }
    public void changeData(double[][] positions){
        dataX.clear();
        dataY.clear();
        for(double[] position : positions){
            dataX.add((float)position[0]);
            dataY.add((float)position[1]);
        }
    }

    @Override
    public int size() {
        return dataX.size();
    }

    @Override
    public Number getX(int index) {
        return dataX.get(index);
    }

    @Override
    public Number getY(int index) {
        return dataY.get(index);
    }

    @Override
    public String getTitle() {
        return title;
    }
}
