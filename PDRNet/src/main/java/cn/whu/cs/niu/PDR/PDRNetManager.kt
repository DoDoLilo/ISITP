package cn.whu.cs.niu.PDR

import android.content.Context

class PDRNetManager {
    private var imuCollector: IMUCollectorZY? = null


    val dataSize = IMUCollectorZY.FRAME_SIZE / IMUCollectorZY.STEP * IMUCollectorZY.SECOND
    private val times: LongArray = LongArray(dataSize)
    private val locations = Array(dataSize) {
        DoubleArray(2)
    }
    private var index = 0
    private var firstRound = true


    private var outPutTimes = LongArray(dataSize)
    private var outPutLocations = Array(dataSize) {
        DoubleArray(2)
    }

    private fun circleArrayToNormal(
        rotArray: Array<DoubleArray>,
        normalArray: Array<DoubleArray>,
        offset: Int = 0
    ){

        for (index in offset until dataSize) {
            normalArray[index - offset][0] = rotArray[index][0]
            normalArray[index - offset][1] = rotArray[index][1]
        }
        for (index in 0 until offset) {
            normalArray[dataSize - offset + index][0] = rotArray[index][0]
            normalArray[dataSize - offset + index][1] = rotArray[index][1]
        }

    }
    private fun circleArrayToNormal(
        rotArray: LongArray,
        normalArray: LongArray,
        offset: Int = 0
    ){

        for (index in offset until dataSize) {
            normalArray[index - offset] = rotArray[index]
            normalArray[index - offset] = rotArray[index]
        }
        for (index in 0 until offset) {
            normalArray[dataSize - offset + index] = rotArray[index]
            normalArray[dataSize - offset + index] = rotArray[index]
        }

    }

    private val imuHandler: ((location: FloatArray, time: Long, rot: FloatArray) -> Unit) =
        { location, time, rot ->
            times[index] = time
            locations[index][0] = location[0].toDouble()
            locations[index][1] = location[1].toDouble()

            index = (index + 1) % dataSize
            circleArrayToNormal(times, outPutTimes, index)
            circleArrayToNormal(locations, outPutLocations, index)
//            CoordinateTool.updateCoordinate(rot.toDoubleArray(), outPutLocations)

            completion?.apply {
                this(outPutTimes, outPutLocations, rot.toDoubleArray())
            }
        }

    private fun FloatArray.toDoubleArray(): DoubleArray {
        val res = DoubleArray(this.size)
        for (i in this.indices) {
            res[i] = this[i].toDouble()
        }
        return res
    }

    private var completion: ((times: LongArray, locations: Array<DoubleArray>, grv: DoubleArray) -> Unit)? = null


    /**
     * 开始采集PDR，使用回调函数获取时间戳和位置数据
     * 回调函数中包含两个形参，分别为时间戳数组和坐标数组。
     * 时间戳数组长度为400，由System.currentTimeMillis()获取。
     * 位置数据为400×2的double类型数据。以最后一个位置为原点，以最后一个位置的IMU的航向角为X轴的水平坐标系。
     * 该模块调用了TYPE_GAME_ROTATION_VECTOR、TYPE_ACCELEROMETER、TYPE_GYROSCOPE_UNCALIBRATED三种传感器。
     * 传感器采样频率为SENSOR_DELAY_FASTEST
     * @param context 安卓上下文
     * @param module PDR网络模型的位置
     * @param handler 需要的回调函数
     */
    fun start(
        context: Context,
        module: String = "mobile_model.ptl",
        handler: (times: LongArray, locations: Array<DoubleArray>, grv: DoubleArray) -> Unit
    ) {
        imuCollector = null
        imuCollector = IMUCollectorZY(context = context, modulePath = module)
        //custom handler passed to imuCollector
        imuCollector?.handler = imuHandler
        completion = handler
        firstRound = true
        index = 0
        imuCollector?.start()

    }

    /**
     * 停止采集PDR
     *
     */
    fun stop(filePath:String) {
        imuCollector?.stop(filePath)
    }
}