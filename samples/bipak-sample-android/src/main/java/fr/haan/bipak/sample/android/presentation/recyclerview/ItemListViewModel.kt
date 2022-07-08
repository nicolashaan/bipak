package fr.haan.bipak.sample.android.presentation.recyclerview

import androidx.lifecycle.ViewModel
import fr.haan.bipak.PagingData
import fr.haan.bipak.PagingViewEvent
import fr.haan.bipak.sample.android.presentation.UiModel
import fr.haan.bipak.sample.shared.GlobalServiceLocator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

class ItemListViewModel : ViewModel() {

    private var repository = GlobalServiceLocator.createRepository()

    fun resetRepository() {
        repository = GlobalServiceLocator.createRepository()
    }

    fun setViewEventFlow(eventFlow: MutableSharedFlow<PagingViewEvent>) {
        repository.setViewEventFlow(eventFlow)
    }

    val listContentFlow: Flow<List<UiModel>>
        get() {
            return repository.getItemFlow()
                .map { pagingData ->
                    pagingData.list.map {
                        UiModel.Item(
                            id = it.id,
                            content = it.content
                        )
                    }.run {
                        // Add Error or Loading item to the UiModel list if needed
                        val bottomItem = when (val state = pagingData.state) {
                            is PagingData.LoadState.Error -> UiModel.Error(state.error)
                            PagingData.LoadState.Loading -> UiModel.Loading
                            PagingData.LoadState.NotLoading -> null
                        }
                        if (bottomItem != null) {
                            this + bottomItem
                        } else {
                            this
                        }
                    }
                }
        }
}
