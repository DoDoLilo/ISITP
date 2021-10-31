package com.dadachen.isitp

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.PointLabelFormatter
import kotlinx.android.synthetic.main.activity_main.*


/* *
* 1st, obtain IMU data.     //获取IMU数据
* 2nd, post processing IMU data by low-pass filters. todo　
* 3rd, transfer it to tensors. //转换为“张量”
* 4th, forward the pre-saved net with tensors. //前向传播，将tensors放入网络中
* 5th, calculate the estimated location and display it on UI. //计算估计的位置并显示在UI上。
* 6th, additionally, it supports gesture estimation and show the doubt value. todo
* 7th, remember, it should be always fully tested with both unit and integrated //软件测试技术——单元测试和集成测试
* @Author dadachen
* */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }
    private var isLoading = false
    private fun initView() {//俩功能按钮的监听
        bt_load_module.setOnClickListener {
            if (!isLoading) {
                loadInitModuleAndInitIMUCollector()
                isLoading = true
                bt_load_module.text = getText(R.string.stop)
                sw_save_csv.isEnabled = false
            }else{
                stopRecord()
                sw_save_csv.isEnabled = true
                isLoading = false
                bt_load_module.text = getText(R.string.load_module)
            }
        }
        sw_save_csv.setOnCheckedChangeListener { _, isChecked ->
            FilterConstant.RECORD_CSV = isChecked
        }
    } //√

    private fun loadInitModuleAndInitIMUCollector() {
        collector = IMUCollector(this) {
            runOnUiThread {
                tv_res.text = it.contentToString()
                drawPlot(it[0],it[1])
            }
        }
        collector.setGestureTypeChangeListener {
            runOnUiThread {
                tv_gesture.text = it.name
            }
        }
        plot.clear()
        initDraw()
        startRecord()
    }

    private lateinit var collector: IMUCollector //后初始化
    private fun startRecord() {
        collector.start()
        //Toast.makeText(this, "load module success", Toast.LENGTH_SHORT).show()
    }
    private lateinit var series:TrackSeries

    private fun initDraw(){
        // Turn the arrays above into XYSeries':
        series = TrackSeries("point")
        val seriesFormat = LineAndPointFormatter()
        seriesFormat.pointLabelFormatter = PointLabelFormatter()
        seriesFormat.configure(
            applicationContext,
            R.xml.line_point_formatter_with_labels
        )
        plot.addSeries(series, seriesFormat)
        plot.setDomainBoundaries(-15,15,BoundaryMode.FIXED)
        plot.setRangeBoundaries(-20,20,BoundaryMode.FIXED)
    }

    private fun drawPlot(x:Float, y:Float) {
        series.appendData(x, y)
        plot.redraw()
    }

    private fun removeDraw() {
        plot.removeSeries(series)
//        plot.redraw()
    }

    private fun stopRecord() {
//        removeDraw()
        collector.stop()
    }
}