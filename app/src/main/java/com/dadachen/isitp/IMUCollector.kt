 package com.dadachen.isitp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.concurrent.thread


class IMUCollector(private val context: Context, private val modulePartial: (FloatArray) -> Unit) {
    private val gyro = FloatArray(3)
    private val acc = FloatArray(3)
    private val rotVector = FloatArray(4)
    private lateinit var sensorManager: SensorManager
    private var rotVSensor: Sensor? = null
    private var accVSensor: Sensor? = null
    private var gyroVSensor: Sensor? = null
    private val data = Array(6) {
        FloatArray(FRAME_SIZE)
    }

    private val currentLoc = floatArrayOf(0f, 0f)

    private enum class Status {
        Running, Idle
    }

    fun start() {
//        loadDataByCsvFile()
        initSensor()
        status = Status.Running
        stringBuilder.clear()
        //start thread
        thread(start = true) { //创建一个thread并运行指定代码块，()->Unit
            var index=0
            var index2=0
            while(index<200){
                fillData(index++,index2++)
                Thread.sleep(FREQ_INTERVAL)
            }
            module = Module.load(cn.whu.cs.niu.PDR.Utils.assetFilePath(context, "mobile_model.ptl"))
            while (status == Status.Running) {
                if (index == FRAME_SIZE) {
                    val tData = data.copyOf()
                    //estimation by using 200 frames IMU-sensor
                    estimate(tData,0,index2)
                    //next step reset offset to zero
                    index = 0
                } else if (index % STEP == 0) { //每10*5ms进行一输出
                    //note index is always more than 1
                    val tData = data.copyOf()
                    estimate(tData,index,index2)
                }
                fillData(index++,index2++)

                Thread.sleep(FREQ_INTERVAL) //这里控制了sleep 5ms，即200Hz
            }
        }
    }

    private fun changeTheAxisOfAccAndGyro(acc0:Float,acc1:Float,acc2:Float,gyro0:Float,gyro1: Float,gyro2: Float,
    rot0:Float,rot1: Float,rot2: Float,rot3: Float): FloatArray {
//        change the acc and the gyro to the same axis by using the rot
//        1.change the rotVector array
//        val rotChanged = floatArrayOf(rotVector[3], rotVector[0], rotVector[1], rotVector[2])
        val rotQuaternion= cn.whu.cs.niu.PDR.Quaternion(rot3, rot0, rot1, rot2)
        val gyroQuaternion= cn.whu.cs.niu.PDR.Quaternion(0f, gyro0, gyro1, gyro2)
        val accQuaternion= cn.whu.cs.niu.PDR.Quaternion(0f, acc0, acc1, acc2)
//        2.use the Quaternion functions
        val gyroChanged=rotQuaternion.times(gyroQuaternion).times(rotQuaternion.conjugate()).toFloatArray()
        val accChanged=rotQuaternion.times(accQuaternion).times(rotQuaternion.conjugate()).toFloatArray()
//        3.past the values to acc and gyro
        val gyroAccChanged= floatArrayOf(gyroChanged[1],gyroChanged[2],gyroChanged[3],accChanged[1],accChanged[2],accChanged[3])
        return gyroAccChanged
    }

    private fun fillData(index: Int, index2: Int=0) {
//        acc gyro rot
//        val gyroAccChanged=changeTheAxisOfAccAndGyro(
//            seqList[index2][0],seqList[index2][1],seqList[index2][2], //acc
//            seqList[index2][3],seqList[index2][4],seqList[index2][5],  //gyro
//            seqList[index2][6],seqList[index2][7],seqList[index2][8],seqList[index2][9]) //rot
        val gyroAccChanged=changeTheAxisOfAccAndGyro(
            acc[0],acc[1],acc[2], //acc
            gyro[0],gyro[1],gyro[2],  //gyro
            rotVector[0],rotVector[1],rotVector[2],rotVector[3]) //rot
//        val gyroAccChanged=changeTheAxisOfAccAndGyro(acc[0],acc[1],acc[2],gyro[0],gyro[1],gyro[2],
//        rotVector[0],rotVector[1],rotVector[2],rotVector[3])
        //the module input [0-2]:gyro  [3-5]:acc
        data[0][index] = gyroAccChanged[0]
        data[1][index] = gyroAccChanged[1]
        data[2][index] = gyroAccChanged[2]
        data[3][index] = gyroAccChanged[3]
        data[4][index] = gyroAccChanged[4]
        data[5][index] = gyroAccChanged[5]
        if (FilterConstant.RECORD_CSV){
            stringBuilder.append("${data[0][index]}, ${data[1][index]}, ${data[2][index]}, ${data[3][index]}, ${data[4][index]}, ${data[5][index]}\n")
        }
    }


//    private fun checkGestureAndSwitchModule(tData: Array<FloatArray>) {
//        checkGesture(tData)
//        val modulePath = when (gestureType) {
//            GestureType.Hand -> {
//                //need to be replaced
//                "mobile_model.ptl"
//            }
//            GestureType.Pocket -> {
//                "mobile_model.ptl"
//            }
//            else -> {
//                "mobile_model.ptl"
//            }
//        }
//        module = Module.load(cn.whu.cs.niu.PDR.Utils.assetFilePath(context, modulePath))
//        println(modulePath)
//    }

//    private fun checkGesture(tData: Array<FloatArray>) {
//        coroutineScope.launch {
//            val gdata = FloatArray(192*6)
//            for (i in 0 until 192){
//                for (j in 0 until 6) {
//                    gdata[i*6+j] = tData[j][i]
//                }
//            }
//            gestureType = gestureClassifier.forward(gdata)
//            gestureTypeListener(gestureType)
//        }
//    }
//    private val gestureClassifier = GestureClassifier(cn.whu.cs.niu.PDR.Utils.assetFilePath(context, "gesture_3.pt"))

    fun stop() {
        status = Status.Idle
//        if(FilterConstant.RECORD_CSV){
//            cn.whu.cs.niu.PDR.Utils.writeToLocalStorage("${context.externalCacheDir}/IMU-${System.currentTimeMillis()}.csv", stringBuilder.toString())
//        }
        stopSensor()
    }

    private val filters = Array(6) {
        IMULowPassFilter(FilterConstant.para)
    }
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private lateinit var module: Module
    private fun  estimate(tData: Array<FloatArray>, offset: Int = 0, index2: Int=0) {
        //low-pass filter need parameters from MatLab
        //note: copy data in the main thread is so important,
        //please do not copy data in the coroutineScope
//        coroutineScope.launch {
        val tempoData = copyData2(tData,offset)
        val tensor = Tensor.fromBlob(tempoData, longArrayOf(1, 6, 200))
        val res = module.forward(IValue.from(tensor)).toTensor().dataAsFloatArray
        //output res for display on UI
        calculateDistance(res)
        modulePartial(currentLoc)
//        }
    }
    private val stringBuilder = StringBuilder()
    private fun copyData(tData:Array<FloatArray>, offset:Int=0):FloatArray{
        val tempoData = FloatArray(DATA_SIZE)  //1200
        for (index in offset until FRAME_SIZE) { //the last data
            //low-pass filters are muted.
//                filters[index].filter(floatArray).copyInto(tempoData, index * FRAME_SIZE)
            for (i in 0 until 6){
                tempoData[(index-offset)*6+i] = tData[i][index]
            }
        }
        val tOffset = FRAME_SIZE-offset
        for (index in 0 until offset) {  //the new data
            for (i in 0 until 6) {
                tempoData[(tOffset+index)*6+i] = tData[i][index]
            }
        }
        return tempoData
    }

    private fun copyData2(tData: Array<FloatArray>, offset: Int=0):FloatArray{
        val tempoData=FloatArray(DATA_SIZE)
        for(sensorIndex in 0 until 6){
            var startIndex= FRAME_SIZE*sensorIndex
            for(index in offset until FRAME_SIZE){
                tempoData[startIndex+(index-offset)]=tData[sensorIndex][index]
            }
            startIndex+=(FRAME_SIZE-offset)
            for(index in 0 until offset){
                tempoData[startIndex+index]=tData[sensorIndex][index]
            }
        }
        return tempoData
    }

    private fun calculateDistance(res: FloatArray) {
        currentLoc[0] += res[0] * V_INTERVAL
        currentLoc[1] += res[1] * V_INTERVAL
    }

    private var status = Status.Idle
    private var gestureType = GestureType.Hand
    private val rotl = object : SensorEventListener {
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

    private lateinit var gestureTypeListener: (GestureType) -> Unit
    fun setGestureTypeChangeListener(listener: (GestureType) -> Unit) {
        gestureTypeListener = listener
    }

    private fun resetSensor() {
        stopSensor()
        sensorManager.registerListener(rotl, rotVSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(accl, accVSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(gyrol, gyroVSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun stopSensor() {
        sensorManager.unregisterListener(accl)
        sensorManager.unregisterListener(gyrol)
        sensorManager.unregisterListener(rotl)
    }

    companion object {
        const val FRAME_SIZE = 200
        const val DATA_SIZE = 6 * 200
        const val FREQ_INTERVAL = 5L
        const val STEP = 10
        const val V_INTERVAL = 1f / (FRAME_SIZE/ STEP)
    }
}