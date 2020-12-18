package com.dadachen.isitp

import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.lang.Exception

enum class GestureType {
    Hand,
    Pocket
}

class GestureClassifier(modulePath: String) {
    private val module: Module = Module.load(modulePath)
    fun forward(data: FloatArray): GestureType {
        val res = module.forward(IValue.from(Tensor.fromBlob(data, longArrayOf(1, 1, 192, 6))))
            .toTensor().dataAsFloatArray
        if (res.size != 2) {
            throw Exception("Gesture recognition output shape is (1, ${res.size}), expected: (1, 2).")
        }
        Log.d("gesture", res.contentToString())
        //TODO there seems to be a bug
        // cuz the module losing a soft-max layer behind the last full-connect layer.
        return if (res[0] < res[1]) {
            GestureType.Hand
        } else {
            GestureType.Pocket
        }
    }
}