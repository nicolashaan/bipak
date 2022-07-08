package fr.haan.bipak.sample.android.presentation.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.haan.bipak.PagingData
import fr.haan.bipak.compose.LazyPagingItems
import fr.haan.bipak.compose.collectAsLazyPagingItems
import fr.haan.bipak.compose.itemsIndexed
import fr.haan.bipak.sample.android.domain.DomainItem
import fr.haan.bipak.sample.android.presentation.ON_SETTING_CHANGED
import fr.haan.bipak.sample.android.presentation.recyclerview.ItemListFragment
import fr.haan.bipak.sample.android.presentation.recyclerview.ItemListViewModel
import fr.haan.bipak.sample.android.presentation.settings.PagingSourceSettingsFragment
import fr.haan.bipak.sample.android.recyclerview.R
import fr.haan.bipak.sample.android.recyclerview.databinding.FragmentItemListComposeBinding

class ItemListComposeFragment : Fragment() {

    lateinit var binding: FragmentItemListComposeBinding
    val viewModel: ItemListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentItemListComposeBinding.inflate(inflater, container, false)

        setFragmentResultListener(ON_SETTING_CHANGED) { _, _ ->
            viewModel.resetRepository()
            setupComposeView()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComposeView()
    }

    fun setupComposeView() {
        binding.composeView.setContent {
            ItemListScreen(
                onViewsClick = {
                    val fragment = ItemListFragment()
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                },
                onSettingsClick = {
                    val fragment = PagingSourceSettingsFragment()
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(PagingSourceSettingsFragment::class.simpleName)
                        .commit()
                }
            )
        }
    }
}

@Composable
private fun ItemListScreen(
    onViewsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val viewModel: ItemListComposeViewModel = viewModel()

    val pagingData: LazyPagingItems<DomainItem> = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    viewModel.setViewEventFlow(pagingData.eventFlow)
    Column {
        TopAppBar(title = { Text("Compose Paging") }, actions = {
            IconButton(onClick = onViewsClick) {
                Text("Views")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Localized description")
            }
        })
        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            itemsIndexed(pagingDataState = pagingData, itemContent = { index, item ->
                item?.let { ListItem(index, it.content) }
            })
            when (val loadState = pagingData.loadState) {
                PagingData.LoadState.Loading -> {
                    item { LoadingListItem() }
                }

                is PagingData.LoadState.Error -> {
                    item { ErrorListItem(loadState.error.message.orEmpty()) { pagingData.retry() } }
                }

                PagingData.LoadState.NotLoading -> { /* noop */
                }
            }
        }
    }
}

@Composable
private fun ListItem(id: Int, content: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "pos: $id",
            fontSize = 16.sp,
        )
        Text(
            modifier = Modifier.padding(16.dp),
            text = content,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun LoadingListItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorListItem(
    errorMessage: String,
    onRetryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(48.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row {
            Text(errorMessage)
            Button(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = onRetryClick
            ) {
                Text("Retry")
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ItemListScreen({}, {})
}

@Preview
@Composable
private fun PreviewLoadingListItem() {
    LoadingListItem()
}

@Preview
@Composable
private fun ErrorLoadingListItem() {
    ErrorListItem("Error in data source!", {})
}
