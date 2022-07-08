@file:OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)

package fr.haan.bipak.test

import app.cash.turbine.test
import fr.haan.bipak.Pager
import fr.haan.bipak.PagingConfig
import fr.haan.bipak.PagingData
import fr.haan.bipak.PagingDataSource
import fr.haan.bipak.PagingEventEmitter
import fr.haan.bipak.test.util.awaitForItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

private const val INITIAL_KEY = 0
private const val SECOND_PAGE_KEY = INITIAL_KEY + 1
private const val THIRD_PAGE_KEY = SECOND_PAGE_KEY + 1

private const val PAGE_SIZE = 10
private const val FIRST_ITEM_INDEX = 0

private const val FIRST_PAGE_LAST_ITEM_INDEX = PAGE_SIZE - 1
private const val SECOND_PAGE_FIRST_ITEM_INDEX = PAGE_SIZE
private const val SECOND_PAGE_SECOND_ITEM_INDEX = SECOND_PAGE_FIRST_ITEM_INDEX + 1
private const val SECOND_PAGE_LAST_ITEM_INDEX = SECOND_PAGE_FIRST_ITEM_INDEX + PAGE_SIZE - 1

private const val THIRD_PAGE_FIRST_ITEM_INDEX = PAGE_SIZE * 2
private const val THIRD_PAGE_SECOND_ITEM_INDEX = THIRD_PAGE_FIRST_ITEM_INDEX + 1

class MockPagingDataSource : PagingDataSource<Int, String>() {
    var error: Throwable? = null
    val errorsForKeys: MutableMap<Int, Throwable> = mutableMapOf<Int, Throwable>()
    var endIndex: Int? = null
    var loadCallCount = 0

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
        loadCallCount++
        if (error != null) return LoadResult.Error(error!!)

        errorsForKeys.get(params.key)?.let {
            return LoadResult.Error(it)
        }
        params.loadSize

        val page = (params.key ?: 0)
        val data = (0..(params.loadSize - 1)).map { page * PAGE_SIZE + it }.map { it.toString() }.toList()
        val nextKeyValue = (params.key ?: 0) + 1

        val nextKey: Int?
        if (endIndex != null && params.key != null) {
            if (params.key!! >= endIndex!!) {
                nextKey = null
            } else {
                nextKey = nextKeyValue
            }
        } else {
            nextKey = nextKeyValue
        }

        return LoadResult.Page(
            data = data,
            prevKey = null,
            nextKey = nextKey,
            // If endIndex is set compute total count, default to 1000 arbitrary
            totalCount = endIndex?.let { (it + 1) * PAGE_SIZE } ?: 1000
        )
    }
}

internal class PagingTest {

    val pagingConfig = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 0,
    )

    @Test
    fun when_item_index_0_is_requested_then_first_page_is_loaded() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager<Int, String>(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        // When
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)

        // Then
        pager.dataFlow.test {
            val item = awaitForItem { it?.list?.isNotEmpty() ?: false }
            assertEquals(PAGE_SIZE, item.list.size)
        }
        job.cancel()
    }

    @Test
    fun when_first_items_are_requested_several_times_then_first_page_is_fetched_only_once() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager<Int, String>(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        // When
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)
        eventEmitter.onGetItem(1)
        eventEmitter.onGetItem(1)
        eventEmitter.onGetItem(1)
        eventEmitter.onGetItem(2)

        // Then
        pager.dataFlow.test {
            val item = awaitForItem { it?.list?.isNotEmpty() ?: false }
            assertEquals(PAGE_SIZE, item.list.size)
        }
        delay(500)
        job.cancelAndJoin()
        assertEquals(1, pagingDataSource.loadCallCount)
    }

    @Test
    fun when_item_on_next_page_has_been_requested_at_least_one_time_then_page_is_loaded() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        // When
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)
        eventEmitter.onGetItem(1)
        eventEmitter.onGetItem(SECOND_PAGE_SECOND_ITEM_INDEX)
        eventEmitter.onGetItem(1)
        eventEmitter.onGetItem(2)

        // Then
        pager.dataFlow.test {
            val item = awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 2, item.list.size)
        }
        job.cancelAndJoin()
        assertEquals(2, pagingDataSource.loadCallCount)
    }

    @Test
    fun on_error_then_error_state_is_exposed() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()

        val testThrowable = Throwable("Fake Error")
        pagingDataSource.error = testThrowable

        val pager = Pager(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        // When
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)

        // Then
        pager.dataFlow.test {
            val item = awaitForItem { it?.state is PagingData.LoadState.Error }
            assertEquals(testThrowable, (item.state as PagingData.LoadState.Error).error)
        }
        job.cancel()
    }

    @Test
    fun when_item_on_second_page_is_requested_then_second_page_is_loaded() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        // When
        eventEmitter.onGetItem(SECOND_PAGE_SECOND_ITEM_INDEX)

        // Then
        pager.dataFlow.test {
            val item = awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 2, item.list.size)
        }
        job.cancel()
    }

    @Test
    fun first_page_then_second_page_can_be_loaded() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        // When
        eventEmitter.onGetItem(1)

        // Then
        pager.dataFlow.test {
            val firstPage = awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.isNotEmpty() }
            assertEquals(PAGE_SIZE, firstPage.list.size)
            eventEmitter.onGetItem(SECOND_PAGE_SECOND_ITEM_INDEX)
            val firstAndSecondPage =
                awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 2, firstAndSecondPage.list.size)
        }
        job.cancel()
    }

    @Test
    fun requesting_item_at_loading_distance_of_the_second_page_should_trigger_second_page_load() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = PagingConfig(
                pagingConfig.pageSize,
                prefetchDistance = 1,
            ),
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        pager.dataFlow.test {
            // When
            eventEmitter.onGetItem(1)
            // Then
            val firstPage = awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.isNotEmpty() }
            assertEquals(PAGE_SIZE, firstPage.list.size)

            eventEmitter.onGetItem(FIRST_PAGE_LAST_ITEM_INDEX)
            val firstAndSecondPage =
                awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 2, firstAndSecondPage.list.size)
        }
        job.cancel()
    }

    @Test
    fun loading_a_page_should_emit_a_loading_state() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = PagingConfig(
                pagingConfig.pageSize,
                prefetchDistance = 1,
            ),
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        pager.dataFlow.test {
            // When
            eventEmitter.onGetItem(1)
            // Then
            val loadPage = awaitForItem { it?.state == PagingData.LoadState.Loading }
            assertIs<PagingData.LoadState.Loading>(loadPage.state)
            val firstPage = awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.isNotEmpty() }
            assertEquals(PAGE_SIZE, firstPage.list.size)

            eventEmitter.onGetItem(FIRST_PAGE_LAST_ITEM_INDEX)
            val firstAndSecondPage =
                awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 2, firstAndSecondPage.list.size)
        }
        job.cancel()
    }

    @Test
    fun should_stop_loading_at_the_end_of_the_list() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        pagingDataSource.endIndex = THIRD_PAGE_KEY
        val pager = Pager(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = PagingConfig(
                pagingConfig.pageSize,
                prefetchDistance = 1,
            ),
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        pager.dataFlow.test {
            // When
            eventEmitter.onGetItem(FIRST_ITEM_INDEX)

            // Then
            val firstPage = awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.isNotEmpty() }
            assertEquals(PAGE_SIZE, firstPage.list.size)

            eventEmitter.onGetItem(FIRST_PAGE_LAST_ITEM_INDEX)
            val firstAndSecondPage =
                awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 2, firstAndSecondPage.list.size)

            eventEmitter.onGetItem(SECOND_PAGE_LAST_ITEM_INDEX)
            val threePages =
                awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.size > 2 * PAGE_SIZE }
            assertEquals(PAGE_SIZE * 3, threePages.list.size)
            eventEmitter.onGetItem(35)
            eventEmitter.stop()
        }
        job.cancel()
    }

    @Test
    fun error_on_the_first_page_is_exposed() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }
        pagingDataSource.errorsForKeys.put(INITIAL_KEY, Throwable("Error for page 0"))

        // When
        eventEmitter.onGetItem(SECOND_PAGE_SECOND_ITEM_INDEX)

        // Then
        pager.dataFlow.test {
            val item = awaitForItem { it?.state is PagingData.LoadState.Error }
            assertEquals(0, item.list.size)
            assertIs<PagingData.LoadState.Error>(item.state)
            assertEquals("Error for page 0", (item.state as PagingData.LoadState.Error).error.message)
        }
        job.cancel()
        job.join()
    }

    @Test
    fun error_on_the_second_page_is_exposed() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager<Int, String>(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }
        pagingDataSource.errorsForKeys.put(SECOND_PAGE_KEY, Throwable("Error for page 1"))

        // When
        eventEmitter.onGetItem(SECOND_PAGE_SECOND_ITEM_INDEX)

        // Then
        pager.dataFlow.test {
            val item = awaitForItem { it?.state is PagingData.LoadState.Error }
            assertEquals(PAGE_SIZE, item.list.size)
            assertIs<PagingData.LoadState.Error>(item.state)
            assertEquals("Error for page 1", (item.state as PagingData.LoadState.Error).error.message)
        }
        job.cancel()
    }

    @Test
    fun when_item_index_on_third_page_is_requested_then_third_page_is_loaded() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager<Int, String>(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        // When
        eventEmitter.onGetItem(THIRD_PAGE_SECOND_ITEM_INDEX)

        // Then
        pager.dataFlow.test {
            val item = awaitForItem { it?.state == PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 3, item.list.size)
        }
        job.cancel()
    }

    @Test
    fun error_on_first_page_can_be_retried() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager<Int, String>(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }
        pagingDataSource.errorsForKeys.put(INITIAL_KEY, Throwable("Error on page 0"))
        // When
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)

        pager.dataFlow.test {
            val item = awaitForItem { it?.state is PagingData.LoadState.Error }
            assertEquals("Error on page 0", (item.state as PagingData.LoadState.Error).error.message)

            // Then
            // Dismiss error and retry
            pagingDataSource.errorsForKeys.clear()
            eventEmitter.retry()

            val firstPageSuccess = awaitForItem { it?.state is PagingData.LoadState.NotLoading && it.list.isNotEmpty() }
            assertEquals(PAGE_SIZE, firstPageSuccess.list.size)
        }
        job.cancel()
    }

    @Test
    fun error_on_second_page_can_be_retried() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager<Int, String>(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }
        pagingDataSource.errorsForKeys.put(SECOND_PAGE_KEY, Throwable("Error on page 1"))

        // When
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)

        pager.dataFlow.test {
            val firstPage = awaitForItem { it?.list?.isNotEmpty() ?: false }

            eventEmitter.onGetItem(SECOND_PAGE_SECOND_ITEM_INDEX)
            val errorPage = awaitForItem { it?.state is PagingData.LoadState.Error }

            // Then
            // Dismiss error and retry
            pagingDataSource.errorsForKeys.clear()
            eventEmitter.retry()

            val secondPage = awaitForItem { it?.state is PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 2, secondPage.list.size)
        }
        job.cancel()
    }

    @Test
    fun pager_subscription_can_be_stopped() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager<Int, String>(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        // When
        eventEmitter.stop()

        // Then

        job.join()
        // TODO: Assert does not throw
    }

    @Test
    fun pager_subscription_can_be_resumed() = runTest {
        // Given
        val eventEmitter = PagingEventEmitter()
        val pagingDataSource = MockPagingDataSource()
        val pager = Pager<Int, String>(
            source = pagingDataSource,
            initialKey = INITIAL_KEY,
            scope = this,
            config = pagingConfig,
        )
        val job = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }
        eventEmitter.onGetItem(FIRST_ITEM_INDEX)
        // When
        eventEmitter.stop()
        // Then
        // TODO: Assert does not throw
        job.join()

        val job2 = launch {
            pager.subscribeToViewEvents(eventEmitter.eventFlow)
        }

        pager.dataFlow.test {
            awaitForItem { it?.state is PagingData.LoadState.NotLoading }
            eventEmitter.onGetItem(SECOND_PAGE_SECOND_ITEM_INDEX)
            val secondPage = awaitForItem { it?.state is PagingData.LoadState.NotLoading && it.list.size > PAGE_SIZE }
            assertEquals(PAGE_SIZE * 2, secondPage.list.size)
            eventEmitter.stop()
        }
        // TODO: Assert does not throw
        job2.join()
    }
}
