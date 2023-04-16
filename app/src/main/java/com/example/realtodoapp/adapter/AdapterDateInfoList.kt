package com.example.realtodoapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.databinding.ItemDateInfoBinding
import com.example.realtodoapp.databinding.ItemTodoPackageBinding
import com.example.realtodoapp.model.DateInfoDto
import java.time.Year

class AdapterDateInfoList(val context: Context, var list: List<DateInfoDto>) : RecyclerView.Adapter<DateInfoHolder>(){
    var pickedYear = 0
    var pickedMonth = 0
    var pickedDay = 0

    var items = list
        @SuppressLint("NotifyDataSetChanged")
        set(value){
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateInfoHolder {
        var bind = ItemDateInfoBinding.inflate(LayoutInflater.from(context), parent, false)
        return DateInfoHolder(context, bind)
    }

    override fun onBindViewHolder(holder: DateInfoHolder, position: Int) {
        var item = items.get(position)
        holder.bind.backgroundConstraintLayout.setOnClickListener(){
            dateInfoOnClickListener.onClick(it, position, item)
        }
        holder.setItem(item, pickedYear, pickedMonth, pickedDay)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface DateInfoOnClickListener{
        fun onClick(view: View, position: Int, item:DateInfoDto)
    }

    private lateinit var dateInfoOnClickListener: DateInfoOnClickListener

    fun setDateInfoOnClickListener(dateInfoOnClickListener: DateInfoOnClickListener){
        this.dateInfoOnClickListener = dateInfoOnClickListener
    }
}

class DateInfoHolder(val context: Context, var bind: ItemDateInfoBinding):RecyclerView.ViewHolder(bind.root){
    @SuppressLint("SetTextI18n")
    fun setItem(item:DateInfoDto, pickedYear: Int, pickedMonth: Int, pickedDay: Int){
        bind.yearTextView.setText(item.year.toString())
        bind.monthDayTextView.setText(item.month.toString() + "/"  +item.day.toString())

        if(item.year == pickedYear && item.month == pickedMonth && item.day == pickedDay){
            bind.backgroundConstraintLayout.setBackgroundResource(R.drawable.green_round_layout)
        }
        else{
            bind.backgroundConstraintLayout.setBackgroundResource(R.drawable.round_layout)
        }

        bind.successProgressView.progress = item.successProgress * 100
    }
}