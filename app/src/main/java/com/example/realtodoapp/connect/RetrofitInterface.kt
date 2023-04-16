package com.example.realtodoapp.connect

import com.example.realtodoapp.model.FeedDto
import retrofit2.Call
import retrofit2.http.*

import com.google.gson.JsonElement

interface RetrofitInterface {
    @GET("/getAllFeeds")
    fun getAllFeeds(): Call<JsonElement>

    @POST("/uploadFeed")
    fun uploadFeed(@Body feed: FeedDto): Call<JsonElement>

    @GET("/login")
    fun login(@Query("id") id: String, @Query("pw") pw: String): Call<JsonElement>

    @GET("/signIn")
    fun signIn(@Query("id") id: String, @Query("pw") pw: String, @Query("name") name: String): Call<JsonElement>

//    @POST("/uploadFeed")
//    fun uploadFeed(@Query("uploader") uploader: String, @Body feed: FeedDto): Call<JsonElement>
}

