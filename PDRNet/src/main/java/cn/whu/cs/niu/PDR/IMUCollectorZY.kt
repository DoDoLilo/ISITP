package cn.whu.cs.niu.PDR

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
            //这个estimate是为了输出0,0？
//            estimate(data.copyOf(), times.copyOf(), -1)
            //填充第一个滑动窗口
            while (index < FRAME_SIZE) {
                fillData(index++)
                Thread.sleep(FREQ_INTERVAL)
            }
            //计算第一个窗口的结果
            estimate(data.copyOf(), times.copyOf(), -1)
            //index重新开始，index指向循环数组下一次存储窗口数据的位置
            index = 0
            fillData(index++)
            Thread.sleep(FREQ_INTERVAL)
            while (status == Status.Running) {
                if (index == FRAME_SIZE) {
                    //index到达循环数组末尾
                    val tData = data.copyOf()
                    val tTimes = times.copyOf()
                    estimate(tData, tTimes, 0)
                    //index重新开始
                    index = 0
                } else if (index % STEP == 0) {
                    //滑动窗口
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

    //return gyro + acc
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

    fun stop(filePath: String) {
        status = Status.Idle
        stopSensor()
    }


    private lateinit var module: Module

    //offset:表示循环数组中的对应输入到模型的窗口起点数据的下标，offset的左边是窗口的后半部分、右边是前半部分
    //tData\tTimes[0..offset-1]是新数据，tData\tTimes[offset..len-1]是旧数据
    private fun estimate(tData: Array<FloatArray>, tTimes: LongArray, offset: Int = 0) {
        GlobalScope.launch {
            val tempoData = copyData(tData, max(0, offset))
            val inputTensor = Tensor.fromBlob(tempoData, longArrayOf(1, 200, 6))
            val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()

            //获取网络输出张量的结果
            val outputShape = outputTensor.shape()
            //张量是 [[0, 1], [2, 3], [4, 5], [6, 7]]，并调用了 dataAsFloatArray 方法获取数据，
            //  得到的一维数组将是 [0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0]
            val tempOutArr: FloatArray = outputTensor.dataAsFloatArray
            val resVxy: FloatArray = floatArrayOf(0.0f, 0.0f)
            val rowNum = outputShape[1].toInt()
            val colNum = outputShape[2].toInt()
            //获取 整个窗口一半帧 的Vx Vy 之和
//            var numToMean =  rowNum / 2
//            var numToMean = STEP
            var numToMean = rowNum
            if (offset == -1) {
                //-1表示是第一个窗口，则取整个窗口
                numToMean = rowNum
            }
            for (i in (rowNum - numToMean) until rowNum) {
                for (j in 0 until colNum) {
                    //Vx Vy
                    val index = i * colNum + j
                    resVxy[j] += tempOutArr[index]
                }
            }
            //求平均，除以整个窗口大小
            resVxy[0] /= numToMean.toFloat()
            resVxy[1] /= numToMean.toFloat()

            //output res for display on UI
            calculateDistance(resVxy, tTimes[max(0, offset)], getMovedTime(tTimes, offset))
        }
    }

    //使用残差pdt模型时，输入是6*200，输出是1*2
    private fun estimate2(tData: Array<FloatArray>, tTimes: LongArray, offset: Int = 0) {
        GlobalScope.launch {
//            println("index:"+offset)
            val tempoData = copyData2(tData, max(0, offset))
            val tensor = Tensor.fromBlob(tempoData, longArrayOf(1, 6, 200))
            val res = module.forward(IValue.from(tensor)).toTensor().dataAsFloatArray
            //output res for display on UI
            calculateDistance(res, tTimes[max(0, offset)], getMovedTime(tTimes, offset))
//            println("index:"+offset)
        }
    }

    //012345|012345|...
    private fun copyData(tData: Array<FloatArray>, offset: Int = 0): FloatArray {
        val tempoData = FloatArray(DATA_SIZE)  //1200
        for (index in offset until FRAME_SIZE) { //the last data
            //low-pass filters are muted.
//                filters[index].filter(floatArray).copyInto(tempoData, index * FRAME_SIZE)
            for (i in 0 until 6) {
                tempoData[(index - offset) * 6 + i] = tData[i][index]
            }
        }
        val tOffset = FRAME_SIZE - offset
        for (index in 0 until offset) {  //the new data
            for (i in 0 until 6) {
                tempoData[(tOffset + index) * 6 + i] = tData[i][index]
            }
        }
        return tempoData
    }

    //000...00|111...11|...|555
    private fun copyData2(tData: Array<FloatArray>, offset: Int = 0): FloatArray {
        val tempoData = FloatArray(DATA_SIZE)
        for (sensorIndex in 0 until 6) {
            var startIndex = FRAME_SIZE * sensorIndex
            for (index in offset until FRAME_SIZE) {
                //窗口的前半部分 = 循环数组offset的后半部分
                tempoData[startIndex + (index - offset)] = tData[sensorIndex][index]
            }
            startIndex += (FRAME_SIZE - offset)
            for (index in 0 until offset) {
                //窗口的后半部分 = 循环数组offset的前半部分
                tempoData[startIndex + index] = tData[sensorIndex][index]
            }
        }
        return tempoData
    }

    private fun getMovedTime(tTimes: LongArray, offset: Int = -1): Float {
        if (offset == -1) {
            //返回第199帧时间 - 第0帧时间
            return (tTimes[FRAME_SIZE - 1] - tTimes[0]) / 1000f
        } else {
            //返回窗口末尾帧时间 - 窗口倒数第STEP帧的时间，即输入到模型的窗口末尾STEP帧的时间间隔
            return (tTimes[(offset - 1 + FRAME_SIZE) % FRAME_SIZE] - tTimes[(offset - STEP - 1 + FRAME_SIZE) % FRAME_SIZE]) / 1000f
        }
    }

    private fun calculateDistance(res: FloatArray, tTime: Long, movedTime: Float) {
//        println("net res" + res[0] + ", " + res[1])
//        println("moved time:" + movedTime)
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
        sensorManager.registerListener(rotl, rotVSensor, SAMPLING_PERIOD_US)
        sensorManager.registerListener(accl, accVSensor, SAMPLING_PERIOD_US)
        sensorManager.registerListener(gyrol, gyroVSensor, SAMPLING_PERIOD_US)
    }


    private fun resetSensor() {
        stopSensor()
        sensorManager.registerListener(rotl, rotVSensor, SAMPLING_PERIOD_US)
        sensorManager.registerListener(accl, accVSensor, SAMPLING_PERIOD_US)
        sensorManager.registerListener(gyrol, gyroVSensor, SAMPLING_PERIOD_US)
    }

    private fun stopSensor() {
        sensorManager.unregisterListener(accl)
        sensorManager.unregisterListener(gyrol)
        sensorManager.unregisterListener(rotl)
    }

    internal companion object {
        internal const val FRAME_SIZE = 200
        internal const val DATA_SIZE = 6 * 200
        internal const val FREQ_INTERVAL = 5L

        //        internal const val STEP = 10
        internal const val STEP = 40
        internal const val V_INTERVAL = 1f / (FRAME_SIZE / STEP)
        internal const val SECOND = 20  //这个应该是记录位置缓存的秒数，缓存数组保存20秒的位置结果
        internal const val SAMPLING_PERIOD_US = 1000000 / FRAME_SIZE
    }
}