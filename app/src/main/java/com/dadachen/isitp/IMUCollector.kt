package com.dadachen.isitp

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class IMUCollector(private val context: Context, private val modulePartial:(FloatArray)->Unit) {
    private val gyro = FloatArray(3)
    private val acc = FloatArray(3)
    private val rotVector = FloatArray(4)
    private lateinit var sensorManager: SensorManager
    private var rotVSensor: Sensor? = null
    private var accVSensor: Sensor? = null
    private var gyroVSensor: Sensor? = null
    val data = Array(6) {
        FloatArray(200)
    }

    fun start() {
        initSensor()
        isRunning = true
        thread(start = true) {
            var index = 0
            while (isRunning) {
                if (index == 200) {
                    forward()
                    index = 0
                }
                fillData(index++)
                Thread.sleep(5)
            }
        }
    }

    private fun fillData(index: Int) {
        data[0][index] = acc[0]
        data[1][index] = acc[1]
        data[2][index] = acc[2]
        data[3][index] = gyro[0]
        data[4][index] = gyro[1]
        data[5][index] = gyro[2]
    }


    fun stop() {
        isRunning = false
        stopSensor()
    }

    private val filters = Array(6) {
        IMULowPassFilter(FilterConstant.para)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private fun forward() {
        //low-pass filter need parameters from MatLab
        val tData = data.copyOf()
        coroutineScope.launch{
            val tempoData = FloatArray(1200)
            tData.forEachIndexed { index, floatArray ->
                filters[index].filter(floatArray).copyInto(tempoData,index*200)
            }
            modulePartial(tempoData)
        }
    }

    var isRunning = false

    private val rotl = object : SensorEventListener {
        @SuppressLint("SetTextI18n")
        override fun onSensorChanged(p0: SensorEvent?) {
            rotVector[0] = p0!!.values[0]
            rotVector[1] = p0.values[1]
            rotVector[2] = p0.values[2]
            rotVector[3] = p0.values[3]

        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            Log.d("imu", "rot accuracy changed")
        }
    }
    private val gyrol = object : SensorEventListener {
        override fun onSensorChanged(p0: SensorEvent?) {
            gyro[0] = p0!!.values[0]
            gyro[1] = p0.values[1]
            gyro[2] = p0.values[2]
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            Log.d("imu", "gyro accuracy changed")
        }
    }
    private val accl = object : SensorEventListener {
        override fun onSensorChanged(p0: SensorEvent?) {
            acc[0] = p0!!.values[0]
            acc[1] = p0.values[1]
            acc[2] = p0.values[2]
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            Log.d("imu", "acc accuracy changed")
        }
    }

    private fun initSensor() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotVSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        accVSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroVSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
        sensorManager.registerListener(rotl, rotVSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(accl, accVSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(gyrol, gyroVSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun stopSensor() {
        sensorManager.unregisterListener(accl)
        sensorManager.unregisterListener(gyrol)
        sensorManager.unregisterListener(rotl)
    }
}