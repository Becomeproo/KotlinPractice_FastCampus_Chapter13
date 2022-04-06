package com.example.practicekotlin13

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.practicekotlin13.StringKey.Companion.USERS
import com.example.practicekotlin13.StringKey.Companion.USER_ID
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth //FirebaseAuth 선언
    private lateinit var callbackManager: CallbackManager // facebook 로그인 여부 확인

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth // FirebaseAuth.getInstance()와 같음, kotlin이기 때문에 간단히 축약가능
        callbackManager = CallbackManager.Factory.create() // callbackkMangaer 초기화

        initLoginButton()
        initSignUpButton()
        initEmailAndPasswordEditText()
        initFacebookLoginButton()
    }

    private fun initLoginButton() { // 로그인 버튼 클릭
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener {
            val email = getInputEmail()
            val password = getInputPassword()

            auth.signInWithEmailAndPassword(email, password) // 가져온 email과 password를 이용해 로그인
                .addOnCompleteListener(this) { task -> // task가 완료가 되었는지 확인
                    if (task.isSuccessful) { // task가 완료되었다면 종료, 현재의 액티비티를 종료하게 되므로 MainActivity로 이동(복귀)
                        handleSuccessLogin()
                    } else {
                        Toast.makeText(
                            this,
                            "로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
        }
    }

    private fun initSignUpButton() { // 회원가입 버튼 클릭
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        signUpButton.setOnClickListener {

            val email = getInputEmail()
            val password = getInputPassword()


            auth.createUserWithEmailAndPassword(email, password) // 회원가입
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "회원가입에 성공했습니다. 로그인 버튼을 눌러 로그인해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this, "이미 가입한 이메일이거나, 회원가입에 실패했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }

    private fun initEmailAndPasswordEditText() {
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        // 이메일과 비밀번호가 빈 값일 때, 로그인 버튼과 회원가입 버튼을 비활성화
        emailEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable

        }

        passwordEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable

        }
    }

    private fun initFacebookLoginButton() { // facebook 로그인 버튼 클릭
        val facebookLoginButton = findViewById<LoginButton>(R.id.facebookLoginButton)

        facebookLoginButton.setPermissions("email", "public_profile") // 페이스북 정보를 가져올 때 어떤 정보를 가져올 지 설정
        facebookLoginButton.registerCallback(callbackManager, object: FacebookCallback<LoginResult> { // facebookCallback 호출
            override fun onSuccess(result: LoginResult) { // 로그인 성공
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token) // 로그인 성공으로 획득한 토큰의 정보
                auth.signInWithCredential(credential) // 위에서 획득한 토큰 정보를 firebase로 전달
                    .addOnCompleteListener(this@LoginActivity) { task ->
                        if (task.isSuccessful) {
                            handleSuccessLogin()
                        } else {
                            Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            override fun onCancel() {

            }

            override fun onError(error: FacebookException?) {
                Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패했습니다.", Toast.LENGTH_SHORT).show() // this@LoginActivity -> 일반적으로 this를 하면 callback을 가리키기 때문에 오류
            }

        })
    }

    private fun getInputEmail(): String { // 이메일 가져오기
        return findViewById<EditText>(R.id.emailEditText).text.toString()
    }

    private fun getInputPassword(): String { // 비밀번호 가져오기
        return findViewById<EditText>(R.id.passwordEditText).text.toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleSuccessLogin() {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid.orEmpty() // currrentUser 설정
        val currentUserDB = Firebase.database.reference.child(USERS).child(userId) // reference(최상위)에서 User 라는 child를 가져옴
        val user = mutableMapOf<String, Any>()
        user[USER_ID] = userId
        currentUserDB.updateChildren(user) // DB내에 user 정보 저장

        finish()
    }


}