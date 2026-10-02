package com.soulstream.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A download currently running (or just finished) - shown live inside the app. */
data class ActiveJob(
    val id: String,
    val title: String,
    val progress: Int,
    val status: Status
) {
    enum class Status { PREPARING, DOWNLOADING, DONE, FAILED }

    val isRunning: Boolean
        get() = status == Status.PREPARING || status == Status.DOWNLOADING
}

object LiveDownloads {

    private val _jobs = MutableStateFlow<List<ActiveJob>>(emptyList())
    val jobs: StateFlow<List<ActiveJob>> = _jobs

    @Synchronized
    fun upsert(job: ActiveJob) {
        val cur = _jobs.value.toMutableList()
        val i = cur.indexOfFirst { it.id == job.id }
        if (i >= 0) cur[i] = job else cur.add(0, job)
        _jobs.value = cur
    }

    @Synchronized
    fun remove(id: String) {
        _jobs.value = _jobs.value.filterNot { it.id == id }
    }

    @Synchronized
    fun clearFinished() {
        _jobs.value = _jobs.value.filter { it.isRunning }
    }
}
