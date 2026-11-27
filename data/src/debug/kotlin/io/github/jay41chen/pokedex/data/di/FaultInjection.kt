package io.github.jay41chen.pokedex.data.di

import android.content.Context
import io.github.jay41chen.pokedex.data.remote.FaultInjectionInterceptor
import java.io.File
import okhttp3.Interceptor

// Usage: put one full URL per line in files/fault_urls.txt, e.g. https://pokeapi.co/api/v2/pokemon/25
internal fun faultInjectionInterceptors(context: Context): List<Interceptor> =
    listOf(FaultInjectionInterceptor(File(context.filesDir, "fault_urls.txt")))
