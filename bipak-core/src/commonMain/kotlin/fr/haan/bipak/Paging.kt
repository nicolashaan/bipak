package fr.haan.bipak

/**
* Returns a PagingData containing the results of applying the given transform function to each element in the original list.
 */
public fun <I : Any, O : Any> PagingData<I>.map(transform: (I) -> O): PagingData<O> {
    return PagingData(
        list = list.map { transform(it) },
        state = state,
        totalCount = totalCount,
    )
}

internal fun pagingDebugLog(str: String) {
    // Log.d("Paging", str)
    println(str)
}
