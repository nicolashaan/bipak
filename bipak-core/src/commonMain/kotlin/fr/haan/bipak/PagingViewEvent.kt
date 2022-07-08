package fr.haan.bipak

/**
 * Events going from UI layer to [Pager] allowing to fetch data
 */
public sealed class PagingViewEvent {
    /**
     * Neutral event
     */
    public object Idle : PagingViewEvent()

    /**
     * The element is currently being displayed
     * @param index index of element
     */
    public class ElementRequested(public val index: Int) : PagingViewEvent()

    /**
     * Retry last operation
     */
    public object Retry : PagingViewEvent()

    /**
     * Stop reacting to events and unsubscribe from source
     */
    public object Terminate : PagingViewEvent()
}
