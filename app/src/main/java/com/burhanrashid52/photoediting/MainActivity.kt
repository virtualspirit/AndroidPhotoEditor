package com.burhanrashid52.photoediting

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ja.burhanrashid52.photoediting.EditImageActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.button).setOnClickListener {
            val intent = Intent(applicationContext, EditImageActivity::class.java)


            intent.putExtra("path", "https://virtualspirit.me/assets/front-end/software-development@2x-93c79cac29e995da260cb38bafb3fc19b8d5307ac7f4c719d62c658a004ba701.png")

            startActivity(intent)
        }
    }
}