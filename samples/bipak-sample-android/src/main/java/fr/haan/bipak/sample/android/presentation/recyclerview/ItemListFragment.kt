package fr.haan.bipak.sample.android.presentation.recyclerview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import fr.haan.bipak.sample.android.presentation.ON_SETTING_CHANGED
import fr.haan.bipak.sample.android.presentation.UiModel
import fr.haan.bipak.sample.android.presentation.compose.ItemListComposeFragment
import fr.haan.bipak.sample.android.presentation.settings.PagingSourceSettingsFragment
import fr.haan.bipak.sample.android.recyclerview.R
import fr.haan.bipak.sample.android.recyclerview.databinding.FragmentItemListBinding
import kotlinx.coroutines.flow.collectLatest

/**
 * A fragment representing a list of Items.
 */
class ItemListFragment : Fragment() {

    val viewModel: ItemListViewModel by viewModels()

    lateinit var binding: FragmentItemListBinding

    private lateinit var adapter: UiModelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentItemListBinding.inflate(inflater, container, false)

        adapter = UiModelAdapter()
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = this.adapter

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_goto_setup) {
                onSettingClicked()
                return@setOnMenuItemClickListener true
            } else if (it.itemId == R.id.action_goto_compose) {
                onComposeClicked()
            }
            return@setOnMenuItemClickListener false
        }

        setFragmentResultListener(ON_SETTING_CHANGED) { requestKey, bundle ->
            viewModel.resetRepository()
            setupRepository()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupRepository()
    }

    override fun onPause() {
        adapter.stop()
        super.onPause()
    }

    private fun onSettingClicked() {
        val fragment = PagingSourceSettingsFragment()
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(PagingSourceSettingsFragment::class.simpleName)
            .commit()
    }

    private fun onComposeClicked() {
        val fragment = ItemListComposeFragment()
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setupRepository() {

        lifecycleScope.launchWhenResumed {
            viewModel.setViewEventFlow(adapter.eventFlow)
        }

        lifecycleScope.launchWhenResumed {
            viewModel.listContentFlow.collectLatest {
                val otherItems = it.filter { it !is UiModel.Item }.size
                adapter.submitList(it, otherItems)
            }
        }

        adapter.start()
    }
}
