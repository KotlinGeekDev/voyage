package com.dluvian.voyage.data.event

import android.util.Log
import com.dluvian.nostr_kt.RelayUrl
import com.dluvian.nostr_kt.SubId
import com.dluvian.voyage.data.model.RelayedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rust.nostr.protocol.Event
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "EventQueue"
private const val EVENT_PROCESSING_DELAY = 500L

class EventQueue(
    private val qualityGate: EventQueueQualityGate,
    private val eventProcessor: EventProcessor,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Not a synchronized set bc we synchronize with `synchronized()`
    private val queue = mutableSetOf<RelayedItem<Event>>()
    private val isProcessingEvents = AtomicBoolean(false)

    init {
        startProcessingJob()
    }

    fun submit(event: Event, subId: SubId, relayUrl: RelayUrl?) {
        if (relayUrl == null) {
            Log.w(TAG, "Unknown relay origin of eventId ${event.id().toHex()} of subId $subId")
            return
        }
        if (!qualityGate.isSubmittable(event = event, subId = subId, relayUrl = relayUrl)) {
            return
        }
        synchronized(queue) { queue.add(RelayedItem(item = event, relayUrl = relayUrl)) }
        if (!isProcessingEvents.get()) startProcessingJob()
    }

    private fun startProcessingJob() {
        if (!isProcessingEvents.compareAndSet(false, true)) return
        Log.i(TAG, "Start job")
        scope.launch {
            while (true) {
                delay(EVENT_PROCESSING_DELAY)
                val events = mutableSetOf<RelayedItem<Event>>()
                synchronized(queue) {
                    events.addAll(queue)
                    queue.clear()
                }
                eventProcessor.processEvents(events = events)
            }
        }.invokeOnCompletion {
            Log.w(TAG, "Processing job completed", it)
            isProcessingEvents.set(false)
        }
    }
}