package fr.haan.bipak

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile

/**
 * Primary entry point into Paging; constructor for a reactive stream of [PagingData].
 *
 * Each [PagingData] represents the current paging state and data.
 * [Pager] will fetch relevant data based on [PagingViewEvent] received via [subscribeToViewEvents]
 *
 */
public class Pager<Key : Any, Value : Any>(
    /**
     * The [CoroutineScope] used to fetch the content asynchronously
     */
    private val scope: CoroutineScope,
    /**
     * The [PagingDataSource] from which data will be fetched
     */
    private val source: PagingDataSource<Key, Value>,
    /**
     * This is the first page identifier that has to be fetched
     */
    private val initialKey: Key,
    /**
     * Optional paging configuration. @see [PagingConfig]
     */
    private val config: PagingConfig = PagingConfig(),
) {
    private val _dataFlow: MutableStateFlow<PagingData<Value>> = MutableStateFlow(PagingData.empty())

    /**
     * Hot [Flow] of [PagingData], exposing the current state & data.
     * A new item will be emitted whenever the [PagingData.LoadState] or [PagingData.list] is updated
     */
    public val dataFlow: Flow<PagingData<Value>> = _dataFlow

    private sealed class InternalPage<Key : Any, Value : Any> {
        class ToFetch<Key : Any, Value : Any> : InternalPage<Key, Value>()
        class Loading<Key : Any, Value : Any>(internal val result: Deferred<InternalPage<Key, Value>>) :
            InternalPage<Key, Value>()

        data class Page<Key : Any, Value : Any>(
            val data: List<Value>,
            val nextKey: Key?,
            val totalCount: Int?,
        ) : InternalPage<Key, Value>()

        data class Error<Key : Any, Value : Any>(
            val error: Throwable,
        ) : InternalPage<Key, Value>()
    }

    private val currentPagingData = ArrayList<InternalPage<Key, Value>>()

    /**
     * Collect events from the view to triggers page loads.
     * Will suspend until eventFlow is terminated by sending [PagingViewEvent.Terminate].
     * This can be done by calling [PagingEventEmitter.stop]
     * @param eventFlow The flow of event coming from the view; May be emitted by [PagingEventEmitter]
     *
     */
    public suspend fun subscribeToViewEvents(eventFlow: MutableSharedFlow<PagingViewEvent>) {
        eventFlow
            .onEach { viewEvent ->
                pagingDebugLog("Pager: received: ${viewEvent::class.simpleName}")
                if (viewEvent is PagingViewEvent.Terminate) {
                    // This prevent terminating again when re-subscribing
                    eventFlow.resetReplayCache()
                }
            }
            // Allows to terminate the Flow
            .takeWhile { it !is PagingViewEvent.Terminate }
            .collect { viewEvent ->
                when (viewEvent) {
                    is PagingViewEvent.ElementRequested -> {
                        pagingDebugLog("ElementRequested: ${viewEvent.index}")
                        fetchPageAtIndex(getPageIndexForElementIndex(viewEvent))
                    }

                    PagingViewEvent.Idle -> {
                        /* Default state, do nothing */
                    }

                    PagingViewEvent.Retry -> fetchPageAtIndex(getPageIndexToRetry(), retry = true)
                    PagingViewEvent.Terminate -> return@collect // We shouldn't go there, thanks to takeWhile operator
                }
            }
    }

    private fun getPageIndexToRetry(): Int {
        return currentPagingData.indexOfFirst { it is InternalPage.Error }
    }

    private fun getPageIndexForElementIndex(event: PagingViewEvent.ElementRequested): Int {
        return (event.index + config.prefetchDistance) / config.pageSize
    }

    private suspend fun fetchPageAtIndex(pageIndex: Int, retry: Boolean = false) {
        pagingDebugLog("fetchPageAtIndex(): $pageIndex")
        if (pageIndex < 0) return
        if (currentPagingData.getOrNull(pageIndex) is InternalPage.Page) return

        var currentIndex = 0
        var previousResult: InternalPage.Page<Key, Value>? = null
        while (true) {
            val currentPage: InternalPage<Key, Value> =
                (currentPagingData.getOrNull(currentIndex) ?: InternalPage.ToFetch())
            pagingDebugLog("fetchPageAtIndex() currentIndex: $currentIndex, currentPage: $currentPage")
            val shouldRetry = retry && currentPage is InternalPage.Error
            pagingDebugLog("shouldRetry: $shouldRetry")

            when {

                currentPage is InternalPage.ToFetch || shouldRetry -> {
                    val deferredResult = fetchPageAtKey(currentIndex, previousResult?.nextKey ?: initialKey)

                    // Emit load state
                    val loadState = InternalPage.Loading(deferredResult)
                    currentPagingData.addOrReplace(currentIndex, loadState)
                    handleResult(loadState)

                    val result = deferredResult.await()
                    pagingDebugLog("fetchPageAtIndex(): result $result")
                    when (result) {
                        is InternalPage.Error -> {
                            currentPagingData.addOrReplace(currentIndex, result)
                            handleResult(result)
                            return
                        }

                        is InternalPage.Page -> {
                            currentPagingData.addOrReplace(currentIndex, result)
                            if (currentIndex == pageIndex) {
                                handleResult(result)
                                return
                            } else {
                                previousResult = result
                                if (result.nextKey != null) {
                                    currentIndex++
                                    currentPagingData.addOrReplace(currentIndex, InternalPage.ToFetch())
                                    handleResult(result)
                                } else {
                                    // End of list
                                    return
                                }
                            }
                        }

                        is InternalPage.Loading -> assert(false) { "Shoudln't be in loading state after await" }
                        is InternalPage.ToFetch -> assert(false) { "Shoudln't be in ToFetch state after await" }
                    }
                }

                currentPage is InternalPage.Error -> {
                    // Do nothing, retry has to be called explicitly
                    return
                }

                currentPage is InternalPage.Loading -> {
                    // Do nothing, result will be emitted on [dataFlow]
                    return
                }

                currentPage is InternalPage.Page -> {
                    // Page requested is already in cache
                    if (currentIndex == pageIndex) {
                        // We have everything asked, exit
                        return
                    } else {
                        // Try to load next page
                        if (currentPage.nextKey != null) {
                            previousResult = currentPage
                            currentIndex++
                        } else {
                            // End of list
                            return
                        }
                    }
                }
            }
        }
    }

    private fun assert(b: Boolean, function: () -> String) {
        // noop
    }

    private suspend fun fetchPageAtKey(pageIndex: Int, key: Key): Deferred<InternalPage<Key, Value>> {
        pagingDebugLog("fetchPageAtKey(): pageIndex: $pageIndex, key: $key")
        val deferredResult: Deferred<InternalPage<Key, Value>> = scope.async {
            val result = source.load(
                PagingDataSource.LoadParams.Append(
                    key,
                    config.pageSize,
                )
            )
            return@async when (result) {
                is PagingDataSource.LoadResult.Error -> {
                    InternalPage.Error(
                        result.throwable
                    )
                }

                is PagingDataSource.LoadResult.Page -> {
                    InternalPage.Page(
                        result.data,
                        nextKey = result.nextKey,
                        totalCount = result.totalCount,
                    )
                }
            }
        }
        return deferredResult
    }

    private fun handleResult(result: InternalPage<Key, Value>) {

        when (result) {
            is InternalPage.Error -> handleError(result.error)
            is InternalPage.Page, is InternalPage.Loading -> handleSourceResult()
            is InternalPage.ToFetch -> { /* NOOP */
            }
        }
    }

    private fun getCurrentInternaList(): List<InternalPage.Page<Key, Value>> {
        return currentPagingData
            .takeWhile { it is InternalPage.Page }
            .map { it as InternalPage.Page }
    }

    private fun getCurrentList(): List<Value> {
        val currentList = getCurrentInternaList()
            .map { it.data }
            .fold(emptyList<Value>()) { acc, next -> acc + next }

        return currentList
    }

    private fun getCurrentState(): PagingData.LoadState {
        val loadingPages = currentPagingData
            .filter { it is InternalPage.Loading || it is InternalPage.ToFetch }

        if (loadingPages.isNotEmpty()) {
            return PagingData.LoadState.Loading
        } else {
            return PagingData.LoadState.NotLoading
        }
    }

    private fun handleSourceResult() {
        _dataFlow.tryEmit(
            PagingData(
                list = getCurrentList(),
                state = getCurrentState(),
                totalCount = getCurrentInternaList().lastOrNull()?.totalCount,
            )
        )
    }

    private fun handleError(error: Throwable) {
        _dataFlow.tryEmit(
            PagingData(
                list = getCurrentList(),
                state = PagingData.LoadState.Error(error),
                totalCount = getCurrentInternaList().lastOrNull()?.totalCount,
            )
        )
    }

    private fun <E> ArrayList<E>.addOrReplace(index: Int, element: E) {
        if (this.lastIndex < index) {
            this.add(index, element)
        } else {
            this.set(index, element)
        }
    }
}
