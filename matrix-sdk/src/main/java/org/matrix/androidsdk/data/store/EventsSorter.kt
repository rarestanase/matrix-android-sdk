package org.matrix.androidsdk.data.store

import org.matrix.androidsdk.rest.model.Event

class EventsSorter {
    fun sortChronologically(
        eventsToSort: LinkedHashMap<String, Event>
    ): LinkedHashMap<String, Event> {
        val sortedEvents =
            eventsToSort.entries.sortedBy {
                it.value.originServerTs
            }
        val sortedEventsMap = linkedMapOf<String, Event>()
        sortedEvents.forEach { entry ->
            sortedEventsMap.put(entry.key, entry.value)
        }
        return sortedEventsMap
    }
}