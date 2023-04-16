package com.example.realtodoapp.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.realtodoapp.R
import com.example.realtodoapp.model.AppRoutineForTimeDto
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.ui.MainActivity
import com.example.realtodoapp.ui.MainFragment
import com.example.realtodoapp.util.AppUtil
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

class CertService: Service(), SensorEventListener {
    lateinit var sensorManager:SensorManager
    lateinit var gravitySensor: Sensor
    var isAlreadyRunning = false

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "notification_channel"
    }

    override fun onCreate() {
        super.onCreate()

        sharedPref = this.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

            // SensorEventListener 등록 (이걸 해야 onSensorChanged에서 인식 가능)
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL)

            setForeGround("", "")

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            "MyApp notification",
            NotificationManager.IMPORTANCE_LOW
        )
//        notificationChannel.enableLights(true)
//        notificationChannel.lightColor = Color.RED
//        notificationChannel.enableVibration(true)
//        notificationChannel.description = "AppApp Tests"

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            notificationChannel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SimpleDateFormat")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {  // service 시작 시 수행됨

        if(isAlreadyRunning == false) { // 서비스 실행 중 한 번만 globalscope 생성
            isAlreadyRunning = true
            val scope = GlobalScope // 비동기 함수 진행
            scope.launch {
                while (true) {
                    delay(1000)

                    // 현재 시간 불러옴
                    var now = System.currentTimeMillis()
                    var date = Date(now)
                    val simpleDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    val simpleYear = SimpleDateFormat("yyyy")
                    val simpleMonth = SimpleDateFormat("MM")
                    val simpleDay = SimpleDateFormat("dd")
                    val simpleHour = SimpleDateFormat("HH") //24시간제 표시
                    val simpleMinute = SimpleDateFormat("mm")
                    val getYear: String = simpleYear.format(date)
                    val getMonth: String = simpleMonth.format(date)
                    val getDay: String = simpleDay.format(date)
                    val getHour: String = simpleHour.format(date)
                    val getMinute: String = simpleMinute.format(date)

                    //setForeGround("날짜", getTime  + "\n" + getYear + getMonth + getDay)

                    // todo목록 불러옴
                    var todoList = mutableListOf<TodoPackageDto>()
                    var emptyTodoListJson = gson.toJson(todoList)

                    var todoListJson =
                        sharedPref.getString("myTodoList", emptyTodoListJson).toString()
                    todoList = gson.fromJson(todoListJson)

                    var filteredTodoList = mutableListOf<TodoPackageDto>()

                    for (todo in todoList) {
                        if (todo.year == Integer.parseInt(getYear) && todo.month == Integer.parseInt(
                                getMonth
                            ) && todo.day == Integer.parseInt(getDay)
                        ) {
                            filteredTodoList.add(todo)
                        }
                    }

                    val comparator: Comparator<TodoPackageDto> =
                        Comparator<TodoPackageDto> { a, b -> a.hour * 60 + a.minute - b.hour * 60 - b.minute } // 시간순 정렬
                    Collections.sort(filteredTodoList, comparator)

                    appRoutineService(getYear, getMonth, getDay)


                    // 현재 인증되어야 하는 todo에 따라 인증 실행
                    for (todo in filteredTodoList) {  // 오늘 날짜의 todo에 한해서 체크
                        if (todo.certType == "SCREEN_AUTO" && todo.success == false) // 화면 인식 방식
                        {
                            var startHour = todo.hour
                            var startMinute = todo.minute
                            var endHour = todo.endHour
                            var endMinute = todo.endMinute

                            var offRatio = recordActionScreen(startHour, startMinute, endHour, endMinute)

                            if(offRatio != -1){ // -1은 인증 체크 시간이 아님을 의미
                                if(offRatio > 70) { // 화면 꺼짐 비율 70% 이상이면 성공
                                    // 성공했으면 todo에 업데이트
                                    updateCertSuccessTodo(todo)

                                    setAlarm("todo 인증 알림" , todo.name + " 성공")
                                }
                                else{
                                    setAlarm("todo 인증 알림" , todo.name + " 실패")
                                }
                            }

                        }
                        else if(todo.certType == "LOCATE_AUTO" && todo.success == false){
                            var startHour = todo.hour
                            var startMinute = todo.minute

                            var successOrFail = recordCurrentLocation(startHour, startMinute)
                            if(successOrFail == true){
                                // 성공했으면 todo에 업데이트
                                updateCertSuccessTodo(todo)
                                setAlarm("todo 인증 알림" , todo.name + " 성공")
                            }
                        }
                    }

                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun appRoutineService(getYear:String, getMonth:String, getDay:String){
        // 오늘 날짜의 appRoutine목록 불러옴
        var appRoutineList = mutableListOf<AppRoutineForTimeDto>()
        var emptyAppRoutineListJson = gson.toJson(appRoutineList)

        var appRoutineListJson =
            sharedPref.getString("appRoutineList"+getYear+getMonth+getDay, emptyAppRoutineListJson).toString()
        appRoutineList = gson.fromJson(appRoutineListJson)

        for(routine in appRoutineList){
            var startHour = routine.startHour
            var startMinute = routine.startMinute
            var endHour = routine.endHour
            var endMinute = routine.endMinute

            var offRatio = recordActionScreen(startHour, startMinute, endHour, endMinute)
        }

    }

    fun setForeGround(title:String, text: String){ // foreground 알림 표시 기능 (항상 켜져있어야 작동)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    fun setAlarm(title:String, text: String){ // 일반 알림 표시 기능
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .build()
        notificationManager.notify(2,notification)
    }

    // 센서 부분
    var x = 0f
    var y = 0f
    var z = 0f

    override fun onSensorChanged(event: SensorEvent?) {

        if(event!!.sensor == gravitySensor){
            x = event.values[0]
            y = event.values[1]
            z = event.values[2]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    fun recordGravity(x: Float, y:Float, z:Float){
        setForeGround(
                        "중력 좌표",
                        "x " + x.toString() + "\n" + "y " + y.toString() + "\n" + "z " + z.toString()
                    )
    }

    fun resetRecordGravity(){

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SimpleDateFormat")
    fun recordActionScreen(startHour:Int, startMinute:Int, endHour:Int, endMinute:Int): Int{ // 기기에 화면 켜짐/꺼짐 기록 저장, 인증 현황 return
        // 현재 시간 불러옴
        var now = System.currentTimeMillis()
        var date = Date(now)
        val simpleYear = SimpleDateFormat("yyyy")
        val simpleMonth = SimpleDateFormat("MM")
        val simpleDay = SimpleDateFormat("dd")
        val simpleHour = SimpleDateFormat("HH") //24시간제 표시
        val simpleMinute = SimpleDateFormat("mm")
        val getYear: String = simpleYear.format(date)
        val getMonth: String = simpleMonth.format(date)
        val getDay: String = simpleDay.format(date)
        val getHour: String = simpleHour.format(date)
        val getMinute: String = simpleMinute.format(date)

        var startTimeString = getYear+"-"+getMonth+"-"+getDay+"-"+String.format("%02d",startHour)+"-"+String.format("%02d",startMinute)
        var endTimeString = getYear+"-"+getMonth+"-"+getDay+"-"+String.format("%02d",endHour)+"-"+String.format("%02d",endMinute) // 두자리수로 맞춰줌
        var currentTimeString = getYear+"-"+getMonth+"-"+getDay+"-"+getHour+"-"+getMinute

        var sharedPrefKey = "interActiveScreenRecord"+ startTimeString+endTimeString

        var appUseTimeSharedPrefKey = "appUseTimeRecord"+ startTimeString+endTimeString

        // 이전 정보 가져옴

        // 사용한 앱 시간 저장하는 리스트
        var appUseTimeRecord = mutableListOf<Pair<String, Int>>() // 앱 패키지명, 실행 시간
        var emptyAppUseTimeRecord = gson.toJson(appUseTimeRecord)

        var appUseTimeRecordJson = sharedPref.getString(appUseTimeSharedPrefKey,emptyAppUseTimeRecord).toString()
        appUseTimeRecord = gson.fromJson(appUseTimeRecordJson)

        // 사용 금지 앱 비율 저장하는 리스트
        var interActiveScreenRecord = mutableListOf<Boolean>()
        var emptyInterActiveScreenRecord = gson.toJson(interActiveScreenRecord)

        var interActiveScreenRecordJson = sharedPref.getString(sharedPrefKey,emptyInterActiveScreenRecord).toString()
        interActiveScreenRecord = gson.fromJson(interActiveScreenRecordJson)

        // 시간 계산하여 해당 시간일 때만 작동
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")

        val startTime: LocalDateTime = LocalDateTime.parse(startTimeString, formatter)
        val endTime: LocalDateTime = LocalDateTime.parse(endTimeString, formatter)
        val currentTime:LocalDateTime = LocalDateTime.parse(currentTimeString, formatter) // 시간 비교 위해 변환


        if(currentTime.isEqual(startTime)|| (currentTime.isAfter(startTime) && currentTime.isBefore(endTime))){ // 인증 시간에만 화면 꺼짐/켜짐 측정 수행

            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isInteractive) {
                // 금지된 앱을 화면에 띄울 때만 인식
                var notUseAppList = AppUtil.loadNotUseAppList(applicationContext, startTimeString+endTimeString)
                var currentApp = AppUtil.getCurrentApp(applicationContext)
                var isfail = false
                Log.d("notUseAppList", notUseAppList.toString())
                Log.d("currentApp", currentApp.packageName)

                // 현재 띄워진 앱이 실행된 시간 1초 추가
                var inRecord = false
                for( i in 0 until appUseTimeRecord.size){
                    if(appUseTimeRecord[i].first == currentApp.packageName && !currentApp.packageName.contains("launcher")){ // 기본 화면 ui 앱 제외
                        appUseTimeRecord[i] = Pair(currentApp.packageName, appUseTimeRecord[i].second +1)
                        Log.d("앱 사용 시간:", appUseTimeRecord[i].second.toString())
                        inRecord = true
                    }
                }
                if(inRecord == false){
                    appUseTimeRecord.add(Pair(currentApp.packageName, 1))
                }

                // 현재 띄워진 앱이 금지된 앱인지 판단
                for (notUseApp in notUseAppList){
                    if(notUseApp.packageName == currentApp.packageName){

                        interActiveScreenRecord.add(true)
                        isfail = true
                        break
                    }
                    if(isfail == true)
                        break
                }
                if(isfail == false){
                    interActiveScreenRecord.add(false)
                }
            } else {
                interActiveScreenRecord.add(false)
            }

            Log.d("레코드:", sharedPrefKey)

            var appUseTimeRecordJson = gson.toJson(appUseTimeRecord)

            sharedPrefEditor.putString(appUseTimeSharedPrefKey, appUseTimeRecordJson) // 시간별로 따로 저장
            sharedPrefEditor.commit()

            var interActiveScreenRecordJson = gson.toJson(interActiveScreenRecord)

            sharedPrefEditor.putString(sharedPrefKey, interActiveScreenRecordJson) // 시간별로 따로 저장
            sharedPrefEditor.commit()
        }
        else if(currentTime.isEqual(endTime)){ // 인증 시간 지나면 성공 여부 체크
            var count = 0
            var offCount = 0

            for (record in interActiveScreenRecord){
                if(record == false){
                    offCount +=1
                }
                count +=1
            }

            var offRatio = ((offCount.toFloat() / count.toFloat()) *100).toInt()

            return offRatio
        }

        return -1 // 인증 완료 전에는 -1 return
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun recordCurrentLocation(startHour:Int, startMinute:Int): Boolean{
        // 현재 시간 불러옴
        var now = System.currentTimeMillis()
        var date = Date(now)
        val simpleYear = SimpleDateFormat("yyyy")
        val simpleMonth = SimpleDateFormat("MM")
        val simpleDay = SimpleDateFormat("dd")
        val simpleHour = SimpleDateFormat("HH") //24시간제 표시
        val simpleMinute = SimpleDateFormat("mm")
        val getYear: String = simpleYear.format(date)
        val getMonth: String = simpleMonth.format(date)
        val getDay: String = simpleDay.format(date)
        val getHour: String = simpleHour.format(date)
        val getMinute: String = simpleMinute.format(date)

        var startTimeString = getYear+"-"+getMonth+"-"+getDay+"-"+String.format("%02d",startHour)+"-"+String.format("%02d",startMinute)
        var currentTimeString = getYear+"-"+getMonth+"-"+getDay+"-"+getHour+"-"+getMinute

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")

        val startTime: LocalDateTime = LocalDateTime.parse(startTimeString, formatter)
        val currentTime:LocalDateTime = LocalDateTime.parse(currentTimeString, formatter) // 시간 비교 위해 변환

        if(currentTime.isEqual(startTime) || currentTime.isAfter(startTime)){ // 인증 시작 시간부터 발동
            // 현재 위치값을 알아내어 기기에 저장
            var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationGPSProvider = LocationManager.GPS_PROVIDER
            val locationNetworkProvider = LocationManager.NETWORK_PROVIDER
            var userLocation = Location(locationGPSProvider)

            var hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            var hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)

            // 위치 권한 허용된 경우에만 실행
            if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

                var mLocationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                    }
                    override fun onProviderDisabled(provider: String) {
                    }

                    override fun onProviderEnabled(provider: String) {
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    }
                }

                locationManager.removeUpdates(mLocationListener) // 중복 방지 위해 끊고 시작

                var handler = Handler(Looper.getMainLooper())
                handler.postDelayed(Runnable {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, mLocationListener)
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0f, mLocationListener)
                }, 0)


                if(locationManager.getLastKnownLocation(locationGPSProvider) != null){
                    userLocation = locationManager.getLastKnownLocation(locationGPSProvider)!!
                }
                else if(locationManager.getLastKnownLocation(locationNetworkProvider) != null){
                    userLocation = locationManager.getLastKnownLocation(locationNetworkProvider)!!
                }
                else{
                    setAlarm("위치", "측정불가")
                    return false
                }

                var latitude = userLocation.latitude
                var longitude = userLocation.longitude

                // 현재 위치값 저장 (todo마다 따로 저장)
                var sharedPrefKey = "currentLocateRecord"+
                        getYear+getMonth+getDay+startHour.toString()+startMinute.toString()

                var currentLocateRecord = mutableListOf<Double>()
                currentLocateRecord.add(latitude)
                currentLocateRecord.add(longitude)

                var currentLocateRecordJson = gson.toJson(currentLocateRecord)

                sharedPrefEditor.putString(sharedPrefKey, currentLocateRecordJson) // 시간별로 따로 저장
                sharedPrefEditor.commit()

                //setAlarm("위치", latitude.toString() + " " + longitude.toString())

                // 목표 위치에 도달했는지 확인
                var sharedPrefKeyGoal = "goalLocateRecord"+
                        getYear+getMonth+getDay+startHour.toString()+startMinute.toString()
                var goalLocateRecord = mutableListOf<Double>()
                var emptyGoalLocateRecord = gson.toJson(goalLocateRecord)

                var goalLocateRecordJson = sharedPref.getString(sharedPrefKeyGoal,emptyGoalLocateRecord).toString()
                goalLocateRecord = gson.fromJson(goalLocateRecordJson)

                if(abs(latitude - goalLocateRecord[0]) < 0.001 && abs(longitude - goalLocateRecord[1]) < 0.001){
                    //setAlarm("가까움", abs(latitude - goalLocateRecord[0]).toString() + " " + abs(longitude - goalLocateRecord[1]).toString())
                    return true
                }
            }

        }

        return false
    }

    fun updateCertSuccessTodo(item: TodoPackageDto){ // 인증 성공 업데이트
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
    }

}