package com.dadachen.isitp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.PointLabelFormatter
import kotlinx.android.synthetic.main.activity_main.*


/* *
* 1st, obtain IMU data.
* 2nd, post processing IMU data by low-pass filters. todo
* 3rd, transfer it to tensors.
* 4th, forward the pre-saved net with tensors.
* 5th, calculate the estimated location and display it on UI.
* 6th, additionally, it supports gesture estimation and show the doubt value. todo
* 7th, remember, it should be always fully tested with both unit and integrated
* @Author dadachen
* */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }
    var isLoading = false
    private fun initView() {
        bt_load_module.setOnClickListener {
            if (!isLoading) {
                loadInitModuleAndInitIMUCollector()
                isLoading = true
            }else{
                stopRecord()
                isLoading = false
            }
        }
    }

    private fun loadInitModuleAndInitIMUCollector() {
        collector = IMUCollector(this) {
            runOnUiThread {
                tv_res.text = it.contentToString()
            }
            drawPlot(it[0],it[1])
        }
        collector.setGestureTypeChangeListener {
            runOnUiThread {
                tv_gesture.text = it.name
            }
        }
        initDraw()
        startRecord()
    }

    private lateinit var collector: IMUCollector
    private fun startRecord() {
        collector.start()
        Toast.makeText(this, "load module success", Toast.LENGTH_SHORT).show()
    }
    private lateinit var series:TrackSeries

    private fun initDraw(){
        // Turn the arrays above into XYSeries':
        series = TrackSeries("p")
        val seriesFormat = LineAndPointFormatter()
        seriesFormat.pointLabelFormatter = PointLabelFormatter()
        seriesFormat.configure(
            applicationContext,
            R.xml.line_point_formatter_with_labels
        )
        plot.addSeries(series, seriesFormat)
    }

    private fun drawPlot(x:Float, y:Float) {
        series.appendData(x, y)
        plot.redraw()
    }

    private fun stopRecord() {
        collector.stop()
    }
}