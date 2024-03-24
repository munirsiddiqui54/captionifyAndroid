package com.example.captionify

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener{
    private var mTts: TextToSpeech? = null
    private val TAG = "TextToSpeechDemo"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
    override fun onInit(status: Int) {
        // Your onInit code here
        if (status == TextToSpeech.SUCCESS) {
            val result = mTts?.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
            } else {


                findViewById<Button>(R.id.btn).setOnClickListener{
                    val text=findViewById<EditText>(R.id.editText)
                    speak(text.text.toString())
                    text.setText("")
                }
            }
        } else {
            Log.e(TAG, "Initialization failed")
        }
    }

    private fun speak(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            mTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    override fun onDestroy() {
        if (mTts != null) {
            mTts!!.stop()
            mTts!!.shutdown()
        }
        super.onDestroy()
    }
}