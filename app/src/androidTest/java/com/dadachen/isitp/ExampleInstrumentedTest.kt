package com.dadachen.isitp

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.dadachen.isitp", appContext.packageName)
    }

    @Test
    fun test_gesture_module(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val module = Module.load(cn.whu.cs.niu.PDR.Utils.assetFilePath(appContext,"gesture_3.pt"))
        val reader = File("${appContext.externalCacheDir}/IMU-1608279949401.csv").bufferedReader()
        val content = reader.readLines()
        val data = content.map {
            it.split(",").map { v-> v.toFloat()  }
        }
        val ress = mutableListOf<Int>()
        for (l in 0 until data.size-200 step 200){
            val tdata = Array(192){ FloatArray(6) }
            for (i in 0 until 192){
                tdata[i] = data[i+l].toFloatArray()
            }
            val ttData = FloatArray(192*6)
            for (i in 0 until 192){
                tdata[i].copyInto(ttData,i*6)
            }
            val res = module.forward(IValue.from(Tensor.fromBlob(ttData, longArrayOf(1,1,192,6)))).toTensor().dataAsFloatArray
            ress.add(res.maxIndex())
        }
        print("done")
    }
    private fun FloatArray.maxIndex():Int{
        var res = -1
        var t = this[0]-1
        for (i in this.indices){
            if(t<this[i]){
                res = i
                t = this[i]
            }
        }
        return res
    }
}