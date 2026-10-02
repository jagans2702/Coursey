package com.example.coursey

import android.app.Application
import com.example.coursey.core.di.AppContainer

class LearningApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
