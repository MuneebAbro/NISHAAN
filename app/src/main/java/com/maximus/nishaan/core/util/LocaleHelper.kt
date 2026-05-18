package com.maximus.nishaan.core.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun wrap(context: Context, language: String): Context {
        val locale = when (language) {
            "ur" -> Locale("ur")
            else -> Locale("en")
        }
        
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }
}
