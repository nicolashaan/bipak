package fr.haan.bipak.sample.shared

import fr.haan.bipak.sample.android.data.ItemDataSource
import fr.haan.bipak.sample.android.domain.ItemRepository

/**
 * For the sake of simplicity, we use this ugly Global service locator as DI and parameter store
 */
object GlobalServiceLocator {
    var currentItemCount = 1000
    var currentPageSize = 10
    var simulatedLoadDelayMs = 1000L

    var errorOnPage: Int? = null

    fun createRepository(): ItemRepository {
        val dataSource = ItemDataSource(currentItemCount)

        val repository = ItemRepository(dataSource, currentPageSize)
        return repository
    }
}
