package com.example.practicekotlin13

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class MatchedUserAdapter: ListAdapter<CardItem, MatchedUserAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {

        fun bind(cardItem: CardItem) {
            view.findViewById<TextView>(R.id.userNameTextView).text = cardItem.name // 해당되는 뷰의 텍스트 뷰에 바인딩 되는 cardItem의 텍스트 대입
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder { // binding을 따로 설정하지 않았기 때문에 해당 메서드에서 inflater 생성 후 레이아웃 생성
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_matched_user, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<CardItem>() {
            override fun areItemsTheSame(oldItem: CardItem, newItem: CardItem): Boolean {
                return oldItem.userId == newItem.userId
            }

            override fun areContentsTheSame(oldItem: CardItem, newItem: CardItem): Boolean {
                return oldItem == newItem
            }

        }
    }
}