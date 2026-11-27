package io.github.jay41chen.pokedex.data.di

import android.content.Context
import okhttp3.Interceptor

// Release ships no fault injection.
internal fun faultInjectionInterceptors(context: Context): List<Interceptor> = emptyList()
