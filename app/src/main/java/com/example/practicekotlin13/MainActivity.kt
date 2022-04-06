  package com.example.practicekotlin13

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

  class MainActivity : AppCompatActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    /*
    값으로 지정하여 미리 객체 가져옴, loginActivity 에서의 사용과 다른 점은
    LoginActivity에서는 lateinit 으로 선언 후, onCreate()에서 초기화 해주었지만,
    MainActivity에서는 선언과 동시에 초기화
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

      override fun onStart() {
          super.onStart()

          if (auth.currentUser == null) { // firebase 서버에 회원 정보가 없다면, 등록된 회원이 없는 상태라면
              startActivity(Intent(this, LoginActivity::class.java))
          } else {
              startActivity(Intent(this, LikeActivity::class.java))
              finish()
          }
      }
}