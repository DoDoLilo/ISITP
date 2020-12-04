package com.dadachen.isitp

import android.content.Context

class IMUCollector(val context: Context) {
    val data = Array<FloatArray>(6){
        FloatArray(200)
    }
    fun start(){
        isRunning = true
    }
    fun stop(){
        isRunning = false
    }
    val lowpassFilters = Array<IMULowPassFilter>(6){
        IMULowPassFilter(FilterConstant.para)
    }
    fun setProcessListener(forward:(FloatArray)->Unit){
        //1st get IMU data
        //2nd low-pass filter
        //lowpass filter need parameters from mathlab
        val tempoData = FloatArray(1200)
        for (i in 0 until 6) {
            val t = lowpassFilters[i].filter(data[i])
            t.copyInto(tempoData,i*200)
        }
        //3rd using the forward method
        forward(tempoData)
    }

    var isRunning = false

}