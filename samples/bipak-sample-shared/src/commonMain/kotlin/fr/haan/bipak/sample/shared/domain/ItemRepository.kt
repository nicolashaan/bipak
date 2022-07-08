package fr.haan.bipak.sample.android.domain

import fr.haan.bipak.Pager
import fr.haan.bipak.PagingConfig
import fr.haan.bipak.PagingData
import fr.haan.bipak.PagingViewEvent
import fr.haan.bipak.sample.android.data.ItemDataSource
import fr.haan.bipak.sample.shared.data.ItemPagingDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class ItemRepository(dataSource: ItemDataSource, pageSize: Int) {

    // Running on Main thread as this is a demo
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val dataSoure = ItemPagingDataSource(dataSource,)

    private val pager = Pager(
        scope = coroutineScope,
        source = dataSoure,
        initialKey = 0,
        config = PagingConfig(
            pageSize = pageSize
        )
    )

    fun getItemFlow(): Flow<PagingData<DomainItem>> {
        return pager.dataFlow
    }

    fun setViewEventFlow(eventFlow: MutableSharedFlow<PagingViewEvent>) {
        coroutineScope.launch {
            pager.subscribeToViewEvents(eventFlow)
        }
    }
}
