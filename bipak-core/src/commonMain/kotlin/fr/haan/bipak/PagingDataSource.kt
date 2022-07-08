/*
 * Copyright 2022 Nicolas Haan
 * (forked from https://github.com/androidx/androidx/blob/androidx-main/paging/paging-common/src/main/kotlin/androidx/paging/PagingSource.kt)
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

/**
 * Base class for an abstraction of pageable static data from some source, where loading pages
 * of data is typically an expensive operation. Some examples of common [PagingDataSource]s might be
 * from network or from a database.
 *
 * An instance of a [PagingDataSource] is used to load pages of data for an instance of [Pager].
 */
public abstract class PagingDataSource<Key : Any, Value : Any> {

    /**
     * Parameters to use for a load request on a [PagingDataSource].
     */
    public sealed class LoadParams<Key : Any>(
        /**
         * Requested number of items to load.
         *
         * Note: It is valid for [PagingDataSource.load] to return a [LoadResult] that has a different
         * number of items than the requested load size.
         */
        public val loadSize: Int
    ) {
        /**
         * Key for the page to be loaded.
         */
        public abstract val key: Key

        /**
         * Params to load a page of data from a [PagingDataSource] via [PagingDataSource.load] to be
         * appended to the end of the list.
         */
        public class Append<Key : Any> constructor(
            override val key: Key,
            loadSize: Int,
        ) : LoadParams<Key>(
            loadSize = loadSize,
        )
    }

    public sealed class LoadResult<Key : Any, Value : Any> {

        public data class Error<Key : Any, Value : Any>(
            val throwable: Throwable
        ) : LoadResult<Key, Value>()

        /**
         * Success result object for [PagingSource.load].
         *
         * @param data Loaded data
         * @param prevKey [Key] for previous page if more data can be loaded in that direction,
         * `null` otherwise.
         * @param nextKey [Key] for next page if more data can be loaded in that direction,
         * `null` otherwise.
         */
        public data class Page<Key : Any, Value : Any> constructor(
            /**
             * Loaded data
             */
            val data: List<Value>,
            /**
             * [Key] for previous page if more data can be loaded in that direction, `null`
             * otherwise.
             */
            val prevKey: Key?,
            /**
             * [Key] for next page if more data can be loaded in that direction, `null` otherwise.
             */
            val nextKey: Key?,

            /**
             * The total item count (may include not fetched pages) if known, null otherwise
             */
            val totalCount: Int?,
        ) : LoadResult<Key, Value>()
    }

    public abstract suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value>
}
