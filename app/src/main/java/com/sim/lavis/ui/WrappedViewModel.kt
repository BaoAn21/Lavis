package com.sim.lavis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sim.lavis.LavisApplication
import com.sim.lavis.data.Wrapped
import com.sim.lavis.data.WrappedPeriod
import com.sim.lavis.data.WrappedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WrappedViewModel(private val wrappedRepository: WrappedRepository) : ViewModel() {

    private val _period = MutableStateFlow(WrappedPeriod.WEEK)
    val period: StateFlow<WrappedPeriod> = _period.asStateFlow()

    /** 0 = current period, 1 = previous, ... */
    private val _offset = MutableStateFlow(0)
    val offset: StateFlow<Int> = _offset.asStateFlow()

    private val _wrapped = MutableStateFlow<Wrapped?>(null)
    val wrapped: StateFlow<Wrapped?> = _wrapped.asStateFlow()

    init {
        recompute()
    }

    fun setPeriod(period: WrappedPeriod) {
        _period.value = period
        _offset.value = 0
        recompute()
    }

    fun previousPeriod() {
        _offset.value += 1
        recompute()
    }

    fun nextPeriod() {
        if (_offset.value > 0) {
            _offset.value -= 1
            recompute()
        }
    }

    fun recompute() {
        viewModelScope.launch {
            _wrapped.value = wrappedRepository.compute(_period.value, _offset.value)
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LavisApplication
                return WrappedViewModel(app.wrappedRepository) as T
            }
        }
    }
}
