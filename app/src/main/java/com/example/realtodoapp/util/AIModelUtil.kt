package com.example.realtodoapp.util

import android.app.Activity
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class AIModelUtil {
    companion object {
        fun getInterestModelResult(input: String, activity: Activity): FloatBuffer {

            var tfModel = TfliteModelUtil.loadInterestModel(activity)
            var inputArray = Array(1){input+" "}
            var output: ByteBuffer = ByteBuffer.allocate(4*4).order(ByteOrder.nativeOrder())
            tfModel.run(inputArray, output)

            // bytebuffer float 변환
            output.rewind()
            var pro = output.asFloatBuffer()
            return pro
        }

        fun getSentenceModelResult(input:String, activity: Activity): Float{

            var tfModel = TfliteModelUtil.loadSentenceModel(activity)
            var inputArray = Array(1){input+" "}
            var output: ByteBuffer = ByteBuffer.allocate(2*4).order(ByteOrder.nativeOrder())
            tfModel.run(inputArray, output)

            // bytebuffer float 변환
            output.rewind()
            var pro = output.asFloatBuffer()
            Log.d("AITEST", pro.get(0).toString() + " "+ pro.get(1).toString())
            return pro.get(1)
        }
    }
}