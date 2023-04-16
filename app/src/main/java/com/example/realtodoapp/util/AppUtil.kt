package com.example.realtodoapp.util

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process.myUid
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import com.example.realtodoapp.model.TodoPackageDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AppUtil {
    companion object {
        inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
        var gson: Gson = Gson()
        lateinit var sharedPref: SharedPreferences
        lateinit var sharedPrefEditor : SharedPreferences.Editor

        // 설치된 앱 불러오기
        fun getInstalledApp(context: Context): List<ApplicationInfo>{
            val packageManager = context.packageManager
            val applications: List<ApplicationInfo> = packageManager.getInstalledApplications(0)

            val notSystemApps = mutableListOf<ApplicationInfo>()

            for (app in applications){
                if((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0){
                    notSystemApps.add(app)
                }
                else if((app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0){
                    notSystemApps.add(app)
                }
                else if(app.packageName == "com.google.android.youtube"){
                    notSystemApps.add(app)
                }
                else if(app.packageName == "com.android.chrome"){
                    notSystemApps.add(app)
                }
            }

            return notSystemApps
        }

        // 화면에 띄워지는 앱 불러오기
        fun getCurrentApp(context: Context) : UsageEvents.Event{
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -1) // 현재 lastEvent가 시간 차이만큼만 정상적으로 return됨

            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val usageEvent = usageStatsManager.queryEvents(cal.timeInMillis, System.currentTimeMillis())

            val usageEvents = mutableListOf<UsageEvents.Event>()

            var lastRunTimeStamp = 0
            var lastEvent = UsageEvents.Event()

            // 가장 최근에 resumed된 앱 불러오기
            while(usageEvent.hasNextEvent()){
                val event = UsageEvents.Event()
                usageEvent.getNextEvent(event)

                if(event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED){
                    usageEvents.add(event)
                    if(event.timeStamp > lastRunTimeStamp){
                        lastRunTimeStamp = event.timeStamp.toInt()
                        lastEvent = event
                    }
                }
            }

            return lastEvent
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        fun checkForPermission(context:Context){
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
            if(mode != MODE_ALLOWED){
                startActivity(context, Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS), null)
            }
        }

        // 사용 금지한 앱 불러오기
        fun loadNotUseAppList(context: Context, timeInfo:String): MutableList<ApplicationInfo>{
            sharedPref = context.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)

            var notUseAppList = mutableListOf<ApplicationInfo>()
            var emptyNotUseAppListJson = gson.toJson(notUseAppList)

            var notUseAppListJson = sharedPref.getString("notUseAppList"+timeInfo,emptyNotUseAppListJson).toString()
            notUseAppList = gson.fromJson(notUseAppListJson)
            return notUseAppList
        }

        // 사용 금지 앱 추가
        fun addNotUseApp(context: Context, applicationInfo: ApplicationInfo, timeInfo: String){
            sharedPref = context.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
            sharedPrefEditor = sharedPref.edit()

            var notUseAppList = loadNotUseAppList(context, timeInfo)
            if(!isNotUseApp(applicationInfo, notUseAppList)){
                notUseAppList.add(applicationInfo)
            }

            var notUseAppListJson = gson.toJson(notUseAppList)

            sharedPrefEditor.putString("notUseAppList"+timeInfo, notUseAppListJson)
            sharedPrefEditor.commit()
        }

        // 사용 금지 앱 목록에서 제거
        fun deleteNotUseApp(context: Context, applicationInfo: ApplicationInfo, timeInfo: String){
            sharedPref = context.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
            sharedPrefEditor = sharedPref.edit()

            var notUseAppList = loadNotUseAppList(context, timeInfo)
            if(isNotUseApp(applicationInfo, notUseAppList)){
                // 같은 앱이라도 package name 제외 다를 수 있기 때문에 package name으로 구분하여 제거
                //  remove 오류 방지 위해 for 문 쓰지 않고 iter 사용
                var iter = notUseAppList.iterator()
                while(iter.hasNext()){
                    var app = iter.next()
                    if(applicationInfo.packageName == app.packageName){
                        iter.remove()
                    }
                }
            }

            var notUseAppListJson = gson.toJson(notUseAppList)

            sharedPrefEditor.putString("notUseAppList"+timeInfo, notUseAppListJson)
            sharedPrefEditor.commit()
        }

        // 사용 금지 앱인지 판단
        fun isNotUseApp(applicationInfo: ApplicationInfo, notUseAppList:MutableList<ApplicationInfo>):Boolean{
            for(app in notUseAppList){
                if(applicationInfo.packageName == app.packageName ){
                    return true
                }
            }
            return false
        }

    }
}