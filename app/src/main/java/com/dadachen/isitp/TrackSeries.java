package com.dadachen.isitp;

import com.androidplot.xy.XYSeries;

import java.util.ArrayList;

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
