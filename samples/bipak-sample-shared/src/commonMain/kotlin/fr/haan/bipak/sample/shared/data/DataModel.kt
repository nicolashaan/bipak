package fr.haan.bipak.sample.android.data

data class DataModel(
    val items: List<Item>,
    val metadata: Metadata
) {
    data class Item(
        val id: Int,
        val content: String,
    )

    data class Metadata(
        val pageIndex: Int,
        val totalPages: Int,
        val nextPage: Int?,
        val totalCount: Int,

    )
}
