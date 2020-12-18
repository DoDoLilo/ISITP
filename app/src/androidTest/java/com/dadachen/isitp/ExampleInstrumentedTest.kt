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

        val module = Module.load(Utils.assetFilePath(appContext,"gesture.pt"))
        val reader = File("${appContext.externalCacheDir}/IMU-1608269898729.csv").bufferedReader()
        val content = reader.readLines()
        val data = content.map {
            it.split(",").map { v-> v.toFloat()  }
        }
        val tdata = Array(192){ FloatArray(6) }
        val offset = 100
        for (i in 0 until 192){
            tdata[i] = data[i+offset].toFloatArray()
        }
        val ttData = FloatArray(192*6)
        for (i in 0 until 6){
            tdata[i].copyInto(ttData,i*192)
        }
        val res = module.forward(IValue.from(Tensor.fromBlob(ttData, longArrayOf(1,1,192,6)))).toTensor().dataAsFloatArray
        println(res.contentToString())
    }
}