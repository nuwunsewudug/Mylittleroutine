package com.example.realtodoapp.util

import android.app.Activity
import com.example.realtodoapp.ui.MainActivity
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import java.nio.ByteBuffer

class TfliteModelUtil {
    companion object {
        fun loadSentenceModel(activity: Activity):Interpreter {
            return Interpreter(loadSentenceModelFile(activity))
        }

        fun loadInterestModel(activity: Activity):Interpreter {
            return Interpreter(loadInterestModelFile(activity))
        }

        private fun loadSentenceModelFile(activity: Activity): ByteBuffer {
            val assetFileDescriptor: AssetFileDescriptor = activity.getAssets().openFd("sentence_model.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val len = assetFileDescriptor.length
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, len)
        }

        private fun loadInterestModelFile(activity: Activity): ByteBuffer {
            val assetFileDescriptor: AssetFileDescriptor = activity.getAssets().openFd("interest_model.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val len = assetFileDescriptor.length
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, len)
        }
    }
}

