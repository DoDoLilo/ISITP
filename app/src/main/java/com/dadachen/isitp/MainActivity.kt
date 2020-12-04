package com.dadachen.isitp

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.net.URI
import java.net.URL

class MainActivity : AppCompatActivity() {
    val modulePath:String  = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //1st, obtain IMU data,
        //2nd, post processing IMU data by low-pass filters. done
        //3rd, transfer it to tensors done
        //4th, forward the pre-saved net with tensors done
        //5th, calculate the estimated location and display it on UI
        //6th, additionally, it supports gesture estimation and show the doubt value
        //7th, remember, it should be always fully tested with both unit and integrated.
        //transfer post-processed IMU data to tensors
        //TODO(implement the path by Uri or etc)
        val module = Module.load(modulePath)
        collector = IMUCollector(this){
            val tensor = Tensor.fromBlob(it, longArrayOf(200,6))
            val res = module.forward(IValue.from(tensor)).toTensor()
        }
    }
    private lateinit var collector:IMUCollector
    fun startRecord(){
        collector.start()
    }
    fun stopRecord(){
        collector.stop()
    }
}