package fr.haan.bipak.compose

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import fr.haan.bipak.PagingData
import fr.haan.bipak.PagingEventEmitter
import fr.haan.bipak.PagingViewEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * The class responsible for accessing the data from a [Flow] of [PagingData].
 * In order to obtain an instance of [LazyPagingItems] use the [collectAsLazyPagingItems] extension
 * method of [Flow] with [PagingData].
 * This instance can be used by the [items] and [itemsIndexed] methods inside [LazyListScope] to
 * display data received from the [Flow] of [PagingData].
 *
 * @param T the type of value used by [PagingData].
 */
public data class LazyPagingItems<T : Any>(
    internal val state: State<PagingData<T>>,
) {

    internal var eventEmitter = PagingEventEmitter()

    /**
     * Retry fetching data in case of error
     */
    public fun retry() {
        eventEmitter.retry()
    }

    /**
     * Current load state (can be used to display a load indicator)
     */
    public val loadState: PagingData.LoadState
        get() = state.value.state

    /**
     * Flow of [PagingViewEvent] to be passed to the [fr.haan.bipak.Pager] instance
     */
    public val eventFlow: MutableSharedFlow<PagingViewEvent>
        get() = eventEmitter.eventFlow
}

/**
 * Collects values from this [Flow] of [PagingData] and represents them inside a [LazyPagingItems]
 * instance. The [LazyPagingItems] instance can be used by the [items] and [itemsIndexed] methods
 * from [LazyListScope] in order to display the data obtained from a [Flow] of [PagingData].
 */
@Composable
public fun <T : Any> Flow<PagingData<T>>.collectAsLazyPagingItems(): LazyPagingItems<T> {
    val state = this.collectAsState(PagingData.empty())
    val lazyPagingItems = remember(this) { LazyPagingItems(state) }

    LaunchedEffect(this) {
        lazyPagingItems.eventEmitter.onGetItem(0)
    }

    DisposableEffect(this) {
        onDispose {
            lazyPagingItems.eventEmitter.stop()
        }
    }

    return lazyPagingItems.copy(
        state = state
    ).apply {
        eventEmitter = lazyPagingItems.eventEmitter
    }
}

/**
 * Adds the [LazyPagingItems] and their content to the scope.
 *
 * @param pagingDataState [LazyPagingItems] instance to display
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed.
 * @param itemContent the content displayed by a single item.
 */
public fun <T : Any> LazyListScope.items(
    pagingDataState: LazyPagingItems<T>,
    key: ((index: Int) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(value: T?) -> Unit,
) {
    val items = pagingDataState.state.value.list
    items(items.size, key) { index ->
        pagingDataState.eventEmitter.onGetItem(index)
        itemContent(items[index])
    }
}

/**
 * Adds the [LazyPagingItems] and their content to the scope where the content of an item is
 * aware of its local index.
 *
 * @param pagingDataState [LazyPagingItems] instance to display
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed.
 * @param itemContent the content displayed by a single item.
 */
public fun <T : Any> LazyListScope.itemsIndexed(
    pagingDataState: LazyPagingItems<T>,
    key: ((index: Int) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(index: Int, value: T?) -> Unit,
) {
    val items = pagingDataState.state.value.list
    items(items.size, key) { index ->
        pagingDataState.eventEmitter.onGetItem(index)
        itemContent(index, items[index])
    }
}
