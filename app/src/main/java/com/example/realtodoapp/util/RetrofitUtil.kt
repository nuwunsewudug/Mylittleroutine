package com.example.realtodoapp.util

import android.content.SharedPreferences
import android.util.Log
import com.example.realtodoapp.connect.RetrofitInterface
import com.example.realtodoapp.model.FeedDto
import com.example.realtodoapp.model.MemberInfoDto
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitUtil {

    companion object {
        inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
        var gson: Gson = Gson()

        val url = "http://10.0.2.2:3000"
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        var server = retrofit.create(RetrofitInterface::class.java) // 만들어둔 interface와 연결

        fun getAllFeeds(successCallback: (List<FeedDto>) -> Unit,
                        failCallback: (Throwable) -> Unit){
            return server.getAllFeeds().enqueue(object: Callback<JsonElement>{
                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    Log.d("getAllFeedsFail : ", "Fail")
                    failCallback(t)
                }
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    var list:List<FeedDto> = gson.fromJson(response.body().toString())
                    Log.d("getAllFeedsSuccess: ", list.toString())
                    successCallback(list)
                }
            })
        }

        fun uploadFeed(feed:FeedDto){
            server.uploadFeed(feed).enqueue(object: Callback<JsonElement>{
                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    Log.d("uploadFeed : ", "Fail")
                }
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    Log.d("uploadFeed : ", "Success")
                }
            })
        }

        fun login(id:String, pw:String,
                  successCallback: (MemberInfoDto) -> Unit,
                  failCallback: (Throwable) -> Unit){
            server.login(id, pw).enqueue(object: Callback<JsonElement>{
                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    Log.d("loginAccessFail : ", "Fail")
                    failCallback(t)
                }
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    var list:List<MemberInfoDto> = gson.fromJson(response.body().toString())
                    if(list.size == 1) {
                        successCallback(list[0])
                    }
                }
            })
        }

        fun signIn(id:String, pw:String, name:String,
                   successCallback: () -> Unit,
                   failCallback: (Throwable) -> Unit){
            server.signIn(id, pw, name).enqueue(object: Callback<JsonElement>{
                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    Log.d("signInAccessFail : ", "Fail")
                    failCallback(t)
                }
                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    successCallback()
                }
            })
        }
    }
}
