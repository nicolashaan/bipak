package fr.haan.bipak.sample.android.presentation

sealed interface UiModel {
    data class Item(
        val id: Int,
        val content: String,
    ) : UiModel

    object Loading : UiModel
    data class Error(val error: Throwable) : UiModel
}
