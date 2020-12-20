package com.example.smbtest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

private lateinit var mAuth: FirebaseAuth

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            mAuth.createUserWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
                .addOnCompleteListener() {
                    if(it.isSuccessful) {
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Registration successful")
                    } else {
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "Registration failed: " + it.exception)
                    }
                }
        }

        val inLoginAct = Intent(this, MainActivity::class.java)
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            mAuth.signInWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
                .addOnCompleteListener() {
                    if(it.isSuccessful) {
                        inLoginAct.putExtra("user_mail", mAuth.currentUser?.email)
                        inLoginAct.putExtra("user", mAuth.currentUser)
                        Log.d(TAG, "Login successful")
                        finish()
                        startActivity(inLoginAct)
                    } else {
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "Login failed " + it.exception)
                    }
                }
        }

        findViewById<Button>(R.id.btnLoginAnonymously).setOnClickListener {
            mAuth.signInAnonymously().addOnCompleteListener() {
                if(it.isSuccessful) {
                    Log.d(TAG, "Login successful")
                    finish()
                    startActivity(inLoginAct)
                } else {
                    Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "Login failed" + it.exception)
                }
            }
        }

    }

    companion object {
        const val TAG = "LOGIN_ACTIVITY"
    }
}