package fr.haan.bipak.android

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.haan.bipak.PagingEventEmitter
import fr.haan.bipak.PagingViewEvent
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * [ListAdapter] base class for presenting paged data from [fr.haan.bipak.PagingData]s in
 * a [RecyclerView].
 *
 * This class is a convenience wrapper around [PagingEventEmitter] that implements common default
 * behavior for listening to PagedList update callbacks.
 */
public abstract class PagingDataAdapter<T, VH : RecyclerView.ViewHolder>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, VH>(diffCallback) {

    private val eventEmitter = PagingEventEmitter()
    public val eventFlow: MutableSharedFlow<PagingViewEvent> get() = eventEmitter.eventFlow

    /**
     * Begin fetching data
     */
    public fun start() {
        if (super.getItemCount() <= 0) {
            // Trigger first page fetch
            eventEmitter.onGetItem(0)
        }
    }

    /**
     * Stop fetching data
     */
    public fun stop() {
        eventEmitter.stop()
    }

    /**
     * Retry fetching data in case of error
     */
    public fun retry() {
        eventEmitter.retry()
    }

    override fun getItem(position: Int): T {
        eventEmitter.onGetItem(position)
        return super.getItem(position)
    }

    override fun getItemCount(): Int {
        return super.getItemCount()
    }

    /**
     * Submit a new list of data
     * @param list List of data to display
     * @param bottomItemOffset Count of additional footer view (for instance Loading) added in the list
     */
    public fun submitList(list: List<T>, bottomItemOffset: Int = 0) {
        eventEmitter.listSize = list.size
        eventEmitter.bottomItemOffset = bottomItemOffset
        super.submitList(list)
    }
}
