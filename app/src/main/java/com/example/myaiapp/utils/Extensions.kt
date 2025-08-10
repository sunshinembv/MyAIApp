package com.example.myaiapp.utils

import android.content.Context
import com.example.myaiapp.MyApp
import com.example.myaiapp.di.AppComponent

val Context.appComponent: AppComponent
    get() = when (this) {
        is MyApp -> appComponent
        else -> applicationContext.appComponent
    }
