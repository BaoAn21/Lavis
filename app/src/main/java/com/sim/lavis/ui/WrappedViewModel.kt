package com.sim.lavis.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sim.lavis.LavisApplication
import com.sim.lavis.data.CsvExport
import com.sim.lavis.data.Wrapped
import com.sim.lavis.data.WrappedPeriod
import com.sim.lavis.data.WrappedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private var computeJob: Job? = null

    /** Cancels any in-flight computation so a slow, older result can't overwrite a newer period. */
    fun recompute() {
        computeJob?.cancel()
        computeJob = viewModelScope.launch {
            _wrapped.value = wrappedRepository.compute(_period.value, _offset.value)
        }
    }

    /**
     * Write the entire play-events log to [uri] as CSV.
     * DB read + CSV build + file write all happen off the main thread; [onResult] is invoked
     * back on the main thread with the row count (or null on failure) so the UI can report it.
     */
    fun exportCsv(resolver: ContentResolver, uri: Uri, onResult: (Int?) -> Unit) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                try {
                    val rows = wrappedRepository.exportRows()
                    val csv = CsvExport.playEvents(rows)
                    resolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                        ?: return@withContext null
                    rows.size
                } catch (e: Exception) {
                    null
                }
            }
            onResult(count)
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
