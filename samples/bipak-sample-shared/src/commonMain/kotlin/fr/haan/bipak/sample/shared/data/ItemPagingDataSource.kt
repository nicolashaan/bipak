package fr.haan.bipak.sample.shared.data

import fr.haan.bipak.PagingDataSource
import fr.haan.bipak.sample.android.data.DataModel
import fr.haan.bipak.sample.android.data.ItemDataSource
import fr.haan.bipak.sample.android.domain.DomainItem

class ItemPagingDataSource(private val dataSource: ItemDataSource) : PagingDataSource<Int, DomainItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DomainItem> {

        try {
            // Get data from data source
            val data = dataSource.getData(params.key ?: 0, params.loadSize)
            val retData = data.items.map { it.toDomain() }

            // Return data and metadata
            return LoadResult.Page(
                data = retData,
                prevKey = null,
                nextKey = data.metadata.nextPage,
                totalCount = data.metadata.totalCount,
            )
        } catch (error: Throwable) {
            // Report the error if any
            return LoadResult.Error(error)
        }
    }
}

private fun DataModel.Item.toDomain(): DomainItem {
    return DomainItem(
        id = id,
        content = content
    )
}
