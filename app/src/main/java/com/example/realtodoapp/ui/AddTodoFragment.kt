package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.adapter.AdapterAppInfoList
import com.example.realtodoapp.adapter.AdapterFeedList
import com.example.realtodoapp.databinding.*
import com.example.realtodoapp.model.FeedDto
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.util.AppUtil
import com.example.realtodoapp.util.LinearLayoutManagerWrapper
import com.example.realtodoapp.util.RetrofitUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.streams.toList

class AddTodoFragment : Fragment(){
    lateinit var fragmentAddTodoBinding: FragmentAddTodoBinding
    lateinit var dialogAppListBinding:DialogAppListBinding
    lateinit var dialogMapBinding: DialogMapBinding

    lateinit var dialogAddTodoNotAutoBinding:DialogAddTodoNotAutoBinding
    lateinit var dialogAddTodoAutoAppBinding: DialogAddTodoAutoAppBinding
    lateinit var dialogAddTodoAutoLocationBinding: DialogAddTodoAutoLocationBinding

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()

    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor
    lateinit var todoList: MutableList<TodoPackageDto>
    lateinit var emptyTodoListJson:String
    lateinit var todoListJson:String

    var saveLatitude = 0.0
    var saveLongitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("CheckResult", "CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentAddTodoBinding = FragmentAddTodoBinding.inflate(layoutInflater)
        dialogAppListBinding = DialogAppListBinding.inflate(layoutInflater)
        dialogMapBinding = DialogMapBinding.inflate(layoutInflater)

        dialogAddTodoNotAutoBinding =DialogAddTodoNotAutoBinding.inflate(layoutInflater)
        dialogAddTodoAutoAppBinding = DialogAddTodoAutoAppBinding.inflate(layoutInflater)
        dialogAddTodoAutoLocationBinding = DialogAddTodoAutoLocationBinding.inflate(layoutInflater)

        sharedPref = requireContext().getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()


        todoList = mutableListOf<TodoPackageDto>()
        emptyTodoListJson = gson.toJson(todoList)
        todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()

        todoList = gson.fromJson(todoListJson) // 기기에 있는 todoList 가져옴

        //dialog()

        // 현재 시간으로 default editText 설정
        val current = LocalDateTime.now()
        val yearFormatter = DateTimeFormatter.ofPattern("yyyy")
        val monthFormatter = DateTimeFormatter.ofPattern("MM")
        val dayFormatter = DateTimeFormatter.ofPattern("dd")
        val hourFormatter = DateTimeFormatter.ofPattern("HH")
        val minuteFormatter = DateTimeFormatter.ofPattern("mm")
        fragmentAddTodoBinding.todoYearEditText.setText(current.format(yearFormatter))
        fragmentAddTodoBinding.todoMonthEditText.setText(current.format(monthFormatter))
        fragmentAddTodoBinding.todoDayEditText.setText(current.format(dayFormatter))

        dialogAddTodoNotAutoBinding.todoHourEditText.setText(current.format(hourFormatter))
        dialogAddTodoNotAutoBinding.todoMinuteEditText.setText(current.format(minuteFormatter))
        dialogAddTodoAutoAppBinding.todoHourEditText.setText(current.format(hourFormatter))
        dialogAddTodoAutoAppBinding.todoMinuteEditText.setText(current.format(minuteFormatter))
        dialogAddTodoAutoLocationBinding.todoHourEditText.setText(current.format(hourFormatter))
        dialogAddTodoAutoLocationBinding.todoMinuteEditText.setText(current.format(minuteFormatter))

        fragmentAddTodoBinding.appAutoButton.setOnClickListener(){
            setAutoAppDialog()
        }
        fragmentAddTodoBinding.locationAutoButton.setOnClickListener(){
            setLocationAutoDialog()
        }
        fragmentAddTodoBinding.notAutoButton.setOnClickListener(){
            setNotAutoDialog()
        }
        fragmentAddTodoBinding.appRoutineAutoButton.setOnClickListener(){
            var year = fragmentAddTodoBinding.todoYearEditText.getText().toString()
            var month = fragmentAddTodoBinding.todoMonthEditText.getText().toString()
            var day = fragmentAddTodoBinding.todoDayEditText.getText().toString()
            var name = fragmentAddTodoBinding.todoNameEditText.getText().toString()

            val bundle = bundleOf("year" to year, "month" to month, "day" to day, "name" to name)
            findNavController().navigate(R.id.action_addTodoFragment_to_appRoutineFragment, bundle)
        }


        val view = fragmentAddTodoBinding.root
        return view
    }

    fun setAppInfoListRecyclerview(recyclerView: RecyclerView, timeInfo:String): AdapterAppInfoList {
        val installedApps = AppUtil.getInstalledApp(requireContext())

        recyclerView.adapter = AdapterAppInfoList(requireContext(), installedApps, timeInfo)
        val adapter = recyclerView.adapter as AdapterAppInfoList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        return adapter
    }

    fun setNotAutoDialog(){
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if(dialogAddTodoNotAutoBinding.root.parent != null){
            (dialogAddTodoNotAutoBinding.root.parent as ViewGroup).removeView(
                dialogAddTodoNotAutoBinding.root
            ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
            dialog.dismiss()
        }
        dialog.setContentView(dialogAddTodoNotAutoBinding.root)
        var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
        params.width = (requireContext().getResources()
            .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
        params.height = (requireContext().getResources()
            .getDisplayMetrics().heightPixels * 0.3).toInt() // device의 세로 길이에 비례하여  결정
        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.getWindow()!!.setAttributes(params)
        dialog.getWindow()!!.setGravity(Gravity.CENTER)
        dialog.setCancelable(true)
        dialog.show()

        // 확인 버튼 클릭 시 todo가 저장되도록 함
        dialogAddTodoNotAutoBinding.okButton.setOnClickListener(){
            var newTodo = TodoPackageDto()
            var year = fragmentAddTodoBinding.todoYearEditText.getText().toString()
            var month = fragmentAddTodoBinding.todoMonthEditText.getText().toString()
            var day = fragmentAddTodoBinding.todoDayEditText.getText().toString()

            // 목표 시간 설정
            var hour = dialogAddTodoNotAutoBinding.todoHourEditText.getText().toString()
            var minute = dialogAddTodoNotAutoBinding.todoMinuteEditText.getText().toString()

            // todo설정하여 기기에 저장
            newTodo.year = Integer.parseInt(year)
            newTodo.month = Integer.parseInt(month)
            newTodo.day = Integer.parseInt(day)

            if(dialogAddTodoNotAutoBinding.disableTimeCheckBox.isChecked){
                newTodo.name = fragmentAddTodoBinding.todoNameEditText.getText().toString()
                newTodo.time = "TODAY"
                todoList.add(newTodo)
            }
            else {
                newTodo.name = fragmentAddTodoBinding.todoNameEditText.getText().toString()
                if (hour != "") newTodo.hour = Integer.parseInt(hour)
                if (minute != "") newTodo.minute = Integer.parseInt(minute)

                newTodo.time = String.format("%02d", newTodo.hour) + ":" + String.format(
                    "%02d",
                    newTodo.minute
                )
                todoList.add(newTodo)
            }

            todoListJson = gson.toJson(todoList)

            sharedPrefEditor.putString("myTodoList", todoListJson)
            sharedPrefEditor.commit()

            // mainFragment로 이동
            if(dialogAddTodoNotAutoBinding.root.parent != null) {
                dialog.dismiss()
                (dialogAddTodoNotAutoBinding.root.parent as ViewGroup).removeView(
                    dialogAddTodoNotAutoBinding.root
                ) // 다음에 쓰기 위해 view 삭제
            }

            findNavController().popBackStack()
        }
    }

    fun setLocationAutoDialog(){
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if(dialogAddTodoAutoLocationBinding.root.parent != null){
            (dialogAddTodoAutoLocationBinding.root.parent as ViewGroup).removeView(
                dialogAddTodoAutoLocationBinding.root
            ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
            dialog.dismiss()
        }
        dialog.setContentView(dialogAddTodoAutoLocationBinding.root)
        var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
        params.width = (requireContext().getResources()
            .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
        params.height = (requireContext().getResources()
            .getDisplayMetrics().heightPixels * 0.3).toInt() // device의 세로 길이에 비례하여  결정
        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.getWindow()!!.setAttributes(params)
        dialog.getWindow()!!.setGravity(Gravity.CENTER)
        dialog.setCancelable(true)
        dialog.show()

        // 목표 위치 선택 dialog 띄움
        dialogAddTodoAutoLocationBinding.selectLocationButton.setOnClickListener(){
            val mapDialog = Dialog(requireContext())
            mapDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if(dialogMapBinding.root.parent != null){
                (dialogMapBinding.root.parent as ViewGroup).removeView(
                    dialogMapBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                mapDialog.dismiss()
            }
            mapDialog.setContentView(dialogMapBinding.root)
            var params: WindowManager.LayoutParams = mapDialog.getWindow()!!.getAttributes()
            params.width = (requireContext().getResources()
                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
            params.height = (requireContext().getResources()
                .getDisplayMetrics().heightPixels * 0.7).toInt() // device의 세로 길이에 비례하여  결정
            mapDialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            mapDialog.getWindow()!!.setAttributes(params)
            mapDialog.getWindow()!!.setGravity(Gravity.CENTER)
            mapDialog.setCancelable(true)
            mapDialog.show()

            dialogMapBinding.textView.setText("목표 위치 설정")

            // 목표 위치를 지도에서 설정
            MapsInitializer.initialize(requireContext())
            dialogMapBinding.mapInDialog.onCreate(dialog.onSaveInstanceState())
            dialogMapBinding.mapInDialog.onResume()

            dialogMapBinding.mapInDialog.getMapAsync(OnMapReadyCallback {
                if(saveLatitude == 0.0 && saveLongitude == 0.0){
                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.4921, 126.9730), 8F))
                }
                else{
                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(saveLatitude, saveLongitude), 16F))
                }

                it.setOnMapClickListener (object: GoogleMap.OnMapClickListener {
                    override fun onMapClick(latLng: LatLng) {
                        it.clear()

                        it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16F))

                        val location = LatLng(latLng.latitude,latLng.longitude)
                        it.addMarker(MarkerOptions().position(location))

                        // 현재 찍은 위치를 전역변수로 저장해둠
                        saveLatitude = latLng.latitude
                        saveLongitude = latLng.longitude
                    }

                })
            })
            dialogMapBinding.okButton.setOnClickListener(){
                if(dialogMapBinding.root.parent != null){
                    (dialogMapBinding.root.parent as ViewGroup).removeView(
                        dialogMapBinding.root
                    ) // 남아 있는 view 삭제
                    mapDialog.dismiss()
                }
            }
        }

        // 확인 버튼 클릭 시 todo가 저장되도록 함
        dialogAddTodoAutoLocationBinding.okButton.setOnClickListener(){
            var newTodo = TodoPackageDto()
            var year = fragmentAddTodoBinding.todoYearEditText.getText().toString()
            var month = fragmentAddTodoBinding.todoMonthEditText.getText().toString()
            var day = fragmentAddTodoBinding.todoDayEditText.getText().toString()

            // 목표 시간 설정
            var hour = dialogAddTodoAutoLocationBinding.todoHourEditText.getText().toString()
            var minute = dialogAddTodoAutoLocationBinding.todoMinuteEditText.getText().toString()

            // todo설정하여 기기에 저장
            newTodo.year = Integer.parseInt(year)
            newTodo.month = Integer.parseInt(month)
            newTodo.day = Integer.parseInt(day)

            newTodo.certType = "LOCATE_AUTO"

            newTodo.name = fragmentAddTodoBinding.todoNameEditText.getText().toString()
            if(hour != "") newTodo.hour = Integer.parseInt(hour)
            if(minute != "") newTodo.minute = Integer.parseInt(minute)

            newTodo.time = String.format("%02d",newTodo.hour) +":" + String.format("%02d",newTodo.minute)
            todoList.add(newTodo)

            todoListJson = gson.toJson(todoList)

            sharedPrefEditor.putString("myTodoList", todoListJson)
            sharedPrefEditor.commit()

            // 목표 위치 저장
            // todo마다 다른 목표 위치를 저장할 필요가 있으므로 각기 다른 곳에 저장
            var sharedPrefKeyGoal = "goalLocateRecord"+
                    newTodo.year+newTodo.month+newTodo.day+newTodo.hour.toString()+newTodo.minute.toString()
            var goalLocateRecord = mutableListOf<Double>()

            // 전역변수로 저장해둔 위치 불러오기
            goalLocateRecord.add(saveLatitude)
            goalLocateRecord.add(saveLongitude)

            var goalLocateRecordJson = gson.toJson(goalLocateRecord)

            sharedPrefEditor.putString(sharedPrefKeyGoal, goalLocateRecordJson) // 시간별로 따로 저장
            sharedPrefEditor.commit()

            // mainFragment로 이동
            if(dialogAddTodoAutoLocationBinding.root.parent != null) {
                dialog.dismiss()
                (dialogAddTodoAutoLocationBinding.root.parent as ViewGroup).removeView(
                    dialogAddTodoAutoLocationBinding.root
                ) // 다음에 쓰기 위해 view 삭제
            }

            findNavController().popBackStack()
        }
    }

    fun setAutoAppDialog(){
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if(dialogAddTodoAutoAppBinding.root.parent != null){
            (dialogAddTodoAutoAppBinding.root.parent as ViewGroup).removeView(
                dialogAddTodoAutoAppBinding.root
            ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
            dialog.dismiss()
        }
        dialog.setContentView(dialogAddTodoAutoAppBinding.root)
        var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
        params.width = (requireContext().getResources()
            .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
        params.height = (requireContext().getResources()
            .getDisplayMetrics().heightPixels * 0.3).toInt() // device의 세로 길이에 비례하여  결정
        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.getWindow()!!.setAttributes(params)
        dialog.getWindow()!!.setGravity(Gravity.CENTER)
        dialog.setCancelable(true)
        dialog.show()

        // 앱 선택 dialog 띄움
        dialogAddTodoAutoAppBinding.selectAppButton.setOnClickListener(){
            val appListDialog = Dialog(requireContext())
            appListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if(dialogAppListBinding.root.parent != null){
                (dialogAppListBinding.root.parent as ViewGroup).removeView(
                    dialogAppListBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                appListDialog.dismiss()
            }
            appListDialog.setContentView(dialogAppListBinding.root)
            var params: WindowManager.LayoutParams = appListDialog.getWindow()!!.getAttributes()
            params.width = (requireContext().getResources()
                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
            params.height = (requireContext().getResources()
                .getDisplayMetrics().heightPixels * 0.7).toInt() // device의 세로 길이에 비례하여  결정
            appListDialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            appListDialog.getWindow()!!.setAttributes(params)
            appListDialog.getWindow()!!.setGravity(Gravity.CENTER)
            appListDialog.setCancelable(true)
            appListDialog.show()

            var year = fragmentAddTodoBinding.todoYearEditText.getText().toString().toInt()
            var month = fragmentAddTodoBinding.todoMonthEditText.getText().toString().toInt()
            var day = fragmentAddTodoBinding.todoDayEditText.getText().toString().toInt()

            // 시작 시간 설정
            var startHour = dialogAddTodoAutoAppBinding.todoHourEditText.getText().toString().toInt()
            var startMinute = dialogAddTodoAutoAppBinding.todoMinuteEditText.getText().toString().toInt()

            // 종료 시간 설정
            var endHour = dialogAddTodoAutoAppBinding.todoEndHourEditText.getText().toString().toInt()
            var endMinute = dialogAddTodoAutoAppBinding.todoEndMinuteEditText.getText().toString().toInt()

            // 시작시간, 종료 시간 담은 timeInfo 생성
            var startTimeString = year.toString()+"-"+String.format("%02d",month)+"-"+String.format("%02d",day)+"-"+String.format("%02d",startHour)+"-"+String.format("%02d",startMinute)
            var endTimeString = year.toString()+"-"+String.format("%02d",month)+"-"+String.format("%02d",day)+"-"+String.format("%02d",endHour)+"-"+String.format("%02d",endMinute) // 두자리수로 맞춰줌
            var timeInfo = startTimeString + endTimeString


            var appInfoListRecyclerview = dialogAppListBinding.appListRecyclerview
            var appInfoListRecyclerviewAdapter = setAppInfoListRecyclerview(appInfoListRecyclerview, timeInfo)

            dialogAppListBinding.okButton.setOnClickListener() {
                if (dialogAppListBinding.root.parent != null) {
                    (dialogAppListBinding.root.parent as ViewGroup).removeView(
                        dialogAppListBinding.root
                    ) // 남아 있는 view 삭제
                    appListDialog.dismiss()
                }
            }
        }

        // 확인 버튼 클릭 시 todo가 저장되도록 함
        dialogAddTodoAutoAppBinding.okButton.setOnClickListener(){
            var newTodo = TodoPackageDto()
            var year = fragmentAddTodoBinding.todoYearEditText.getText().toString()
            var month = fragmentAddTodoBinding.todoMonthEditText.getText().toString()
            var day = fragmentAddTodoBinding.todoDayEditText.getText().toString()

            // 시작 시간 설정
            var hour = dialogAddTodoAutoAppBinding.todoHourEditText.getText().toString()
            var minute = dialogAddTodoAutoAppBinding.todoMinuteEditText.getText().toString()

            // 종료 시간 설정
            var endHour = dialogAddTodoAutoAppBinding.todoEndHourEditText.getText().toString()
            var endMinute = dialogAddTodoAutoAppBinding.todoEndMinuteEditText.getText().toString()

            // todo설정하여 기기에 저장
            newTodo.year = Integer.parseInt(year)
            newTodo.month = Integer.parseInt(month)
            newTodo.day = Integer.parseInt(day)

            newTodo.certType = "SCREEN_AUTO"

            newTodo.name = fragmentAddTodoBinding.todoNameEditText.getText().toString()
            if(hour != "") newTodo.hour = Integer.parseInt(hour)
            if(minute != "") newTodo.minute = Integer.parseInt(minute)
            if(endHour != "") newTodo.endHour = Integer.parseInt(endHour)
            if(endMinute != "") newTodo.endMinute = Integer.parseInt(endMinute)

            newTodo.time = String.format("%02d",newTodo.hour) +":" + String.format("%02d",newTodo.minute)
            todoList.add(newTodo)

            todoListJson = gson.toJson(todoList)

            sharedPrefEditor.putString("myTodoList", todoListJson)
            sharedPrefEditor.commit()

            // mainFragment로 이동
            if(dialogAddTodoAutoAppBinding.root.parent != null) {
                dialog.dismiss()
                (dialogAddTodoAutoAppBinding.root.parent as ViewGroup).removeView(
                    dialogAddTodoAutoAppBinding.root
                ) // 다음에 쓰기 위해 view 삭제
            }
            findNavController().popBackStack()
        }
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    fun dialog(){
//        // todolist 생성 dialog 띄움
//        val dialog = Dialog(requireContext())
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        if(dialogAddTodoBinding.root.parent != null){
//            (dialogAddTodoBinding.root.parent as ViewGroup).removeView(
//                dialogAddTodoBinding.root
//            ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
//            dialog.dismiss()
//        }
//        dialog.setContentView(dialogAddTodoBinding.root)
//        var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
//        params.width = (requireContext().getResources()
//            .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
//        params.height = (requireContext().getResources()
//            .getDisplayMetrics().heightPixels * 0.5).toInt() // device의 세로 길이에 비례하여  결정
//        dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.getWindow()!!.setAttributes(params)
//        dialog.getWindow()!!.setGravity(Gravity.CENTER)
//        dialog.setCancelable(true)
//        dialog.show()
//
//        // 현재 시간으로 default editText 설정
//        val current = LocalDateTime.now()
//        val yearFormatter = DateTimeFormatter.ofPattern("yyyy")
//        val monthFormatter = DateTimeFormatter.ofPattern("MM")
//        val dayFormatter = DateTimeFormatter.ofPattern("dd")
//        val hourFormatter = DateTimeFormatter.ofPattern("HH")
//        val minuteFormatter = DateTimeFormatter.ofPattern("mm")
//        dialogAddTodoBinding.todoYearEditText.setText(current.format(yearFormatter))
//        dialogAddTodoBinding.todoMonthEditText.setText(current.format(monthFormatter))
//        dialogAddTodoBinding.todoDayEditText.setText(current.format(dayFormatter))
//        dialogAddTodoBinding.todoHourEditText.setText(current.format(hourFormatter))
//        dialogAddTodoBinding.todoMinuteEditText.setText(current.format(minuteFormatter))
//
//        // 예외 앱 선택 기능 띄우기
//        dialogAddTodoBinding.selectAppButton.setOnClickListener(){
//            // dialog 띄움
//            val appListDialog = Dialog(requireContext())
//            appListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//            if(dialogAppListBinding.root.parent != null){
//                (dialogAppListBinding.root.parent as ViewGroup).removeView(
//                    dialogAppListBinding.root
//                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
//                appListDialog.dismiss()
//            }
//            appListDialog.setContentView(dialogAppListBinding.root)
//            var params: WindowManager.LayoutParams = appListDialog.getWindow()!!.getAttributes()
//            params.width = (requireContext().getResources()
//                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
//            params.height = (requireContext().getResources()
//                .getDisplayMetrics().heightPixels * 0.7).toInt() // device의 세로 길이에 비례하여  결정
//            appListDialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//            appListDialog.getWindow()!!.setAttributes(params)
//            appListDialog.getWindow()!!.setGravity(Gravity.CENTER)
//            appListDialog.setCancelable(true)
//            appListDialog.show()
//
//            var appInfoListRecyclerview = dialogAppListBinding.appListRecyclerview
//            var appInfoListRecyclerviewAdapter = setAppInfoListRecyclerview(appInfoListRecyclerview)
//
//            dialogAppListBinding.okButton.setOnClickListener() {
//                if (dialogAppListBinding.root.parent != null) {
//                    (dialogAppListBinding.root.parent as ViewGroup).removeView(
//                        dialogAppListBinding.root
//                    ) // 남아 있는 view 삭제
//                    appListDialog.dismiss()
//                }
//            }
//        }
//
//        // 지도에서 목표 위치 선택 기능 띄우기
//        dialogAddTodoBinding.selectLocateButton.setOnClickListener(){
//            // dialog 띄움
//            val mapDialog = Dialog(requireContext())
//            mapDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//            if(dialogMapBinding.root.parent != null){
//                (dialogMapBinding.root.parent as ViewGroup).removeView(
//                    dialogMapBinding.root
//                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
//                mapDialog.dismiss()
//            }
//            mapDialog.setContentView(dialogMapBinding.root)
//            var params: WindowManager.LayoutParams = mapDialog.getWindow()!!.getAttributes()
//            params.width = (requireContext().getResources()
//                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
//            params.height = (requireContext().getResources()
//                .getDisplayMetrics().heightPixels * 0.7).toInt() // device의 세로 길이에 비례하여  결정
//            mapDialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//            mapDialog.getWindow()!!.setAttributes(params)
//            mapDialog.getWindow()!!.setGravity(Gravity.CENTER)
//            mapDialog.setCancelable(true)
//            mapDialog.show()
//
//            dialogMapBinding.textView.setText("목표 위치 설정")
//
//            // 목표 위치를 지도에서 설정
//            MapsInitializer.initialize(requireContext())
//            dialogMapBinding.mapInDialog.onCreate(dialog.onSaveInstanceState())
//            dialogMapBinding.mapInDialog.onResume()
//
//            dialogMapBinding.mapInDialog.getMapAsync(OnMapReadyCallback {
//                if(saveLatitude == 0.0 && saveLongitude == 0.0){
//                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.4921, 126.9730), 8F))
//                }
//                else{
//                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(saveLatitude, saveLongitude), 16F))
//                }
//
//                it.setOnMapClickListener (object: GoogleMap.OnMapClickListener {
//                    override fun onMapClick(latLng: LatLng) {
//                        it.clear()
//
//                        it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16F))
//
//                        val location = LatLng(latLng.latitude,latLng.longitude)
//                        it.addMarker(MarkerOptions().position(location))
//
//                        // 현재 찍은 위치를 전역변수로 저장해둠
//                        saveLatitude = latLng.latitude
//                        saveLongitude = latLng.longitude
//                    }
//
//                })
//            })
//            dialogMapBinding.okButton.setOnClickListener(){
//                if(dialogMapBinding.root.parent != null){
//                    (dialogMapBinding.root.parent as ViewGroup).removeView(
//                        dialogMapBinding.root
//                    ) // 남아 있는 view 삭제
//                    mapDialog.dismiss()
//                }
//            }
//        }
//
//        //  체크 항목 변경
//        dialogAddTodoBinding.autoScreenCheckBox.setOnCheckedChangeListener{ _, isChecked ->
//            if(isChecked){
//                dialogAddTodoBinding.autoLocateCheckBox.setChecked(false)
//            }
//        }
//        dialogAddTodoBinding.autoLocateCheckBox.setOnCheckedChangeListener{ _, isChecked ->
//            if(isChecked){
//                dialogAddTodoBinding.autoScreenCheckBox.setChecked(false)
//            }
//        }
//
//
//        dialogAddTodoBinding.okButton.setOnClickListener(){
//            var newTodo = TodoPackageDto()
//            var year = dialogAddTodoBinding.todoYearEditText.getText().toString()
//            var month = dialogAddTodoBinding.todoMonthEditText.getText().toString()
//            var day = dialogAddTodoBinding.todoDayEditText.getText().toString()
//            var hour = dialogAddTodoBinding.todoHourEditText.getText().toString()
//            var minute = dialogAddTodoBinding.todoMinuteEditText.getText().toString()
//
//            // 종료 시간 있을 때 설정
//            var endHour = dialogAddTodoBinding.todoEndHourEditText.getText().toString()
//            var endMinute = dialogAddTodoBinding.todoEndMinuteEditText.getText().toString()
//
//            newTodo.year = Integer.parseInt(year)
//            newTodo.month = Integer.parseInt(month)
//            newTodo.day = Integer.parseInt(day)
//
//            if(dialogAddTodoBinding.autoScreenCheckBox.isChecked){
//                newTodo.certType = "SCREEN_AUTO"
//            }
//            else if(dialogAddTodoBinding.autoLocateCheckBox.isChecked){
//                newTodo.certType = "LOCATE_AUTO"
//            }
//
//
//            if(dialogAddTodoBinding.disableTimeCheckBox.isChecked){
//                newTodo.name = dialogAddTodoBinding.todoNameEditText.getText().toString()
//                newTodo.time = "TODAY"
//                todoList.add(newTodo)
//            }
//            else{
//                newTodo.name = dialogAddTodoBinding.todoNameEditText.getText().toString()
//                if(hour != "") newTodo.hour = Integer.parseInt(hour)
//                if(minute != "") newTodo.minute = Integer.parseInt(minute)
//                if(endHour != "") newTodo.endHour = Integer.parseInt(endHour)
//                if(endMinute != "") newTodo.endMinute = Integer.parseInt(endMinute)
//
//                newTodo.time = String.format("%02d",newTodo.hour) +":" + String.format("%02d",newTodo.minute)
//                todoList.add(newTodo)
//
//                // 목표 위치 저장
//                if(newTodo.certType == "LOCATE_AUTO"){
//                    // todo마다 다른 목표 위치를 저장할 필요가 있으므로 각기 다른 곳에 저장
//                    var sharedPrefKeyGoal = "goalLocateRecord"+
//                            newTodo.year+newTodo.month+newTodo.day+newTodo.hour.toString()+newTodo.minute.toString()
//                    var goalLocateRecord = mutableListOf<Double>()
//
//                    // 전역변수로 저장해둔 위치 불러오기
//                    goalLocateRecord.add(saveLatitude)
//                    goalLocateRecord.add(saveLongitude)
//
//                    var goalLocateRecordJson = gson.toJson(goalLocateRecord)
//
//                    sharedPrefEditor.putString(sharedPrefKeyGoal, goalLocateRecordJson) // 시간별로 따로 저장
//                    sharedPrefEditor.commit()
//                }
//            } // 시간 설정 여부에 따라 다른 방식으로 dto 추가
//
//            todoListJson = gson.toJson(todoList)
//
//            sharedPrefEditor.putString("myTodoList", todoListJson)
//            sharedPrefEditor.commit()
//
//            if(dialogAddTodoBinding.root.parent != null) {
//                dialog.dismiss()
//                (dialogAddTodoBinding.root.parent as ViewGroup).removeView(
//                    dialogAddTodoBinding.root
//                ) // 다음에 쓰기 위해 view 삭제
//            }
//        }
//    }
}