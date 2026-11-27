package io.github.jay41chen.pokedex.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jay41chen.pokedex.domain.usecase.RequestSync
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val requestSync: RequestSync) : ViewModel() {
    fun onAppForegrounded() = requestSync()
}
