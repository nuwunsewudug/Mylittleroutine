package com.example.realtodoapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.databinding.DialogAppListBinding
import com.example.realtodoapp.databinding.ItemAppInfoBinding
import com.example.realtodoapp.model.DateInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.util.AppUtil
import kotlinx.coroutines.selects.select

class AdapterAppInfoList(val context: Context, var list: List<ApplicationInfo>, var timeInfo:String) : RecyclerView.Adapter<AppInfoListHolder>() {
    var items = list
        @SuppressLint("NotifyDataSetChanged")
        set(value){
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppInfoListHolder {
        var bind = ItemAppInfoBinding.inflate(LayoutInflater.from(context), parent, false)
        return AppInfoListHolder(context, bind, timeInfo)
    }

    override fun onBindViewHolder(holder: AppInfoListHolder, position: Int) {
        var item = items.get(position)
        holder.setItem(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class AppInfoListHolder(val context: Context, var bind:ItemAppInfoBinding, var timeInfo: String): RecyclerView.ViewHolder(bind.root) {
    fun setItem(item:ApplicationInfo){
        bind.appIconImageView.setImageDrawable(item.loadIcon(context.packageManager))
        bind.appLabelTextView.setText(item.loadLabel(context.packageManager))


        var notUseAppList = AppUtil.loadNotUseAppList(context, timeInfo)

        if(AppUtil.isNotUseApp(item, notUseAppList)){
            bind.checkBox.setChecked(true)
        }
        else{
            bind.checkBox.setChecked(false)
        }

        bind.checkBox.setOnCheckedChangeListener{ _, isChecked ->
            if(isChecked){
                AppUtil.addNotUseApp(context, item, timeInfo)
            }
            else{
                AppUtil.deleteNotUseApp(context, item, timeInfo)
            }

        }
    }
}