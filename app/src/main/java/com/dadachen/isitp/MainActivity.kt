package com.dadachen.isitp

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.util.*

/* *
* 1st, obtain IMU data,done
* 2nd, post processing IMU data by low-pass filters. todo
* 3rd, transfer it to tensors done
* 4th, forward the pre-saved net with tensors done
* 5th, calculate the estimated location and display it on UI. todo
* 6th, additionally, it supports gesture estimation and show the doubt value. todo
* 7th, remember, it should be always fully tested with both unit and integrated. todo
* @Author dadachen
* */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
    }

    private fun initView() {
        bt_load_module.setOnClickListener {
            loadInitModuleAndInitIMUCollector()
        }
    }

    private fun loadInitModuleAndInitIMUCollector(){
        collector = IMUCollector(this){
            runOnUiThread {
                tv_res.text = it.contentToString()
            }
        }
        startRecord()
    }

    private lateinit var collector:IMUCollector
    fun startRecord(){
        collector.start()
        Toast.makeText(this, "load module success", Toast.LENGTH_SHORT).show()

    }
    fun stopRecord(){
        collector.stop()
    }
}