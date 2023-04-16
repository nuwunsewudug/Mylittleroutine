package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.adapter.AdapterAppRoutineForTimeList
import com.example.realtodoapp.databinding.*
import com.example.realtodoapp.model.AppRoutineForTimeDto
import com.example.realtodoapp.model.MemberInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.util.AppUtil
import com.example.realtodoapp.util.LinearLayoutManagerWrapper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppRoutineFragment: Fragment() {
    lateinit var fragmentAppRoutineBinding: FragmentAppRoutineBinding
    lateinit var dialogAppListBinding: DialogAppListBinding
    lateinit var dialogAddAppRoutineBinding: DialogAddAppRoutineBinding
    lateinit var dialogGraphBinding: DialogGraphBinding
    lateinit var dialogAppUsePiechartBinding: DialogAppUsePiechartBinding

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()

    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPref = requireContext().getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()

        fragmentAppRoutineBinding = FragmentAppRoutineBinding.inflate(layoutInflater)
        dialogAppListBinding = DialogAppListBinding.inflate(layoutInflater)
        dialogAddAppRoutineBinding = DialogAddAppRoutineBinding.inflate(layoutInflater)
        dialogGraphBinding = DialogGraphBinding.inflate(layoutInflater)
        dialogAppUsePiechartBinding = DialogAppUsePiechartBinding.inflate(layoutInflater)

        val appRoutineRecyclerView = fragmentAppRoutineBinding.appRoutineRecyclerview

        // 해당 날짜의 appRoutine목록 불러옴
        var appRoutineList = mutableListOf<AppRoutineForTimeDto>()
        var emptyAppRoutineListJson = gson.toJson(appRoutineList)

        var year = String.format("%02d",arguments?.get("year").toString().toInt())
        var month = String.format("%02d",arguments?.get("month").toString().toInt())
        var day = String.format("%02d",arguments?.get("day").toString().toInt())
        var name = arguments?.get("name").toString()


        var appRoutineListJson =
            sharedPref.getString("appRoutineList"+year+month+day, emptyAppRoutineListJson).toString()
        appRoutineList = gson.fromJson(appRoutineListJson)

        appRoutineList.sortBy { it.startHour }

        var appRoutineForTimeAdpater = setAppRoutineForTimeListRecyclerview(appRoutineRecyclerView, appRoutineList, dialogAppListBinding, dialogGraphBinding, dialogAppUsePiechartBinding)

        // 메인 화면에 todo로 추가
        fragmentAppRoutineBinding.addTodoButton.setOnClickListener(){
            var todoList = mutableListOf<TodoPackageDto>()
            var emptyTodoListJson = gson.toJson(todoList)
            var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()

            todoList = gson.fromJson(todoListJson) // 기기에 있는 todoList 가져옴

            var newTodo = TodoPackageDto()

            // todo설정하여 기기에 저장
            newTodo.year = Integer.parseInt(year)
            newTodo.month = Integer.parseInt(month)
            newTodo.day = Integer.parseInt(day)

            newTodo.name = name
            newTodo.time = "TODAY"
            newTodo.certType = "AppRoutine"
            todoList.add(newTodo)

            todoListJson = gson.toJson(todoList)

            sharedPrefEditor.putString("myTodoList", todoListJson)
            sharedPrefEditor.commit()

            // 메인 화면으로 이동
            findNavController().popBackStack()
            findNavController().popBackStack()
        }


        // 앱 루틴 추가
        fragmentAppRoutineBinding.addRoutineButton.setOnClickListener(){
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if(dialogAddAppRoutineBinding.root.parent != null){
                (dialogAddAppRoutineBinding.root.parent as ViewGroup).removeView(
                    dialogAddAppRoutineBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                dialog.dismiss()
            }
            dialog.setContentView(dialogAddAppRoutineBinding.root)
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

            dialogAddAppRoutineBinding.okButton.setOnClickListener(){
                var startHour = dialogAddAppRoutineBinding.todoHourEditText.text.toString().toInt()
                var startMinute = dialogAddAppRoutineBinding.todoMinuteEditText.text.toString().toInt()
                var endHour = dialogAddAppRoutineBinding.todoEndHourEditText.text.toString().toInt()
                var endMinute = dialogAddAppRoutineBinding.todoEndMinuteEditText.text.toString().toInt()

                addRoutineTime(year, month, day, startHour, startMinute, endHour, endMinute)

                // 해당 날짜의 appRoutine목록 불러와 화면 업데이트
                var appRoutineList = mutableListOf<AppRoutineForTimeDto>()
                var emptyAppRoutineListJson = gson.toJson(appRoutineList)

                var year = String.format("%02d",arguments?.get("year").toString().toInt())
                var month = String.format("%02d",arguments?.get("month").toString().toInt())
                var day = String.format("%02d",arguments?.get("day").toString().toInt())

                var appRoutineListJson =
                    sharedPref.getString("appRoutineList"+year+month+day, emptyAppRoutineListJson).toString()
                appRoutineList = gson.fromJson(appRoutineListJson)

                appRoutineList.sortBy { it.startHour }

                appRoutineForTimeAdpater = setAppRoutineForTimeListRecyclerview(appRoutineRecyclerView, appRoutineList, dialogAppListBinding, dialogGraphBinding, dialogAppUsePiechartBinding)

                if(dialogAddAppRoutineBinding.root.parent != null){
                    (dialogAddAppRoutineBinding.root.parent as ViewGroup).removeView(
                        dialogAddAppRoutineBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    dialog.dismiss()
                }
            }
        }

        val view = fragmentAppRoutineBinding.root
        return view
    }

    fun addRoutineTime(year:String, month:String, day:String, startHour:Int, startMinute:Int, endHour:Int, endMinute:Int){
        var appRoutineList = mutableListOf<AppRoutineForTimeDto>()
        var emptyAppRoutineListJson = gson.toJson(appRoutineList)
        var appRoutineListJson =
            sharedPref.getString("appRoutineList"+year+month+day, emptyAppRoutineListJson).toString()
        appRoutineList = gson.fromJson(appRoutineListJson)


        var newAppRoutine = AppRoutineForTimeDto()
        newAppRoutine.year = year.toInt()
        newAppRoutine.month = month.toInt()
        newAppRoutine.day = day.toInt()

        newAppRoutine.startHour = startHour
        newAppRoutine.startMinute = startMinute
        newAppRoutine.endHour = endHour
        newAppRoutine.endMinute = endMinute

        appRoutineList.add(newAppRoutine)

        var appRoutineJson = gson.toJson(appRoutineList)
        sharedPrefEditor.putString("appRoutineList"+year+month+day, appRoutineJson)
        sharedPrefEditor.commit()
    }

    fun setAppRoutineForTimeListRecyclerview(recyclerView: RecyclerView, list:MutableList<AppRoutineForTimeDto>, dialogAppListBinding: DialogAppListBinding
    , dialogGraphBinding: DialogGraphBinding, dialogAppUsePiechartBinding: DialogAppUsePiechartBinding): AdapterAppRoutineForTimeList {

        recyclerView.adapter = AdapterAppRoutineForTimeList(requireContext(), list, dialogAppListBinding, dialogGraphBinding, dialogAppUsePiechartBinding)
        val adapter = recyclerView.adapter as AdapterAppRoutineForTimeList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        return adapter
    }
}