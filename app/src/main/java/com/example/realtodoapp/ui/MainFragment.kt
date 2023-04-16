package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.adapter.AdapterAppInfoList
import com.example.realtodoapp.adapter.AdapterDateInfoList
import com.example.realtodoapp.adapter.AdapterToDoPackageList
import com.example.realtodoapp.databinding.*
import com.example.realtodoapp.model.DateInfoDto
import com.example.realtodoapp.model.MemberInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.util.AppUtil
import com.example.realtodoapp.util.LinearLayoutManagerWrapper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainFragment : Fragment(){

    lateinit var fragmentMainBinding: FragmentMainBinding
    lateinit var itemTodoPackageBinding: ItemTodoPackageBinding
    lateinit var  dialogDefaultBinding: DialogDefaultBinding
    lateinit var dialogGraphBinding: DialogGraphBinding
    lateinit var dialogMapBinding: DialogMapBinding
    lateinit var dialogAppListBinding: DialogAppListBinding

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()

    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor :SharedPreferences.Editor

    var curYear = 0
    var curMonth = 0
    var curDay = 0

    var backKeyPressedTime = 0.toLong()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("CheckResult", "CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedPref = requireContext().getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()

        // Inflate the layout for this fragment
        fragmentMainBinding = FragmentMainBinding.inflate(layoutInflater)
        itemTodoPackageBinding = ItemTodoPackageBinding.inflate(layoutInflater)
        dialogDefaultBinding = DialogDefaultBinding.inflate(layoutInflater)
        dialogGraphBinding = DialogGraphBinding.inflate(layoutInflater)
        dialogMapBinding = DialogMapBinding.inflate(layoutInflater)
        dialogAppListBinding = DialogAppListBinding.inflate(layoutInflater)

        // 저장된 id 정보 불러오기
        var loginMemberInfo = MemberInfoDto()
        var emptyLoginMemberInfo = gson.toJson(loginMemberInfo)
        var loginMemberInfoJson = sharedPref.getString("loginMemberInfo",emptyLoginMemberInfo).toString()
        loginMemberInfo = gson.fromJson(loginMemberInfoJson)

        // 사용자 이름 업데이트
        fragmentMainBinding.userName.setText(loginMemberInfo.mem_name)

        var todoByDayRecyclerView = fragmentMainBinding.fragmentMainRecyclerView
        var todoByDayRecyclerViewAdapter = setTodoByDayRecyclerView(todoByDayRecyclerView)

        var todoByTimeRecyclerView = fragmentMainBinding.todoByTimeRecyclerView
        var todoByTimeRecyclerViewAdapter = setTodoByTimeRecyclerView(todoByTimeRecyclerView)

        var dateInfoRecyclerView = fragmentMainBinding.dateInfoRecyclerview
        var dateInfoRecyclerViewAdapter = setDateInfoRecyclerView(dateInfoRecyclerView)

        // 뒤로가기 시 mainFragment면 앱 종료
        requireActivity().onBackPressedDispatcher.addCallback {
            findNavController().currentDestination?.label?.let {
                if(it == "MainFragment"){
                    if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
                        backKeyPressedTime = System.currentTimeMillis()
                        Toast.makeText(requireContext(), "한번 더 누를 시 종료합니다", Toast.LENGTH_LONG).show()
                    }else if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
                        requireActivity().finish()
                    }
                    return@addCallback
                }
                else{
                    findNavController().popBackStack()
                }
            }
        }

        // 로그아웃
        fragmentMainBinding.logoutButton.setOnClickListener(){
            // 기기에 빈 로그인 정보 저장
            var emptyMember = MemberInfoDto()
            var loginMemberInfoJson = gson.toJson(emptyMember)
            sharedPrefEditor.putString("loginMemberInfo", loginMemberInfoJson)
            sharedPrefEditor.commit()

            // 로그인 화면으로 돌아가기
            findNavController().popBackStack()
        }

        fragmentMainBinding.writeReviewButton.setOnClickListener(){
            // fragment에 선택된 날짜 넘겨줌
            val bundle = bundleOf("curYear" to curYear.toString(), "curMonth" to curMonth.toString(), "curDay" to curDay.toString())
            findNavController().navigate(R.id.action_mainFragment_to_reviewFragment, bundle)
        }

        fragmentMainBinding.communityButton.setOnClickListener(){
            findNavController().navigate(R.id.action_mainFragment_to_communityFragment)
        }

        fragmentMainBinding.addTodoButton.setOnClickListener(){

            // todoRoutine 생성 fragment로 이동
            findNavController().navigate(R.id.action_mainFragment_to_addTodoFragment)

            // 다른 fragment 다녀올 시 onCreateView가 재실행되어 todo목록이 업데이트됨

        }

        fragmentMainBinding.deleteAllButton.setOnClickListener(){
            sharedPrefEditor.clear()
            sharedPrefEditor.commit()

            refreshTodoList()
        }

        val view = fragmentMainBinding.root
        return view
    }

    fun setTodoByDayRecyclerView(recyclerView: RecyclerView): AdapterToDoPackageList{
        var todoList = mutableListOf<TodoPackageDto>()
        var emptyTodoListJson = gson.toJson(todoList)

        var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()
        todoList = gson.fromJson(todoListJson)

        var filteredTodoList = mutableListOf<TodoPackageDto>()

        for(todo in todoList){
            if(todo.time == "TODAY" && todo.year == curYear && todo.month == curMonth && todo.day == curDay)
            {
                filteredTodoList.add(todo)
            }
        }

        recyclerView.adapter = AdapterToDoPackageList(requireActivity(), this, requireContext(), filteredTodoList, dialogDefaultBinding, dialogGraphBinding, dialogMapBinding)
        val adapter = recyclerView.adapter as AdapterToDoPackageList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        adapter.setManualCertOnClickListener(object: AdapterToDoPackageList.ManualCertOnClickListener{
            override fun onClick(item: TodoPackageDto) {
                updateSuccessTodo(item)
            }
        })

        return adapter
    }

    fun setTodoByTimeRecyclerView(recyclerView: RecyclerView): AdapterToDoPackageList{
        var todoList = mutableListOf<TodoPackageDto>()
        var emptyTodoListJson = gson.toJson(todoList)

        var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()
        todoList = gson.fromJson(todoListJson)

        var filteredTodoList = mutableListOf<TodoPackageDto>()

        for(todo in todoList){
            if(todo.time != "TODAY" && todo.year == curYear && todo.month == curMonth && todo.day == curDay)
            {
                filteredTodoList.add(todo)
            }
        }

        val comparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.hour * 60 + a.minute - b.hour * 60 - b.minute } // 시간순 정렬

        Collections.sort(filteredTodoList, comparator)

        recyclerView.adapter = AdapterToDoPackageList(requireActivity(), this, requireContext(), filteredTodoList, dialogDefaultBinding, dialogGraphBinding, dialogMapBinding)
        val adapter = recyclerView.adapter as AdapterToDoPackageList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        adapter.setManualCertOnClickListener(object: AdapterToDoPackageList.ManualCertOnClickListener{
            override fun onClick(item: TodoPackageDto) {
                updateSuccessTodo(item)
            }
        })

        return adapter
    }

    fun updateSuccessTodo(item: TodoPackageDto){
        var todoList = mutableListOf<TodoPackageDto>()
        var emptyTodoListJson = gson.toJson(todoList)

        var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()
        todoList = gson.fromJson(todoListJson)

        val yearComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.year - b.year } //  년도순 정렬

        val monthComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.month - b.month } //  월순 정렬

        val dayComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.day - b.day } //  날짜순 정렬

        Collections.sort(todoList, dayComparator)
        Collections.sort(todoList, monthComparator)
        Collections.sort(todoList, yearComparator)

        for(todo in todoList){
            if(item.year == todo.year && item.month == todo.month && item.day == todo.day &&
                item.hour == todo.hour && item.minute == todo.minute && item.name == todo.name){
                todo.success = true

                todoListJson = gson.toJson(todoList)

                sharedPrefEditor.putString("myTodoList", todoListJson)
                sharedPrefEditor.commit()

                break
            }
        }

        refreshTodoList()
    }

    fun setDateInfoRecyclerView(recyclerView: RecyclerView): AdapterDateInfoList{
        var dateInfoList = mutableListOf<DateInfoDto>()

        var todoList = mutableListOf<TodoPackageDto>()
        var emptyTodoListJson = gson.toJson(todoList)

        var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()
        todoList = gson.fromJson(todoListJson)

        val yearComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.year - b.year } //  년도순 정렬

        val monthComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.month - b.month } //  월순 정렬

        val dayComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.day - b.day } //  날짜순 정렬

        Collections.sort(todoList, dayComparator)
        Collections.sort(todoList, monthComparator)
        Collections.sort(todoList, yearComparator)

        var lastTodo = TodoPackageDto()

        var todoCount = 0f
        var successTodoCount = 0f // 날짜별 성공횟수를 체크하기 위함
        for(todo in todoList){
            if(!(todo.year == lastTodo.year && todo.month == lastTodo.month && todo.day == lastTodo.day)){ // 날짜가 바뀔 경우
                if(lastTodo.year != 0) // 초기 상태가 아닐 시 날짜 dto 추가
                {
                    var lastDateInfo = DateInfoDto()
                    lastDateInfo.year = lastTodo.year
                    lastDateInfo.month = lastTodo.month
                    lastDateInfo.day = lastTodo.day
                    lastDateInfo.successProgress = successTodoCount / todoCount

                    dateInfoList.add(lastDateInfo)
                    successTodoCount = 0f
                    todoCount = 0f
                }
            }
            todoCount +=1
            if(todo.success == true) successTodoCount +=1
            lastTodo = todo
        }
        // 마지막 날짜 dto 추가
        if(lastTodo.year != 0) // 초기 상태가 아닐 시 날짜 dto 추가
        {
            var lastDateInfo = DateInfoDto()
            lastDateInfo.year = lastTodo.year
            lastDateInfo.month = lastTodo.month
            lastDateInfo.day = lastTodo.day
            lastDateInfo.successProgress = successTodoCount / todoCount

            dateInfoList.add(lastDateInfo)
        }

        recyclerView.adapter = AdapterDateInfoList(requireContext(),dateInfoList)
        val adapter = recyclerView.adapter as AdapterDateInfoList
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        adapter.pickedYear = curYear
        adapter.pickedMonth = curMonth
        adapter.pickedDay = curDay
        adapter.notifyDataSetChanged() // 아이템 업데이트 - setItem을 다시 수행하여 pick된 날짜가 초록색으로 보이게 함

        adapter.setDateInfoOnClickListener(object:AdapterDateInfoList.DateInfoOnClickListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onClick(view: View, position: Int, item: DateInfoDto) {
                curYear = item.year
                curMonth = item.month
                curDay = item.day
                adapter.pickedYear = curYear
                adapter.pickedMonth = curMonth
                adapter.pickedDay = curDay
                adapter.notifyDataSetChanged() // 아이템 업데이트 - setItem을 다시 수행하여 pick된 날짜가 초록색으로 보이게 함

                refreshTodoList()
            }

        })

        return adapter
    }

    fun refreshTodoList(){
        setTodoByDayRecyclerView(fragmentMainBinding.fragmentMainRecyclerView)
        setTodoByTimeRecyclerView(fragmentMainBinding.todoByTimeRecyclerView)
        setDateInfoRecyclerView(fragmentMainBinding.dateInfoRecyclerview)
    }

}