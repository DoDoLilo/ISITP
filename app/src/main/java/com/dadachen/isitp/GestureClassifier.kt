package com.dadachen.isitp

import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.lang.Exception

enum class GestureType {
    Hand,
    Pocket,
    Swing
}

class GestureClassifier(modulePath: String) {
    private val module: Module = Module.load(modulePath)
    fun forward(data: FloatArray): GestureType {
        val res = module.forward(IValue.from(Tensor.fromBlob(data, longArrayOf(1, 1, 192, 6))))
            .toTensor().dataAsFloatArray
        if (res.size != 3) {
            throw Exception("Gesture recognition output shape is (1, ${res.size}), expected: (1, 2).")
        }
        Log.d("gesture", res.contentToString())
        return when(res.maxIndex()){
            0-> GestureType.Pocket
            1-> GestureType.Hand
            2-> GestureType.Swing
            else-> GestureType.Hand
        }
    }

    private fun FloatArray.maxIndex():Int{
        var res = -1
        var t = Float.MIN_VALUE
        for (i in this.indices){
            if(t<this[i]){
                res = i
                t = this[i]
            }
        }
        return res
    }
}