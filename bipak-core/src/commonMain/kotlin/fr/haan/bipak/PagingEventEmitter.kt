package fr.haan.bipak

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * This class exposes an [eventFlow] ready to be consumed by [Pager]
 * It's a Helper class to be used in the UI layer.
 * Calling [onGetItem] by giving the index of the currently displayed item will trigger the fetching and prefetching
 * of data based on [Pager] configuration and state.
 */
public class PagingEventEmitter {
    private val _eventFlow: MutableSharedFlow<PagingViewEvent> = MutableSharedFlow(
        replay = 10,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Hot flow of [PagingViewEvent], to be consumed by [Pager]
     */
    public val eventFlow: MutableSharedFlow<PagingViewEvent> = _eventFlow

    /**
     * This can be used to prevent loading un-needed data when calling [onGetItem] for an index which is not data,
     * such as a bottom loading view.
     * Value of 0 (default) will have no effect
     */
    public var bottomItemOffset: Int = 0

    /**
     * This is the list size used to discard event based on the previous parameter.
     * This is not taken into account if bottomItemOffset is 0
     */
    public var listSize: Int = 0

    /**
     * Will trigger the fetching and prefetching of data based on [Pager] configuration and state.
     * This has to be called when the data at the index provided is being displayed (for instance when binding data and
     * view in a list adapter logic)
     * @param index The index of item currently being displayed
     */
    public fun onGetItem(index: Int) {

        if (bottomItemOffset > 0 && index in ((listSize - bottomItemOffset) until listSize)) {
            return
        }
        _eventFlow.tryEmit(PagingViewEvent.ElementRequested(index))
    }

    /**
     * Will retry the fetching of data in case of Error
     * @see PagingData.LoadState.Error
     */
    public fun retry() {
        _eventFlow.tryEmit(PagingViewEvent.Retry)
    }

    /**
     * Will request the pager to stop reacting to [eventFlow]. This must be called when the screen displaying
     * the list is not visible anymore.
     * The paging operation can be restored by calling [Pager.subscribeToViewEvents]
     */
    public fun stop() {
        _eventFlow.tryEmit(PagingViewEvent.Terminate)
    }
}
