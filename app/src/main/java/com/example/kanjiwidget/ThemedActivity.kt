package com.example.kanjiwidget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kanjiwidget.theme.ThemeController

abstract class ThemedActivity : AppCompatActivity() {
    protected fun prepareTheme(savedInstanceState: Bundle?) {
        ThemeController.applyTheme(this)
        super.onCreate(savedInstanceState)
    }

    protected fun runScreenEntranceAnimation() {
        ThemeController.animateScreenEntrance(this)
    }
}
