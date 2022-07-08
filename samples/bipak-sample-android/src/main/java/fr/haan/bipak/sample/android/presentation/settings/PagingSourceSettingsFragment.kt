package fr.haan.bipak.sample.android.presentation.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import fr.haan.bipak.sample.android.presentation.ON_SETTING_CHANGED
import fr.haan.bipak.sample.android.recyclerview.databinding.FragmentPagingSourceSettingsBinding
import fr.haan.bipak.sample.shared.GlobalServiceLocator

/**
 * /!\ Quick&Dirty implementation with UI & Domain mixed :-/
 */
class PagingSourceSettingsFragment : Fragment() {
    lateinit var binding: FragmentPagingSourceSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPagingSourceSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.buttonApply.setOnClickListener {
            applyChanges()
            setFragmentResult(ON_SETTING_CHANGED, bundleOf())
            requireActivity().supportFragmentManager.popBackStack()
        }
        binding.seekBarItemCount.addOnChangeListener { _, _, _ -> syncUiAfterChange() }
        binding.seekBarPageSize.addOnChangeListener { _, _, _ -> syncUiAfterChange() }
        binding.seekBarLoadDelay.addOnChangeListener { _, _, _ -> syncUiAfterChange() }
        binding.inputError.addTextChangedListener(textWatcher)
    }

    override fun onResume() {
        super.onResume()
        syncModelToView()
    }

    private fun syncModelToView() {
        binding.seekBarItemCount.value = fr.haan.bipak.sample.shared.GlobalServiceLocator.currentItemCount.toFloat()

        binding.seekBarPageSize.value = fr.haan.bipak.sample.shared.GlobalServiceLocator.currentPageSize.toFloat()

        binding.seekBarLoadDelay.value = fr.haan.bipak.sample.shared.GlobalServiceLocator.simulatedLoadDelayMs.toFloat()

        binding.inputError.setText(fr.haan.bipak.sample.shared.GlobalServiceLocator.errorOnPage?.toString() ?: "")
        syncUiAfterChange()
    }

    private fun syncUiAfterChange() {
        binding.textItemCount.text = "Item count: ${binding.seekBarItemCount.value}"
        binding.textPageSize.text = "Page size: ${binding.seekBarPageSize.value}"
        binding.textLoadDelay.text = "Load delay: ${binding.seekBarLoadDelay.value}ms"

        if (binding.seekBarItemCount.value == fr.haan.bipak.sample.shared.GlobalServiceLocator.currentItemCount.toFloat() &&
            binding.seekBarPageSize.value == fr.haan.bipak.sample.shared.GlobalServiceLocator.currentPageSize.toFloat() &&
            binding.seekBarLoadDelay.value.toLong() == fr.haan.bipak.sample.shared.GlobalServiceLocator.simulatedLoadDelayMs &&
            fr.haan.bipak.sample.shared.GlobalServiceLocator.errorOnPage == binding.inputError.text.toString()
                .toIntOrNull()
        ) {
            binding.buttonApply.isEnabled = false
        } else {
            binding.buttonApply.isEnabled = true
        }
    }

    private fun applyChanges() {
        fr.haan.bipak.sample.shared.GlobalServiceLocator.currentItemCount = binding.seekBarItemCount.value.toInt()
        fr.haan.bipak.sample.shared.GlobalServiceLocator.currentPageSize = binding.seekBarPageSize.value.toInt()
        fr.haan.bipak.sample.shared.GlobalServiceLocator.simulatedLoadDelayMs = binding.seekBarLoadDelay.value.toLong()
        fr.haan.bipak.sample.shared.GlobalServiceLocator.errorOnPage = binding.inputError.text.toString().toIntOrNull()
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            syncUiAfterChange()
        }

        override fun afterTextChanged(p0: Editable?) {
        }
    }
}
