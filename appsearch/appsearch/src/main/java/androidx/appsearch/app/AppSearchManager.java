/*
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

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.List;

/**
 * This class provides access to the centralized AppSearch index maintained by the system.
 *
 * <p>Apps can index structured text documents with AppSearch, which can then be retrieved through
 * the query API.
 */
// TODO(b/148046169): This class header needs a detailed example/tutorial.
public class AppSearchManager {
    /** The default empty database name.*/
    public static final String DEFAULT_DATABASE_NAME = "";

    private final String mDatabaseName;
    private final AppSearchBackend mBackend;

    /** Builder class for {@link AppSearchManager} objects. */
    public static final class Builder {
        private AppSearchBackend mBackend;
        private String mDatabaseName = DEFAULT_DATABASE_NAME;
        private boolean mBuilt = false;

        /**
         * Sets the name of the database to create or open.
         *
         * <p>Databases with different names are fully separate with distinct types, namespaces, and
         * data.
         *
         * <p>Database name cannot contain {@code '/'}.
         *
         * <p>If not specified, defaults to {@link #DEFAULT_DATABASE_NAME}.
         * @param databaseName The name of the database.
         * @throws IllegalArgumentException if the databaseName contains {@code '/'}.
         */
        @NonNull
        public Builder setDatabaseName(@NonNull String databaseName) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(databaseName);
            if (databaseName.contains("/")) {
                throw new IllegalArgumentException("Database name cannot contain '/'");
            }
            mDatabaseName = databaseName;
            return this;
        }

        /**
         * Sets the backend where this {@link AppSearchManager} will store its data.
         * @hide
         */
        @NonNull
        public Builder setBackend(@NonNull AppSearchBackend backend) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(backend);
            mBackend = backend;
            return this;
        }

        /**
         * Asynchronously connects to the AppSearch backend and returns the initialized instance.
         */
        @NonNull
        public ListenableFuture<AppSearchResult<AppSearchManager>> build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkState(mBackend != null, "setBackend() has never been called");
            mBuilt = true;
            ResolvableFuture<AppSearchResult<AppSearchManager>> result = ResolvableFuture.create();
            result.set(AppSearchResult.newSuccessfulResult(
                    new AppSearchManager(mDatabaseName, mBackend)));
            return result;
        }
    }

    AppSearchManager(@NonNull String databaseName, @NonNull AppSearchBackend backend) {
        mDatabaseName = databaseName;
        mBackend = backend;
    }

    /**
     * Sets the schema being used by documents provided to the {@link #putDocuments} method.
     *
     * <p>The schema provided here is compared to the stored copy of the schema previously supplied
     * to {@link #setSchema}, if any, to determine how to treat existing documents. The following
     * types of schema modifications are always safe and are made without deleting any existing
     * documents:
     * <ul>
     *     <li>Addition of new types
     *     <li>Addition of new
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_OPTIONAL OPTIONAL} or
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_REPEATED REPEATED} properties to a
     *         type
     *     <li>Changing the cardinality of a data type to be less restrictive (e.g. changing an
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_OPTIONAL OPTIONAL} property into a
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_REPEATED REPEATED} property.
     * </ul>
     *
     * <p>The following types of schema changes are not backwards-compatible:
     * <ul>
     *     <li>Removal of an existing type
     *     <li>Removal of a property from a type
     *     <li>Changing the data type ({@code boolean}, {@code long}, etc.) of an existing property
     *     <li>For properties of {@code Document} type, changing the schema type of
     *         {@code Document}s of that property
     *     <li>Changing the cardinality of a data type to be more restrictive (e.g. changing an
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_OPTIONAL OPTIONAL} property into a
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_REQUIRED REQUIRED} property).
     *     <li>Adding a
     *         {@link AppSearchSchema.PropertyConfig#CARDINALITY_REQUIRED REQUIRED} property.
     * </ul>
     * <p>Supplying a schema with such changes will, by default, result in this call returning an
     * {@link AppSearchResult} with a code of {@link AppSearchResult#RESULT_INVALID_SCHEMA} and an
     * error message describing the incompatibility. In this case the previously set schema will
     * remain active.
     *
     * <p>If you need to make non-backwards-compatible changes as described above, you can set the
     * {@link SetSchemaRequest.Builder#setForceOverride} method to {@code true}. In this case,
     * instead of returning an {@link AppSearchResult} with the
     * {@link AppSearchResult#RESULT_INVALID_SCHEMA} error code, all documents which are not
     * compatible with the new schema will be deleted and the incompatible schema will be applied.
     *
     * <p>It is a no-op to set the same schema as has been previously set; this is handled
     * efficiently.
     *
     * @param request The schema update request.
     * @return The pending result of performing this operation.
     */
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> setSchema(@NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);
        return mBackend.setSchema(mDatabaseName, request);
    }

    /**
     * Indexes documents into AppSearch.
     *
     * <p>Each {@link GenericDocument}'s {@code schemaType} field must be set to the name of a
     * schema type previously registered via the {@link #setSchema} method.
     *
     * @param request {@link PutDocumentsRequest} containing documents to be indexed
     * @return The pending result of performing this operation. The keys of the returned
     * {@link AppSearchBatchResult} are the URIs of the input documents. The values are
     * {@code null} if they were successfully indexed, or a failed {@link AppSearchResult}
     * otherwise.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        return mBackend.putDocuments(mDatabaseName, request);
    }

    /**
     * Retrieves {@link GenericDocument}s by URI.
     *
     * @param request {@link GetByUriRequest} containing URIs to be retrieved.
     * @return The pending result of performing this operation. The keys of the returned
     * {@link AppSearchBatchResult} are the input URIs. The values are the returned
     * {@link GenericDocument}s on success, or a failed {@link AppSearchResult} otherwise.
     * URIs that are not found will return a failed {@link AppSearchResult} with a result code
     * of {@link AppSearchResult#RESULT_NOT_FOUND}.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByUri(
            @NonNull GetByUriRequest request) {
        Preconditions.checkNotNull(request);
        return mBackend.getByUri(mDatabaseName, request);
    }

    /**
     * Searches a document based on a given query string.
     *
     * <p>Currently we support following features in the raw query format:
     * <ul>
     *     <li>AND
     *     <p>AND joins (e.g. “match documents that have both the terms ‘dog’ and
     *     ‘cat’”).
     *     Example: hello world matches documents that have both ‘hello’ and ‘world’
     *     <li>OR
     *     <p>OR joins (e.g. “match documents that have either the term ‘dog’ or
     *     ‘cat’”).
     *     Example: dog OR puppy
     *     <li>Exclusion
     *     <p>Exclude a term (e.g. “match documents that do
     *     not have the term ‘dog’”).
     *     Example: -dog excludes the term ‘dog’
     *     <li>Grouping terms
     *     <p>Allow for conceptual grouping of subqueries to enable hierarchical structures (e.g.
     *     “match documents that have either ‘dog’ or ‘puppy’, and either ‘cat’ or ‘kitten’”).
     *     Example: (dog puppy) (cat kitten) two one group containing two terms.
     *     <li>Property restricts
     *     <p> Specifies which properties of a document to specifically match terms in (e.g.
     *     “match documents where the ‘subject’ property contains ‘important’”).
     *     Example: subject:important matches documents with the term ‘important’ in the
     *     ‘subject’ property
     *     <li>Schema type restricts
     *     <p>This is similar to property restricts, but allows for restricts on top-level document
     *     fields, such as schema_type. Clients should be able to limit their query to documents of
     *     a certain schema_type (e.g. “match documents that are of the ‘Email’ schema_type”).
     *     Example: { schema_type_filters: “Email”, “Video”,query: “dog” } will match documents
     *     that contain the query term ‘dog’ and are of either the ‘Email’ schema type or the
     *     ‘Video’ schema type.
     * </ul>
     *
     * <p> This method is lightweight. The heavy work will be done in
     *     {@link SearchResults#getNextPage()}.
     *
     * @param queryExpression Query String to search.
     * @param searchSpec      Spec for setting filters, raw query etc.
     * @return The search result of performing this operation.
     * @hide
     */
    @NonNull
    public SearchResults query(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        AppSearchBackend.BackendSearchResults backendSearchResults =
                mBackend.query(mDatabaseName, queryExpression, searchSpec);
        return new SearchResults(backendSearchResults);
    }

    /**
     * Removes {@link GenericDocument}s from the index by URI.
     *
     * @param request Request containing URIs to be removed.
     * @return The pending result of performing this operation. The keys of the returned
     * {@link AppSearchBatchResult} are the input URIs. The values are {@code null} on success,
     * or a failed {@link AppSearchResult} otherwise. URIs that are not found will return a
     * failed {@link AppSearchResult} with a result code of
     * {@link AppSearchResult#RESULT_NOT_FOUND}.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByUri(
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(request);
        return mBackend.removeByUri(mDatabaseName, request);
    }

    /**
     * Removes {@link GenericDocument}s from the index by schema type.
     *
     * @param schemaTypes Schema types whose documents to delete.
     * @return The pending result of performing this operation. The keys of the returned
     * {@link AppSearchBatchResult} are the input schema types. The values are {@code null} on
     * success, or a failed {@link AppSearchResult} otherwise. Types that are not found will
     * return a failed {@link AppSearchResult} with a result code of
     * {@link AppSearchResult#RESULT_NOT_FOUND}.
     * @hide
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByType(
            @NonNull String... schemaTypes) {
        Preconditions.checkNotNull(schemaTypes);
        return removeByType(Arrays.asList(schemaTypes));
    }

    /**
     * Removes {@link GenericDocument}s from the index by schema type.
     *
     * @param schemaTypes Schema types whose documents to delete.
     * @return The pending result of performing this operation. The keys of the returned
     * {@link AppSearchBatchResult} are the input schema types. The values are {@code null} on
     * success, or a failed {@link AppSearchResult} otherwise. Types that are not found will
     * return a failed {@link AppSearchResult} with a result code of
     * {@link AppSearchResult#RESULT_NOT_FOUND}.
     * @hide
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByType(
            @NonNull List<String> schemaTypes) {
        Preconditions.checkNotNull(schemaTypes);
        return mBackend.removeByType(mDatabaseName, schemaTypes);
    }

    /**
     * Removes {@link GenericDocument}s from the index by namespace.
     *
     * @param namespaces Namespaces whose documents to delete.
     * @return The pending result of performing this operation. The keys of the returned
     * {@link AppSearchBatchResult} are the input namespaces. The values are {@code null} on
     * success, or a failed {@link AppSearchResult} otherwise. Namespaces that are not found
     * will return a failed {@link AppSearchResult} with a result code of
     * {@link AppSearchResult#RESULT_NOT_FOUND}.
     * @hide
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByNamespace(
            @NonNull String... namespaces) {
        Preconditions.checkNotNull(namespaces);
        return removeByNamespace(Arrays.asList(namespaces));
    }

    /**
     * Removes {@link GenericDocument}s from the index by namespace.
     *
     * @param namespaces Namespaces whose documents to delete.
     * @return The pending result of performing this operation. The keys of the returned
     * {@link AppSearchBatchResult} are the input namespaces. The values are {@code null} on
     * success, or a failed {@link AppSearchResult} otherwise. Namespaces that are not found
     * will return a failed {@link AppSearchResult} with a result code of
     * {@link AppSearchResult#RESULT_NOT_FOUND}.
     * @hide
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByNamespace(
            @NonNull List<String> namespaces) {
        Preconditions.checkNotNull(namespaces);
        return mBackend.removeByNamespace(mDatabaseName, namespaces);
    }

    /**
     * Removes all documents owned by this instance.
     *
     * <p>The schemas will remain. To clear everything including schemas, please call
     * {@link #setSchema} with an empty schema and {@code forceOverride} set to true.
     *
     * @return The pending result of performing this operation.
     * @hide
     */
    @NonNull
    public ListenableFuture<AppSearchResult<Void>> removeAll() {
        return mBackend.removeAll(mDatabaseName);
    }
}
