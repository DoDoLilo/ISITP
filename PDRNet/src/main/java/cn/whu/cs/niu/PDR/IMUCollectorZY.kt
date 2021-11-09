package cn.whu.cs.niu.PDR

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import kotlin.concurrent.thread
import kotlin.math.max

internal class IMUCollectorZY(
    private val context: Context,
    private val modulePath: String = "mobile_model.ptl"
) {
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
    private val times = LongArray(FRAME_SIZE)
    private val currentLoc = floatArrayOf(0f, 0f)
    private val rotData = FloatArray(4)

    private enum class Status {
        Running, Idle
    }

    fun start() {
        initSensor()
        println("sensor init!")
        status = Status.Running

        //start thread
        thread(start = true) { //创建一个thread并运行指定代码块，()->Unit
            module = Module.load(Utils.assetFilePath(context, modulePath))
            var index = 0
            while (index < FRAME_SIZE) {
                fillData(index++)
                Thread.sleep(FREQ_INTERVAL)
            }
            estimate(data.copyOf(), times.copyOf(), -1)
            index = 0
            fillData(index++)
            Thread.sleep(FREQ_INTERVAL)
            while (status == Status.Running) {
                if (index == FRAME_SIZE) {
                    val tData = data.copyOf()
                    val tTimes = times.copyOf()
                    //estimation by using 200 frames IMU-sensor
                    estimate(tData, tTimes, 0)
                    //next step reset offset to zero
                    index = 0
                } else if (index % STEP == 0) { //每10*5ms进行一输出
                    //note index is always more than 1
                    val tData = data.copyOf()
                    val tTimes = times.copyOf()
                    estimate(tData, tTimes, index)
                }
                fillData(index++)
                Thread.sleep(FREQ_INTERVAL) //这里控制了sleep 5ms，即200Hz
            }
        }
    }

    private fun changeTheAxisOfAccAndGyro(
        acc0: Float, acc1: Float, acc2: Float, gyro0: Float, gyro1: Float, gyro2: Float,
        rot0: Float, rot1: Float, rot2: Float, rot3: Float
    ): FloatArray {
//        change the acc and the gyro to the same axis by using the rot
//        1.change the rotVector array
//        val rotChanged = floatArrayOf(rotVector[3], rotVector[0], rotVector[1], rotVector[2])
        val rotQuaternion = Quaternion(rot3, rot0, rot1, rot2)
        val gyroQuaternion = Quaternion(0f, gyro0, gyro1, gyro2)
        val accQuaternion = Quaternion(0f, acc0, acc1, acc2)
//        2.use the Quaternion functions
        val gyroChanged =
            rotQuaternion.times(gyroQuaternion).times(rotQuaternion.conjugate()).toFloatArray()
        val accChanged =
            rotQuaternion.times(accQuaternion).times(rotQuaternion.conjugate()).toFloatArray()
//        3.past the values to acc and gyro
        val gyroAccChanged = floatArrayOf(
            gyroChanged[1],
            gyroChanged[2],
            gyroChanged[3],
            accChanged[1],
            accChanged[2],
            accChanged[3]
        )
        return gyroAccChanged
    }

    private fun updateLocalPositions() {
        //todo
    }

    private fun fillData(index: Int) {
        times[index] = System.currentTimeMillis()
        rotVector.copyInto(rotData)
        val gyroAccChanged = changeTheAxisOfAccAndGyro(
            acc[0], acc[1], acc[2], //acc
            gyro[0], gyro[1], gyro[2],  //gyro
            rotVector[0], rotVector[1], rotVector[2], rotVector[3]
        ) //rot
        data[0][index] = gyroAccChanged[0]
        data[1][index] = gyroAccChanged[1]
        data[2][index] = gyroAccChanged[2]
        data[3][index] = gyroAccChanged[3]
        data[4][index] = gyroAccChanged[4]
        data[5][index] = gyroAccChanged[5]
    }

    fun stop() {
        status = Status.Idle
        stopSensor()
    }


    private lateinit var module: Module
    private fun estimate(tData: Array<FloatArray>, tTimes: LongArray, offset: Int = 0) {
        val tempoData = copyData2(tData, max(0, offset))

        GlobalScope.launch {
            val tensor = Tensor.fromBlob(tempoData, longArrayOf(1, 6, 200))
            val res = module.forward(IValue.from(tensor)).toTensor().dataAsFloatArray
            //output res for display on UI
            calculateDistance(res, tTimes[max(0, offset)], getMovedTime(tTimes, offset))
        }
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

    private fun getMovedTime(tTimes: LongArray, offset: Int = -1): Float {
        if (offset == -1) {
            return (tTimes[FRAME_SIZE - 1] - tTimes[0]) / 1000f
        } else {
            return (tTimes[(offset-1 + FRAME_SIZE) % FRAME_SIZE] - tTimes[(offset - STEP + FRAME_SIZE) % FRAME_SIZE]) / 1000f
        }
    }

    private fun calculateDistance(res: FloatArray, tTime: Long, movedTime: Float) {
//        println("net res" + res[0] + ", " + res[1])
        currentLoc[0] += res[0] * movedTime
        currentLoc[1] += res[1] * movedTime
        //do some operations to pass locations and time array to Class Tool.
//        println("net res" + res[0] + ", " + res[1] + " ||| cur pos" + currentLoc[1] / currentLoc[0])
        handler?.apply { this(floatArrayOf(currentLoc[0], currentLoc[1]), tTime, rotData) }
    }

    var handler: ((location: FloatArray, time: Long, vector: FloatArray) -> Unit)? = null

    private var status = Status.Idle
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
        const val V_INTERVAL = 1f / (FRAME_SIZE / STEP)
        const val SECOND = 20
    }
}