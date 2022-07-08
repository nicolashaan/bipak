package fr.haan.bipak.sample.android.presentation.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import fr.haan.bipak.android.PagingDataAdapter
import fr.haan.bipak.sample.android.presentation.UiModel
import fr.haan.bipak.sample.android.recyclerview.databinding.ItemContentBinding
import fr.haan.bipak.sample.android.recyclerview.databinding.ItemErrorBinding
import fr.haan.bipak.sample.android.recyclerview.databinding.ItemLoadingBinding

class UiModelAdapter() : PagingDataAdapter<UiModel, UiModelAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<UiModel>() {
        override fun areItemsTheSame(oldItem: UiModel, newItem: UiModel): Boolean {
            if (oldItem is UiModel.Item && newItem is UiModel.Item) {
                return oldItem.id == newItem.id
            }
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: UiModel, newItem: UiModel): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = when (viewType) {
            UiModel.Error::class.hashCode() ->
                ItemErrorBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)

            UiModel.Loading::class.hashCode() ->
                ItemLoadingBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)

            UiModel.Item::class.hashCode() ->
                ItemContentBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)

            else -> throw java.lang.IllegalStateException("Unknown view type: $viewType")
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        when (item) {
            is UiModel.Error -> (holder.binding as ItemErrorBinding).apply {
                itemNumber.text = ""
                content.text = item.error.message.orEmpty()
                buttonRetry.setOnClickListener { retry() }
            }
            is UiModel.Item -> (holder.binding as ItemContentBinding).apply {
                itemNumber.text = "pos: $position"
                content.text = item.content
            }
            UiModel.Loading -> { /* noop */
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is UiModel.Error -> UiModel.Error::class.hashCode()
            is UiModel.Item -> UiModel.Item::class.hashCode()
            UiModel.Loading -> UiModel.Loading::class.hashCode()
        }
    }

    inner class ViewHolder(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
}
