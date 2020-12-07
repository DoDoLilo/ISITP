package com.dadachen.isitp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.XYGraphWidget
import kotlinx.android.synthetic.main.activity_main.*
import java.text.FieldPosition
import java.text.ParsePosition
import kotlin.math.roundToInt

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
        }
        startRecord()
    }

    private lateinit var collector: IMUCollector
    private fun startRecord() {
        collector.start()
        Toast.makeText(this, "load module success", Toast.LENGTH_SHORT).show()
        drawPlot()

    }

    private fun drawPlot() {
        val numbers = List<Number>(6) { index -> index + 1 }
        val numbersX = List<Number>(6) { index -> index + 1 }
        val numbersY = List<Number>(6) { index -> index + 1 }

        val series1 = SimpleXYSeries(numbersX, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series1")
        val series2 = SimpleXYSeries(numbersY, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2")
        plot.addSeries(series1, LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels))
        plot.addSeries(series2, LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels))

        plot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format = object :
            java.text.Format() {
            override fun format(
                obj: Any?,
                toAppendTo: StringBuffer?,
                pos: FieldPosition?
            ): StringBuffer {
                val i = (obj as Number).toFloat().roundToInt()
                return toAppendTo!!.append(numbers[i])
            }

            override fun parseObject(source: String?, pos: ParsePosition?): Any {
                return Any()
            }

        }

    }

    fun stopRecord() {
        collector.stop()
    }
}