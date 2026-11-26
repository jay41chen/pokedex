package io.github.jay41chen.pokedex.ui.main

import androidx.lifecycle.ViewModel
import io.github.jay41chen.pokedex.domain.usecase.RequestSync
import javax.inject.Inject

// 尚未加 @HiltViewModel：repo binding 還不存在，Hilt 整合階段再加。
class MainViewModel @Inject constructor(private val requestSync: RequestSync) : ViewModel() {
    fun onAppForegrounded() = requestSync()
}
