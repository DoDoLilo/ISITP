package com.dadachen.isitp

import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.lang.Exception

enum class GestureType {
    Hand,
    Pocket
}

class GestureClassifier(private val modulePath: String) {
    private val module: Module = Module.load(modulePath)
    fun forward(data: FloatArray): GestureType {
        val res = module.forward(IValue.from(Tensor.fromBlob(data, longArrayOf(1, 1, 192, 6))))
            .toTensor().dataAsFloatArray
        if (res.size != 2) {
            throw Exception("Gesture recognition output shape is (1, ${res.size}), expected: (1, 2).")
        }
        return if (res[0] < res[1]) {
            GestureType.Hand
        } else {
            GestureType.Pocket
        }
    }
}