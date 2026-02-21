package com.example.callorderapp.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavUtil {

    fun writeWavFile(file: File, audioData: ByteArray, sampleRate: Int) {
        val totalDataLen = audioData.size + 36
        val byteRate = sampleRate * 2

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44)
            header.order(ByteOrder.LITTLE_ENDIAN)

            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1.toShort())
            header.putShort(1.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(2.toShort())
            header.putShort(16.toShort())
            header.put("data".toByteArray())
            header.putInt(audioData.size)

            out.write(header.array())
            out.write(audioData)
        }
    }
}
