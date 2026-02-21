package com.example.callorderapp

object NativeBridge {

    init {
        System.loadLibrary("native-lib")
    }

    external fun stringFromJNI(): String
}