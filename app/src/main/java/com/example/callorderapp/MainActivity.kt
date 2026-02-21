package com.example.callorderapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.callorderapp.audio.AudioRecorderManager
import java.io.File
import com.example.callorderapp.NativeBridge

class MainActivity : AppCompatActivity() {

    private lateinit var recorder: AudioRecorderManager
    private val REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Toast.makeText(this, NativeBridge.stringFromJNI(), Toast.LENGTH_LONG).show()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE
            )
        }

        val outputFile = File(getExternalFilesDir(null), "order_recording.wav")
        recorder = AudioRecorderManager(outputFile)

        findViewById<Button>(R.id.startBtn).setOnClickListener {
            recorder.startRecording()
            Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            recorder.stopRecording()
            Toast.makeText(this, "Recording Saved", Toast.LENGTH_SHORT).show()
        }
    }
}
