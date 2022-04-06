package com.example.practicekotlin13

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicekotlin13.StringKey.Companion.LIKED_BY
import com.example.practicekotlin13.StringKey.Companion.MATCH
import com.example.practicekotlin13.StringKey.Companion.NAME
import com.example.practicekotlin13.StringKey.Companion.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MatchedUserActivity: AppCompatActivity() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference
    private val adapter = MatchedUserAdapter()
    private val cardItems = mutableListOf<CardItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        userDB = Firebase.database.reference.child(USERS)


        initMatchedUserRecyclerView()
        getMatchUsers()
    }

    private fun initMatchedUserRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.matchedUserRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun getMatchUsers() {
        val matchedDB = userDB.child(getCurrentUserId()).child(LIKED_BY).child(MATCH)

        matchedDB.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.key?.isNotEmpty() == true) { // match 항목의 키가 null 이 아니라면, 키 값이 존재한다면
                    getUserByKey(snapshot.key.orEmpty()) // 키 값을 통해 db 재조회
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}

        })
    }

    private fun getUserByKey(userId: String) {
        userDB.child(userId).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                cardItems.add(CardItem(userId, snapshot.child(NAME).value.toString())) // 키 값을 통해 타 사용자의 이름값 받음
                adapter.submitList(cardItems)
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun getCurrentUserId(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish() // 로그인이 되어있지 않다면 MainActivity로 이동(복귀)
        }

        return auth.currentUser?.uid.orEmpty() // 예외 처리
    }
}