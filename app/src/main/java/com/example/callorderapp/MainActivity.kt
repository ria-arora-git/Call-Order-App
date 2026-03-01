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
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var transcriptText: TextView
    private lateinit var statusText: TextView
    private lateinit var recorder: AudioRecorderManager
    private val REQUEST_CODE = 101
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        transcriptText = findViewById(R.id.transcriptText)
        val recent = loadTranscripts()
        if (recent.isNotEmpty()) {
            transcriptText.text = recent.joinToString("\n\n----------------\n\n")
        }
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

            if (isProcessing) {
                statusText.text = "Already processing..."
                return@setOnClickListener
            }

            recorder.stopRecording()

            isProcessing = true
            statusText.text = "Processing..."
            transcriptText.text = ""

            val modelPath = copyModelIfNeeded()
            val audioPath = File(getExternalFilesDir(null), "order_recording.wav").absolutePath

            Thread {
                val result = NativeBridge.transcribe(modelPath, audioPath)

                runOnUiThread {
                    saveTranscript(result)
                    val recent = loadTranscripts()
                    transcriptText.text = recent.joinToString("\n\n----------------\n\n")
                    statusText.text = "Transcription Complete"
                    isProcessing = false
                }
            }.start()
        }
    }

    private fun copyModelIfNeeded(): String {
        val file = File(filesDir, "ggml-base-q5_1.bin")

        if (!file.exists()) {
            assets.open("ggml-base-q5_1.bin").use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return file.absolutePath
    }

    private fun saveTranscript(newTranscript: String) {
        val file = File(filesDir, "transcripts.json")

        val list = if (file.exists()) {
            file.readText().split("||").toMutableList()
        } else {
            mutableListOf()
        }

        list.add(0, newTranscript) // newest first

        if (list.size > 4) {
            list.removeAt(list.size - 1)
        }

        file.writeText(list.joinToString("||"))
    }

    private fun loadTranscripts(): List<String> {
        val file = File(filesDir, "transcripts.json")
        return if (file.exists()) {
            file.readText().split("||")
        } else {
            emptyList()
        }
    }
}
