package com.dadachen.isitp

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor

class MainActivity : AppCompatActivity() {
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
        initView()
    }

    private fun initView() {
        bt_load_module.setOnClickListener {
            loadInitModuleAndInitIMUCollector()
        }
    }

    fun loadInitModuleAndInitIMUCollector(){
        val module = Module.load(Utils.assetFilePath(this,"checkpoint_100.pt"))
        collector = IMUCollector(this){
            val tensor = Tensor.fromBlob(it, longArrayOf(200,6))
            val res = module.forward(IValue.from(tensor)).toTensor()
        }
        Toast.makeText(this, "load success", Toast.LENGTH_SHORT).show()
    }


    private lateinit var collector:IMUCollector
    fun startRecord(){
        collector.start()
    }
    fun stopRecord(){
        collector.stop()
    }
}