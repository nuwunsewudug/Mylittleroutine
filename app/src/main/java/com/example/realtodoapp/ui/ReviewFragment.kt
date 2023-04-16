package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.realtodoapp.R
import com.example.realtodoapp.databinding.FragmentReviewBinding
import com.example.realtodoapp.model.FeedDto
import com.example.realtodoapp.model.MemberInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.util.AIModelUtil
import com.example.realtodoapp.util.RetrofitUtil
import com.example.realtodoapp.util.TfliteModelUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL
import kr.co.shineware.nlp.komoran.core.Komoran
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ReviewFragment: Fragment() {
    lateinit var fragmentReviewBinding: FragmentReviewBinding
    var lastResult = 50f
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = requireContext().getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()
    }

    @SuppressLint("CheckResult", "CommitPrefEdits", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentReviewBinding = FragmentReviewBinding.inflate(layoutInflater)
        val view = fragmentReviewBinding.root

        val curYear = arguments?.getString("curYear")
        val curMonth = arguments?.getString("curMonth")
        val curDay = arguments?.getString("curDay")

        // todo를 리뷰할 경우 활성화
        val todoName = arguments?.getString("todoName")
        val todoSuccess = arguments?.getBoolean("todoSuccess")

        if(todoName != null){
            if(todoSuccess == true){
                fragmentReviewBinding.title.setText(todoName +"(성공)"+" 리뷰")
            }
            else{
                fragmentReviewBinding.title.setText(todoName +"(실패)"+" 리뷰")
            }
            fragmentReviewBinding.reviewEditText.setText(loadReview(curYear!!, curMonth!!, curDay!!, todoName))
        }
        else{
            fragmentReviewBinding.title.setText(curYear + "-" + curMonth + "-" + curDay + " 리뷰", )
            fragmentReviewBinding.reviewEditText.setText(loadReview(curYear!!, curMonth!!, curDay!!, "todayReview"))
        }


        fragmentReviewBinding.saveButton.setOnClickListener(){
            var review = fragmentReviewBinding.reviewEditText.text.toString()
            if(todoName != null) {
                saveReview(curYear!!, curMonth!!, curDay!!, todoName, review)
            }
            else{
                saveReview(curYear!!, curMonth!!, curDay!!, "todayReview", review)
            }
        }

        fragmentReviewBinding.submitButton.setOnClickListener(){
            runSentenceAI()
            runInterestAI()
        }

        fragmentReviewBinding.shareButton.setOnClickListener(){
            var review = fragmentReviewBinding.reviewEditText.text.toString()
            var feed = FeedDto()

            // 저장된 id 정보 불러오기
            var loginMemberInfo = MemberInfoDto()
            var emptyLoginMemberInfo = gson.toJson(loginMemberInfo)
            var loginMemberInfoJson = sharedPref.getString("loginMemberInfo",emptyLoginMemberInfo).toString()
            loginMemberInfo = gson.fromJson(loginMemberInfoJson)

            feed.mem_id = loginMemberInfo.mem_id

            feed.feed_title = fragmentReviewBinding.title.text.toString()
            feed.feed_text = review

            RetrofitUtil.uploadFeed(feed)
        }

        return view
    }

    @SuppressLint("SetTextI18n")
    fun runInterestAI(){
        var input = fragmentReviewBinding.reviewEditText.text.toString()
        var output = AIModelUtil.getInterestModelResult(input, requireActivity())
        fragmentReviewBinding.firstInterestTextView.setText("운동 : "+ (output.get(0)* 100).toInt().toString())
        fragmentReviewBinding.secondInterestTextView.setText("독서 : "+ (output.get(1)* 100).toInt().toString())
        fragmentReviewBinding.thirdInterestTextView.setText("여행 : "+ (output.get(2)* 100).toInt().toString())
        fragmentReviewBinding.forthInterestTextView.setText("요리 : "+ (output.get(3)* 100).toInt().toString())

        // 관심사에 따라 이미지 변경
        fragmentReviewBinding.interestImageView.setImageResource(R.drawable.question)
        if(output.get(0) * 100 > 50){
            fragmentReviewBinding.interestImageView.setImageResource(R.drawable.exercise)
        }
        else if(output.get(1) * 100 > 50){
            fragmentReviewBinding.interestImageView.setImageResource(R.drawable.read)
        }
        else if(output.get(2) * 100 > 50){
            fragmentReviewBinding.interestImageView.setImageResource(R.drawable.travel)
        }
        else if(output.get(3) * 100 > 50){
            fragmentReviewBinding.interestImageView.setImageResource(R.drawable.cook)
        }

        // 세부 글자 색상 변경 과정
        fragmentReviewBinding.reviewEditText.text.clear()

        // . 기준으로 나눔
        var token = input.split(".", "\n")
        for (element in token){
            if(element.length >= 2) {
                val builder = SpannableStringBuilder(element)
                if (AIModelUtil.getInterestModelResult(element, requireActivity()).get(0) * 100 > 50) {
                    builder.setSpan(
                        ForegroundColorSpan(Color.BLUE), 0, element.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else if(AIModelUtil.getInterestModelResult(element, requireActivity()).get(1) * 100 > 50){
                    builder.setSpan(
                        ForegroundColorSpan(Color.RED), 0, element.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                else if(AIModelUtil.getInterestModelResult(element, requireActivity()).get(2) * 100 > 50){
                    builder.setSpan(
                        ForegroundColorSpan(Color.GREEN), 0, element.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                else if(AIModelUtil.getInterestModelResult(element, requireActivity()).get(3) * 100 > 50){
                    builder.setSpan(
                        ForegroundColorSpan(Color.MAGENTA), 0, element.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                else{
                    builder.setSpan(
                        ForegroundColorSpan(Color.BLACK), 0, element.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                fragmentReviewBinding.reviewEditText.text.append(builder)
                fragmentReviewBinding.reviewEditText.text.append(".")
            }
        }
    }

    fun runSentenceAI(){
        var input = fragmentReviewBinding.reviewEditText.text.toString()
        var totalResult = AIModelUtil.getSentenceModelResult(input, requireActivity()) * 100
        fragmentReviewBinding.resultProgressView.progress = totalResult

        if(totalResult > lastResult){
            upArrowAnimation()
        }
        else if(totalResult < lastResult){
            downArrowAnimation()
        }
        lastResult = totalResult

    }

    fun loadReview(year:String, month:String, day:String, detail:String):String{
        return sharedPref.getString("review-"+year+month+day+detail ,"").toString()
    }

    fun saveReview(year:String, month:String, day:String, detail:String, review:String){
        sharedPrefEditor.putString("review-"+year+month+day+detail, review) // 시간별로 따로 저장
        sharedPrefEditor.commit()
    }

    fun shareReview(year:String, month:String, day:String, review:String){

    }

    fun upArrowAnimation(){
        val scope = GlobalScope // 비동기 함수 진행
        scope.launch {
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_1)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_2)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_3)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_4)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(0)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_5)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_6)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_7)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_8)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(0)
            }
        }
    }

    fun downArrowAnimation(){
        val scope = GlobalScope // 비동기 함수 진행
        scope.launch {
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_1)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_2)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_3)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_4)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(0)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_5)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_6)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_7)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_8)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(0)
            }
        }
    }
}