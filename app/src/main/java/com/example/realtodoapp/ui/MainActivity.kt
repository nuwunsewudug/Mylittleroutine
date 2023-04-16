package com.example.realtodoapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.realtodoapp.R
import com.example.realtodoapp.service.CertService
import com.example.realtodoapp.util.AppUtil
import com.example.realtodoapp.connect.RetrofitInterface
import com.example.realtodoapp.model.MemberInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MainActivity : AppCompatActivity(){
    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // CertService Foreground 실행
        val intent = Intent(this, CertService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent)
        } else {
            this.startService(intent)
        }
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)
        requirePermissions(permissions, 999)

        // 테스트
        AppUtil.checkForPermission(this)

        //komoran 테스트
//        var komoran = Komoran(DEFAULT_MODEL.FULL)
//        var sample = "좋은 아침입니다"
//        var komoranResult = komoran.analyze(sample)
//        var tokenList = komoranResult.tokenList
//        var morphList = mutableListOf<String>()
//        for(token in tokenList){
//            morphList.add(token.morph)
//        }
//        Log.d("komoran", morphList.toString())


        // Ai 테스트
//        var tfModel = TfliteModelUtil.loadTfModel(this)
//        var input = Array(1){"당신의 긍정적인 자세에 매우 감사하게 생각해요"}
//        var output: ByteBuffer = ByteBuffer.allocate(2*4).order(ByteOrder.nativeOrder())
//        tfModel.run(input, output)
//
//        // bytebuffer float 변환
//        output.rewind()
//        var pro = output.asFloatBuffer()
//        Log.d("AITEST", pro.get(0).toString() + " "+ pro.get(1).toString())

        // retrofit 테스트
//        val url = "http://10.0.2.2:3000"
//        val retrofit = Retrofit.Builder()
//            .baseUrl(url)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//        var server = retrofit.create(RetrofitInterface::class.java) // 만들어둔 interface와 연결
//
//        server.getAllFeeds().enqueue(object: Callback<JsonElement>{
//            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
//                Log.d("getAllFeedsFail : ", "Fail")
//            }
//            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
//                Log.d("getAllFeeds : ", response.body().toString())
//                var list = mutableListOf<MemberInfoDto>()
//                list = gson.fromJson(response.body().toString())
//                Log.d("getAllFeedsElement : ", list[0].mem_id)
//            }
//        })

    }

    // 위치 권한 부여
    fun requirePermissions(permissions: Array<String>, requestCode: Int) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        } else {
            val isAllPermissionsGranted = permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
            if (isAllPermissionsGranted) {
            } else {
                ActivityCompat.requestPermissions(this, permissions, requestCode)
            }
        }
    }


}


