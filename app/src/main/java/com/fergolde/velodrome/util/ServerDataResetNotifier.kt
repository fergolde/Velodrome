package com.fergolde.velodrome.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Broadcasts that locally cached server data was discarded after a server-side
 * ID migration. Consumers holding in-memory copies keyed by server IDs (player
 * queue, radio library snapshot) observe [epoch] and reset their state.
 */
@Singleton
class ServerDataResetNotifier @Inject constructor() {

    private val _epoch = MutableStateFlow(0L)

    /** Increments once per performed reset. */
    val epoch: StateFlow<Long> = _epoch.asStateFlow()

    fun notifyReset() {
        _epoch.value += 1
    }
}
