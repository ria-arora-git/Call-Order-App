package com.example.callorderapp

object NativeBridge {

    init {
        System.loadLibrary("native-lib")
    }

    external fun transcribe(modelPath: String, audioPath: String): String
}