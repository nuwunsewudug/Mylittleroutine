package com.example.realtodoapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.databinding.DialogInterestProgressBinding
import com.example.realtodoapp.databinding.ItemAppInfoBinding
import com.example.realtodoapp.databinding.ItemFeedBinding
import com.example.realtodoapp.model.FeedDto
import com.example.realtodoapp.util.AIModelUtil
import com.example.realtodoapp.util.AppUtil

class AdapterFeedList(val context: Context, val activity: Activity, var list: List<FeedDto>, var dialogInterestProgressBinding: DialogInterestProgressBinding): RecyclerView.Adapter<FeedListHolder>() {
    var items = list
        @SuppressLint("NotifyDataSetChanged")
        set(value){
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedListHolder {
        var bind = ItemFeedBinding.inflate(LayoutInflater.from(context), parent, false)
        return FeedListHolder(context, activity, bind, dialogInterestProgressBinding)
    }

    override fun onBindViewHolder(holder: FeedListHolder, position: Int) {
        var item = items.get(position)
        holder.setItem(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class FeedListHolder(val context: Context, val activity:Activity, var bind: ItemFeedBinding, var dialogInterestProgressBinding: DialogInterestProgressBinding): RecyclerView.ViewHolder(bind.root) {
    fun setItem(item:FeedDto){
        var interestResult = AIModelUtil.getInterestModelResult(item.feed_text, activity)
        var emotionResult = AIModelUtil.getSentenceModelResult(item.feed_text, activity)

        var maxInterestIndex = -1
        var maxInterestPercent = 0f

        for(index in 0 until 4){
            if(maxInterestPercent < interestResult.get(index)*100){
                maxInterestPercent = interestResult.get(index)*100
                maxInterestIndex = index
            }
        }

        if(maxInterestPercent >= 40){
            if(maxInterestIndex == 0){
                bind.feedInterestButton.setText("운동")
                bind.feedInterestButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#30EEFF")))
            }
            else if(maxInterestIndex == 1){
                bind.feedInterestButton.setText("독서")
                bind.feedInterestButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFCCCC")))
            }
            else if(maxInterestIndex == 2){
                bind.feedInterestButton.setText("여행")
                bind.feedInterestButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DDFFDD")))
            }
            else if(maxInterestIndex == 3){
                bind.feedInterestButton.setText("요리")
                bind.feedInterestButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0D8F3")))
            }
        }
        else{
            bind.feedInterestButton.setText("")
            bind.feedInterestButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")))
        }

        if(emotionResult >= 0.66f){
            bind.feedEmotion.setImageResource(R.drawable.good_emotion)
        }
        else if(emotionResult >= 0.33f){
            bind.feedEmotion.setImageResource(R.drawable.normal_emotion)
        }
        else{
            bind.feedEmotion.setImageResource(R.drawable.bad_emotion)
        }

        bind.feedWriter.setText(item.mem_id)
        bind.feedTime.setText(item.feed_time.toString())
        bind.feedTitle.setText(item.feed_title)
        bind.feedContents.setText(item.feed_text)

        bind.feedInterestButton.setOnClickListener(){
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if (dialogInterestProgressBinding.root.parent != null) {
                (dialogInterestProgressBinding.root.parent as ViewGroup).removeView(
                    dialogInterestProgressBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                dialog.dismiss()
            }
            dialog.setContentView(dialogInterestProgressBinding.root)
            var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
            params.width = (context.getResources()
                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
            params.height = (context.getResources()
                .getDisplayMetrics().heightPixels * 0.7).toInt() // device의 세로 길이에 비례하여  결정
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.getWindow()!!.setAttributes(params)
            dialog.getWindow()!!.setGravity(Gravity.CENTER)
            dialog.setCancelable(true)
            dialog.show()

            dialogInterestProgressBinding.oneProgressView.progress = interestResult.get(0)*100
            dialogInterestProgressBinding.twoProgressView.progress = interestResult.get(1)*100
            dialogInterestProgressBinding.threeProgressView.progress = interestResult.get(2)*100
            dialogInterestProgressBinding.fourProgressView.progress = interestResult.get(3)*100

            dialogInterestProgressBinding.okButton.setOnClickListener(){
                if (dialogInterestProgressBinding.root.parent != null) {
                    (dialogInterestProgressBinding.root.parent as ViewGroup).removeView(
                        dialogInterestProgressBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    dialog.dismiss()
                }
            }
        }

    }
}