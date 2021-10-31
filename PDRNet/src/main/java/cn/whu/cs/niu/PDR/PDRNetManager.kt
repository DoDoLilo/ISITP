package cn.whu.cs.niu.PDR

import android.content.Context
import com.dadachen.isitp.IMUCollectorZY

/***
 * 
 */
class PDRNetManager {
    private var imuCollector: IMUCollectorZY? = null


    val dataSize = IMUCollectorZY.FRAME_SIZE / IMUCollectorZY.STEP * IMUCollectorZY.SECOND
    private val times: LongArray = LongArray(dataSize)
    private val locations = Array(dataSize) {
        DoubleArray(2)
    }
    private var index = 0
    private var firstRound = true
    private var offset = 10
    private var preIndex = 0

    private var outPutTimes = LongArray(dataSize)
    private var outPutLocations = Array(dataSize) {
        DoubleArray(2)
    }

    private fun <T> rotationArrayToNormal(
        rotArray: Array<T>,
        normalArray: Array<T>,
        offset: Int = 0
    ) {
        rotArray.copyInto(
            destination = normalArray,
            destinationOffset = 0,
            startIndex = offset,
            endIndex = rotArray.size
        )
        rotArray.copyInto(
            destination = normalArray,
            destinationOffset = rotArray.size - offset,
            startIndex = 0,
            endIndex = offset
        )
    }

    private fun rotationArrayToNormal(
        rotArray: LongArray,
        normalArray: LongArray,
        offset: Int = 0
    ) {
        rotArray.copyInto(
            destination = normalArray,
            destinationOffset = 0,
            startIndex = offset,
            endIndex = rotArray.size
        )
        rotArray.copyInto(
            destination = normalArray,
            destinationOffset = rotArray.size - offset,
            startIndex = 0,
            endIndex = offset
        )
    }

    private val imuHandler: ((location: FloatArray, time: Long, rot: FloatArray) -> Unit) =
        { location, time, rot ->
            if (firstRound) {
                if (index < dataSize) {
                    times[index] = time
                    locations[index] = location.toDoubleArray()
                    index++
                }
                if (index == dataSize) {
                    //before do the operations, we need to reverse out data to the normal array
                    //tools operations
                    rotationArrayToNormal(times, outPutTimes)
                    rotationArrayToNormal(locations, outPutLocations)
                    CoordinateTool.updateCoordinate(rot.toDoubleArray(), outPutLocations)
                    //locations
                    completion?.apply {
                        this(outPutTimes, outPutLocations)
                    }
                    //reset index
                    index %= dataSize
                    firstRound = false
                }
            } else {
                val rightBar = (preIndex + offset) % dataSize
                if (index < rightBar) {
                    times[index] = time
                    locations[index] = location.toDoubleArray()
                    index++
                }
                if (index == rightBar) {
                    rotationArrayToNormal(times, outPutTimes)
                    rotationArrayToNormal(locations, outPutLocations)
                    CoordinateTool.updateCoordinate(rot.toDoubleArray(), outPutLocations)
                    //locations
                    completion?.apply {
                        this(outPutTimes, outPutLocations)
                    }
                    //updating preIndex
                    preIndex = index % dataSize
                    //index is right
                    index %= dataSize

                }
            }
        }

    private fun FloatArray.toDoubleArray(): DoubleArray {
        val res = DoubleArray(this.size)
        for (i in this.indices) {
            res[i] = this[i].toDouble()
        }
        return res
    }

    private var completion: ((times: LongArray, Array<DoubleArray>) -> Unit)? = null


    fun start(
        context: Context,
        module: String = "mobile_model.ptl",
        handler: (times: LongArray, locations: Array<DoubleArray>) -> Unit
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

    //Note, stop must be invoked after start, or it would crash due to NullPointer exception
    fun stop() {
        imuCollector?.stop()
    }
}