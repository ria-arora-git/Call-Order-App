package com.example.callorderapp.audio

import android.media.*
import java.io.File
import java.io.FileOutputStream

class AudioRecorderManager(private val outputFile: File) {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var tempRawFile: File? = null

    fun startRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        tempRawFile = File(outputFile.parent, "temp_audio.raw")

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            writeAudioDataToFile(bufferSize)
        }

        recordingThread?.start()
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        convertRawToWav()
        tempRawFile?.delete()
    }

    private fun writeAudioDataToFile(bufferSize: Int) {
        val data = ByteArray(bufferSize)

        FileOutputStream(tempRawFile).use { fos ->
            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (read > 0) {
                    fos.write(data, 0, read)
                }
            }
        }
    }

    private fun convertRawToWav() {
        val rawData = tempRawFile?.readBytes() ?: return
        WavUtil.writeWavFile(outputFile, rawData, sampleRate)
    }
}
