package fr.haan.bipak.sample.android.data

import fr.haan.bipak.sample.shared.GlobalServiceLocator
import kotlinx.coroutines.delay

/**
 * Fake remote data source with tweakable parameters
 */
class ItemDataSource(private val totalCount: Int) {

    var errorOnPage: Int?
        get() = GlobalServiceLocator.errorOnPage
        set(value) {
            GlobalServiceLocator.errorOnPage = value
        }
    var loadDelayMs: Int = 0

    suspend fun getData(pageIndex: Int, pageSize: Int): DataModel {

        val endIndex = totalCount / pageSize
        val page = pageIndex

        val data = (0..(pageSize - 1)).map { page * pageSize + it }.map {
            DataModel.Item(
                id = pageIndex * 10_000 + it,
                content = "Page: $page, item $it",
            )
        }.toList()

        val nextKeyValue = (pageIndex) + 1

        val nextKey: Int?
        if (pageIndex >= endIndex) {
            nextKey = null
        } else {
            nextKey = nextKeyValue
        }

        delay(GlobalServiceLocator.simulatedLoadDelayMs)

        if (errorOnPage == pageIndex) {
            errorOnPage = null
            throw RuntimeException("Error in data source!")
        }

        return DataModel(
            items = data,
            metadata = DataModel.Metadata(
                pageIndex = pageIndex,
                totalPages = endIndex + 1,
                nextPage = nextKey,
                totalCount = totalCount,
            )
        )
    }
}
