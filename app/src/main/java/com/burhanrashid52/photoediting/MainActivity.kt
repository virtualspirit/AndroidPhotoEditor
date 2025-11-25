package com.burhanrashid52.photoediting

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ja.burhanrashid52.photoediting.EditImageActivity
import ja.burhanrashid52.photoediting.constant.ResponseCode

class MainActivity : AppCompatActivity() {
    private val EDIT_SUCCESSFUL = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        findViewById<Button>(R.id.button).setOnClickListener {
            val intent = Intent(applicationContext, EditImageActivity::class.java)


            intent.putExtra("path", "https://virtualspirit.me/assets/front-end/software-development@2x-93c79cac29e995da260cb38bafb3fc19b8d5307ac7f4c719d62c658a004ba701.png")
            val tools = arrayOf("line", "circle", "clip", "filter", "imageSticker", "textSticker")
            intent.putExtra("tools", tools)
            
            // Custom sticker URLs
            val customStickers = arrayOf(
                "https://cdn-icons-png.flaticon.com/256/4392/4392452.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392455.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392459.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392462.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392465.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392467.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392469.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392471.png",
                "https://cdn-icons-png.flaticon.com/256/4392/4392522.png",
            )
            intent.putExtra("stickerPaths", customStickers)
            
            startActivity(intent)
        }
    }
}