package com.example.realtodoapp.adapter

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.databinding.*
import com.example.realtodoapp.model.AppRoutineForTimeDto
import com.example.realtodoapp.model.DateInfoDto
import com.example.realtodoapp.util.AppUtil
import com.example.realtodoapp.util.LinearLayoutManagerWrapper
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Year
import java.util.ArrayList

class AdapterAppRoutineForTimeList(val context: Context, var list: List<AppRoutineForTimeDto>,
                                   var dialogAppListBinding: DialogAppListBinding, var dialogGraphBinding: DialogGraphBinding,
                                   var dialogAppUsePiechartBinding: DialogAppUsePiechartBinding) : RecyclerView.Adapter<AppRoutineForTimeHolder>(){

    var items = list
        @SuppressLint("NotifyDataSetChanged")
        set(value){
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppRoutineForTimeHolder {
        var bind =ItemAppRoutineBinding.inflate(LayoutInflater.from(context), parent, false)
        return AppRoutineForTimeHolder(context, bind, dialogAppListBinding, dialogGraphBinding, dialogAppUsePiechartBinding)
    }

    override fun onBindViewHolder(holder: AppRoutineForTimeHolder, position: Int) {
        var item = items.get(position)
        holder.setItem(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class AppRoutineForTimeHolder(val context: Context, var bind: ItemAppRoutineBinding, var dialogAppListBinding: DialogAppListBinding,
                              var dialogGraphBinding: DialogGraphBinding, var dialogAppUsePiechartBinding: DialogAppUsePiechartBinding):RecyclerView.ViewHolder(bind.root){
    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor

    @SuppressLint("SetTextI18n")
    fun setItem(item:AppRoutineForTimeDto){
        val startHour = item.startHour
        val endHour = item.endHour
        val startMinute = item.startMinute
        val endMinute = item.endMinute

        // item의 정보로 시작시간, 종료 시간 담은 timeInfo 생성
        var startTimeString = item.year.toString()+"-"+String.format("%02d",item.month)+"-"+String.format("%02d",item.day)+"-"+String.format("%02d",startHour)+"-"+String.format("%02d",startMinute)
        var endTimeString = item.year.toString()+"-"+String.format("%02d",item.month)+"-"+String.format("%02d",item.day)+"-"+String.format("%02d",endHour)+"-"+String.format("%02d",endMinute) // 두자리수로 맞춰줌
        var timeInfo = startTimeString + endTimeString

        // 처음 이미지 불러옴
        var list = AppUtil.loadNotUseAppList(context, timeInfo)

        bind.forbiddenAppOne.setImageDrawable(null)
        bind.forbiddenAppTwo.setImageDrawable(null)
        bind.forbiddenAppThree.setImageDrawable(null)
        bind.forbiddenAppFour.setImageDrawable(null)

        if(list.size >= 1) bind.forbiddenAppOne.setImageDrawable(list[0].loadIcon(context.packageManager))
        if(list.size >= 2) bind.forbiddenAppTwo.setImageDrawable(list[1].loadIcon(context.packageManager))
        if(list.size >= 3) bind.forbiddenAppThree.setImageDrawable(list[2].loadIcon(context.packageManager))
        if(list.size >= 4) bind.forbiddenAppFour.setImageDrawable(list[3].loadIcon(context.packageManager))

        val timeLength = endHour - startHour
        if(timeLength * 100 <= 300){
            bind.appRoutineLinearLayout.layoutParams.height = 300
        }
        else{
            bind.appRoutineLinearLayout.layoutParams.height = timeLength * 100
        }

        bind.startHourTextView.setText(String.format("%02d",startHour)+": " + String.format("%02d",startMinute))
        bind.endHourTextView.setText(String.format("%02d",endHour)+": "+ String.format("%02d",endMinute))

        // sharedPreference 사용
        sharedPref = context.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()

        // 전체 앱 사용 시간 표시
        var appUseTimeRecord = mutableListOf<Pair<String, Int>>() // 앱 패키지명, 실행 시간
        var emptyAppUseTimeRecord = gson.toJson(appUseTimeRecord)

        var appUseTimeRecordJson = sharedPref.getString("appUseTimeRecord"+timeInfo,emptyAppUseTimeRecord).toString()
        appUseTimeRecord = gson.fromJson(appUseTimeRecordJson)

        // 사용 시간 순 정렬
        appUseTimeRecord.sortByDescending { it.second }

        // 가장 사용 시간이 높은 4개 앱 표시
        bind.realUseAppOne.setImageDrawable(null)
        bind.realUseAppTwo.setImageDrawable(null)
        bind.realUseAppThree.setImageDrawable(null)
        bind.realUseAppFour.setImageDrawable(null)

        var installedApps = AppUtil.getInstalledApp(context)
        if(appUseTimeRecord.size> 0) {
            for (app in installedApps) {
                if (app.packageName == appUseTimeRecord[0].first) {
                    bind.realUseAppOne.setImageDrawable(app.loadIcon(context.packageManager))
                    break
                }
            }
        }
        if(appUseTimeRecord.size> 1) {
            for (app in installedApps) {
                if (app.packageName == appUseTimeRecord[1].first) {
                    bind.realUseAppTwo.setImageDrawable(app.loadIcon(context.packageManager))
                    break
                }
            }
        }
        if(appUseTimeRecord.size> 2) {
            for (app in installedApps) {
                if (app.packageName == appUseTimeRecord[2].first) {
                    bind.realUseAppThree.setImageDrawable(app.loadIcon(context.packageManager))
                    break
                }
            }
        }
        if(appUseTimeRecord.size> 3) {
            for (app in installedApps) {
                if (app.packageName == appUseTimeRecord[3].first) {
                    bind.realUseAppFour.setImageDrawable(app.loadIcon(context.packageManager))
                    break
                }
            }
        }

        // 앱 사용 비율 표시
        bind.realUseAppConstraintLayout.setOnClickListener(){

            // dialog로 상태 출력
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if (dialogAppUsePiechartBinding.root.parent != null) {
                (dialogAppUsePiechartBinding.root.parent as ViewGroup).removeView(
                    dialogAppUsePiechartBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                dialog.dismiss()
            }
            dialog.setContentView(dialogAppUsePiechartBinding.root)
            var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
            params.width = (context.getResources()
                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
            params.height = (context.getResources()
                .getDisplayMetrics().heightPixels * 0.8).toInt() // device의 세로 길이에 비례하여  결정
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.getWindow()!!.setAttributes(params)
            dialog.getWindow()!!.setGravity(Gravity.CENTER)
            dialog.setCancelable(true)
            dialog.show()

            // pieChart 세팅
            dialogAppUsePiechartBinding.appUsePieChart.setUsePercentValues(true)
            val entries = ArrayList<PieEntry>()
            for (i in 0 until appUseTimeRecord.size){
                for (app in installedApps) {
                    if (app.packageName == appUseTimeRecord[i].first) {
                        entries.add(PieEntry(appUseTimeRecord[i].second.toFloat(),app.loadLabel(context.packageManager).toString() + " " + appUseTimeRecord[i].second /60 + "분"))
                        break
                    }
                }
            }

            var colorsItems = ArrayList<Int>()
            for (c in ColorTemplate.VORDIPLOM_COLORS) colorsItems.add(c)

            var pieDataSet = PieDataSet(entries, "")
            pieDataSet.apply{
                colors = colorsItems
                valueTextColor= Color.BLACK
                valueTextSize = 15f
            }

            val pieData = PieData(pieDataSet)
            dialogAppUsePiechartBinding.appUsePieChart.apply{
                data = pieData
                description.isEnabled = false
                isRotationEnabled = false
                setEntryLabelColor(Color.BLACK)
                setEntryLabelTextSize(12f)
                animateY(1400, Easing.EaseInOutQuad)
                animate()
            }

            dialogAppUsePiechartBinding.okButton.setOnClickListener(){
                if (dialogAppUsePiechartBinding.root.parent != null) {
                    (dialogAppUsePiechartBinding.root.parent as ViewGroup).removeView(
                        dialogAppUsePiechartBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    dialog.dismiss()
                }
            }
        }

        bind.timeBarConstraintLayout.setOnClickListener(){

            var interActiveScreenRecord = mutableListOf<Boolean>()
            var emptyInterActiveScreenRecordJson = gson.toJson(interActiveScreenRecord)

            var interActiveScreenRecordJson = sharedPref.getString("interActiveScreenRecord"+timeInfo,emptyInterActiveScreenRecordJson).toString()
            interActiveScreenRecord = gson.fromJson(interActiveScreenRecordJson)

            // dialog로 상태 출력
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if (dialogGraphBinding.root.parent != null) {
                (dialogGraphBinding.root.parent as ViewGroup).removeView(
                    dialogGraphBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                dialog.dismiss()
            }
            dialog.setContentView(dialogGraphBinding.root)
            var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
            params.width = (context.getResources()
                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
            params.height = (context.getResources()
                .getDisplayMetrics().heightPixels * 0.5).toInt() // device의 세로 길이에 비례하여  결정
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.getWindow()!!.setAttributes(params)
            dialog.getWindow()!!.setGravity(Gravity.CENTER)
            dialog.setCancelable(true)
            dialog.show()

            //dialogGraphBinding.graphResultTextView.setText(timeInfo)
            setChartData(interActiveScreenRecord)
        }

        bind.forbiddenAppConstraintLayout.setOnClickListener(){
            //눌렀을 때 그에 대한 dialog를 띄워야함 (누르기 전에 띄우면 마지막 item에 대한 dialog로 바뀜)
            var appInfoListRecyclerview = dialogAppListBinding.appListRecyclerview
            var appInfoListRecyclerviewAdapter = setAppInfoListRecyclerview(appInfoListRecyclerview, timeInfo)

            val appListDialog = Dialog(context)
            appListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if(dialogAppListBinding.root.parent != null){
                (dialogAppListBinding.root.parent as ViewGroup).removeView(
                    dialogAppListBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                appListDialog.dismiss()
            }
            appListDialog.setContentView(dialogAppListBinding.root)
            var params: WindowManager.LayoutParams = appListDialog.getWindow()!!.getAttributes()
            params.width = (context.getResources()
                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
            params.height = (context.getResources()
                .getDisplayMetrics().heightPixels * 0.7).toInt() // device의 세로 길이에 비례하여  결정
            appListDialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            appListDialog.getWindow()!!.setAttributes(params)
            appListDialog.getWindow()!!.setGravity(Gravity.CENTER)
            appListDialog.setCancelable(true)
            appListDialog.show()

            dialogAppListBinding.okButton.setOnClickListener() {
                // 이미지 변경
                var list = AppUtil.loadNotUseAppList(context, timeInfo)

                bind.forbiddenAppOne.setImageDrawable(null)
                bind.forbiddenAppTwo.setImageDrawable(null)
                bind.forbiddenAppThree.setImageDrawable(null)
                bind.forbiddenAppFour.setImageDrawable(null)

                if(list.size >= 1) bind.forbiddenAppOne.setImageDrawable(list[0].loadIcon(context.packageManager))
                if(list.size >= 2) bind.forbiddenAppTwo.setImageDrawable(list[1].loadIcon(context.packageManager))
                if(list.size >= 3) bind.forbiddenAppThree.setImageDrawable(list[2].loadIcon(context.packageManager))
                if(list.size >= 4) bind.forbiddenAppFour.setImageDrawable(list[3].loadIcon(context.packageManager))

                if (dialogAppListBinding.root.parent != null) {
                    (dialogAppListBinding.root.parent as ViewGroup).removeView(
                        dialogAppListBinding.root
                    ) // 남아 있는 view 삭제
                    appListDialog.dismiss()
                }
            }
        }
    }

    fun setAppInfoListRecyclerview(recyclerView: RecyclerView, timeInfo:String): AdapterAppInfoList {
        val installedApps = AppUtil.getInstalledApp(context)

        recyclerView.adapter = AdapterAppInfoList(context, installedApps, timeInfo)
        val adapter = recyclerView.adapter as AdapterAppInfoList
        val linearLayoutManager = LinearLayoutManagerWrapper(context)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        return adapter
    }

    @SuppressLint("SetTextI18n")
    fun setChartData(records: MutableList<Boolean>){
        var chartData = LineData()
        var entry = ArrayList<Entry>()
        var count = 0
        var offCount = 0

        for(record in records){
            if(record == false){
                entry.add(Entry(count.toFloat(), 0f))
                offCount +=1
            }
            if(record == true){
                entry.add(Entry(count.toFloat(), 1f))
            }
            count +=1
        }

        var lineDataSet = LineDataSet(entry, "화면")
        chartData.addDataSet(lineDataSet)
        dialogGraphBinding.linechart.setData(chartData)
        dialogGraphBinding.linechart.invalidate()

        var offRatio = ((offCount.toFloat() / count.toFloat()) *100).toInt()
        dialogGraphBinding.graphResultTextView.setText("금지 앱 미사용 비율: "+ offRatio +"%")

    }
}