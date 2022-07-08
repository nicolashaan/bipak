/*
 * Copyright 2022 Nicolas Haan
 * (forked from https://github.com/androidx/androidx/blob/androidx-main/paging/paging-common/src/main/kotlin/androidx/paging/PagingConfig.kt)
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.haan.bipak

import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * An object used to configure loading behavior within a [Pager], as it loads content from a
 * [PagingSource].
 */
public class PagingConfig @JvmOverloads public constructor(
    /**
     * Defines the number of items loaded at once from the [PagingSource].
     *
     * Should be several times the number of visible items onscreen.
     *
     * Configuring your page size depends on how your data is being loaded and used. Smaller
     * page sizes improve memory usage, latency, and avoid GC churn. Larger pages generally
     * improve loading throughput, to a point (avoid loading more than 2MB from SQLite at
     * once, since it incurs extra cost).
     *
     * If you're loading data for very large, social-media style cards that take up most of
     * a screen, and your database isn't a bottleneck, 10-20 may make sense. If you're
     * displaying dozens of items in a tiled grid, which can present items during a scroll
     * much more quickly, consider closer to 100.
     *
     * Note: [pageSize] is used to inform [PagingSource.LoadParams.loadSize], but is not enforced.
     * A [PagingSource] may completely ignore this value and still return a valid
     * [Page][PagingSource.LoadResult.Page].
     */
    @JvmField
    public val pageSize: Int = 10,

    /**
     * Prefetch distance which defines how far from the edge of loaded content an access must be to
     * trigger further loading. Typically should be set several times the number of visible items
     * onscreen.
     *
     * E.g., If this value is set to 50, a [PagingData] will attempt to load 50 items in advance of
     * data that's already been accessed.
     *
     * A value of 0 indicates that no list items will be loaded until they are specifically
     * requested. This is generally not recommended, so that users don't observe a
     *  end of list while scrolling.
     */
    @JvmField
    public val prefetchDistance: Int = pageSize / 2,

) {
    init {
        if (pageSize <= 0) throw IllegalArgumentException("Page size must be strictly positive!")
        if (prefetchDistance < 0) throw IllegalArgumentException("Prefetch distance must be positive!")
    }
}
