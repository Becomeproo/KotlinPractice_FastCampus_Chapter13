package com.example.practicekotlin13

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.practicekotlin13.StringKey.Companion.DISLIKE
import com.example.practicekotlin13.StringKey.Companion.LIKE
import com.example.practicekotlin13.StringKey.Companion.LIKED_BY
import com.example.practicekotlin13.StringKey.Companion.MATCH
import com.example.practicekotlin13.StringKey.Companion.NAME
import com.example.practicekotlin13.StringKey.Companion.USERS
import com.example.practicekotlin13.StringKey.Companion.USER_ID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity : AppCompatActivity(), CardStackListener {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference

    private val adapter = CardItemAdapter()
    private val cardItems = mutableListOf<CardItem>()

    private val manager by lazy {
        CardStackLayoutManager(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child(USERS)

        val currentUserDB = userDB.child(getCurrentUserId())
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { // 데이터가 처음 들어오거나 수정 되었을 때 해당 메서드로 값이 들어옴
                if (snapshot.child(NAME).value == null) {
                    showNameInputPopup()
                    return
                }

                getUnselectedUsers()

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        initCardStackView()
        initSignOutButton()
        initMatchedListButton()
    }

    private fun initCardStackView() {
        val stackView = findViewById<CardStackView>(R.id.cardStackView)

        stackView.layoutManager = manager
        stackView.adapter = adapter
    }

    private fun initSignOutButton() {
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun initMatchedListButton() {
        val matchedListButton = findViewById<Button>(R.id.matchListButton)
        matchedListButton.setOnClickListener {
            startActivity(Intent(this, MatchedUserActivity::class.java))
        }
    }


    fun getUnselectedUsers() { // 타 유저정보 불러오기
        userDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child(USER_ID).value != getCurrentUserId() // userId가 사용자와 같지 않거나
                    && snapshot.child(LIKED_BY).child(LIKE).hasChild(getCurrentUserId()).not() // 사용자의 like 또는 dislike 이벤트가 다른 유저의 정보에 저장되지 않았거나
                    && snapshot.child(LIKED_BY).child(DISLIKE).hasChild(getCurrentUserId()).not()
                ) { // -> 사용자가 한번도 본적 없는 유저이다.

                    val userId = snapshot.child("userId").value.toString()
                    var name = "undecided"
                    if (snapshot.child("name").value != null) {
                        name = snapshot.child("name").value.toString()
                    }

                    cardItems.add(CardItem(userId, name)) // 위의 생성된 정보를 CardItem에 저장
                    adapter.submitList(cardItems) // cardItem 갱신
                    adapter.notifyDataSetChanged() // recyclerView를 갱신
                }
            }

            override fun onChildChanged(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) { // 상대방의 데이터가 변경되었을 때

                // 이름이 바뀌었을 경우
                cardItems.find { it.userId == snapshot.key }?.let {
                    it.name = snapshot.child(NAME).value.toString()
                }

                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun showNameInputPopup() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("이름을 입력해주세요.")
            .setView(editText)
            .setPositiveButton("저장") { _, _ ->
                if (editText.text.isEmpty()) {
                    showNameInputPopup()
                } else {
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false) // 바깥쪽을 누르는 행위로 인한 팝업 닫기 비활성화
            .show()
    }

    private fun saveUserName(name: String) {
        val userId = getCurrentUserId()
        val currentUserDB = userDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user[USER_ID] = userId
        user[NAME] = name
        currentUserDB.updateChildren(user) // DB내에 user 정보 저장

        getUnselectedUsers()
    }

    private fun getCurrentUserId(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish() // 로그인이 되어있지 않다면 MainActivity로 이동(복귀)
        }

        return auth.currentUser?.uid.orEmpty() // 예외 처리
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}

    private fun like() {
        val card =
            cardItems[manager.topPosition - 1] // 일반적인 리사이클러 뷰가 아닌 커스텀 리사이클러 뷰를 사용하고 있기 때문에 top이 아닌 CardStackLayoutManager의 topPosition을 사용
        cardItems.removeFirst() // 처리하는 카드가 더 이상 필요가 없기 때문에 removeFirst() 사용

        // 타 사용자의 like에 저장
        userDB.child(card.userId)
            .child(LIKED_BY)
            .child(LIKE)
            .child(getCurrentUserId())
            .setValue(true) // 이후 접속시 스와이프 처리한 유저는 매칭이 되지 않음

        // 매칭이 된 시점
        saveMatchIfOtherLikedMe(card.userId)


        Toast.makeText(this, "${card.name}님을 Like 했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun disLike() {
        val card =
            cardItems[manager.topPosition - 1] // 일반적인 리사이클러 뷰가 아닌 커스텀 리사이클러 뷰를 사용하고 있기 때문에 top이 아닌 CardStackLayoutManager의 topPosition을 사용
        cardItems.removeFirst() // 처리하는 카드가 더 이상 필요가 없기 때문에 removeFirst() 사용

        // 타 사용자의 dislike에 저장
        userDB.child(card.userId)
            .child(LIKED_BY)
            .child(DISLIKE)
            .child(getCurrentUserId())
            .setValue(true) // 이후 접속시 스와이프 처리한 유저는 매칭이 되지 않음

        Toast.makeText(this, "${card.name}님을 disLike 했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun saveMatchIfOtherLikedMe(otherUserId: String) { // 나를 like한 타 사용자와 매칭
        val otherUserDB = userDB.child(getCurrentUserId()).child(LIKED_BY).child(LIKE).child(otherUserId)
        otherUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true) { // true일 때, 타 사용자가 사용자를 like
                    userDB.child(getCurrentUserId()) // 사용자의 db
                        .child(LIKED_BY)
                        .child(MATCH)
                        .child(otherUserId)
                        .setValue(true)

                    userDB.child(otherUserId) // 타 사용자의 db
                        .child(LIKED_BY)
                        .child(MATCH)
                        .child(getCurrentUserId())
                        .setValue(true)

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    override fun onCardSwiped(direction: Direction?) {
        when (direction) {
            Direction.Right -> like()
            Direction.Left -> disLike()
            else -> {
            }
        }
    }

    override fun onCardRewound() {}

    override fun onCardCanceled() {}

    override fun onCardAppeared(view: View?, position: Int) {}

    override fun onCardDisappeared(view: View?, position: Int) {}
}