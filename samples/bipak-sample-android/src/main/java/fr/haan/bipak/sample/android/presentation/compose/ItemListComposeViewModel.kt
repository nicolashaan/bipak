package fr.haan.bipak.sample.android.presentation.compose

import androidx.lifecycle.ViewModel
import fr.haan.bipak.PagingData
import fr.haan.bipak.PagingViewEvent
import fr.haan.bipak.sample.android.domain.DomainItem
import fr.haan.bipak.sample.shared.GlobalServiceLocator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class ItemListComposeViewModel : ViewModel() {

    private var repository = GlobalServiceLocator.createRepository()

    fun setViewEventFlow(eventFlow: MutableSharedFlow<PagingViewEvent>) {
        repository.setViewEventFlow(eventFlow)
    }

    val pagingDataFlow: Flow<PagingData<DomainItem>>
        get() {
            return repository.getItemFlow()
        }
}
