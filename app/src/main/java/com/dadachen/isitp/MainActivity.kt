package com.dadachen.isitp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.PointLabelFormatter
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.XYSeries
import kotlinx.android.synthetic.main.activity_main.*


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
        // Turn the above arrays into XYSeries':
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

    fun stopRecord() {
        collector.stop()
    }
}