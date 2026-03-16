package com.example.kanjiwidget
import androidx.appcompat.app.AppCompatActivity
import com.example.kanjiwidget.theme.ThemeController

abstract class ThemedActivity : AppCompatActivity() {
    protected fun applyPreparedTheme() {
        ThemeController.applyTheme(this)
    }

    protected fun runScreenEntranceAnimation() {
        ThemeController.animateScreenEntrance(this)
    }
}
