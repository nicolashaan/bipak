package fr.haan.bipak

/**
 * Contain current paged data (in [list]) and current load state (in [state])
 */
public data class PagingData<Value : Any>(
    /**
     * The list of currently fetched data
     */
    public val list: List<Value>,
    /**
     * The current state of fetching operation
     */
    public val state: LoadState,
    /**
     * The total item count, if reported in [PagingDataSource.LoadResult.Page]
     */
    public val totalCount: Int?,
) {
    /**
     * Model of load state.
     */
    public sealed class LoadState {
        /**
         * No fetching operation in progress. No further data has been requested or the end of the list has
         * been reached
         */
        public object NotLoading : LoadState()

        /**
         * A page is currently being fetched. This value can be used in the UI layer to show a list loader
         */
        public object Loading : LoadState()

        /**
         * The last fetching operation has failed. his value can be used in the UI layer to show an error message.
         * Operation can be retried by using [PagingEventEmitter.retry]
         */
        public class Error(public val error: Throwable) : LoadState()
    }

    public companion object {
        /**
         * Return an empty [PagingData]. This can be used as an initial state to prevent using nullable value.
         */
        public fun <Value : Any> empty(): PagingData<Value> = PagingData(
            emptyList(),
            LoadState.NotLoading,
            0,
        )
    }
}
