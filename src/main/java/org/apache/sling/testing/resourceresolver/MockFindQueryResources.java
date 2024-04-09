/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.resourceresolver;

import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;

/**
 * Allows to provide mocked search result for queries via {@link ResourceResolver}.
 * This works if {@link MockResourceResolver} is used directly or indirectly via {@link MockResourceProvider}.
 */
public final class MockFindQueryResources {

    private MockFindQueryResources() {
        // static methods only
    }

    /**
     * Adds a handler that can provide a mocked find resources result. You can add multiple handlers which are called
     * in the order they were added when calling {@link ResourceResolver#findResources(String, String)}.
     * The result of the first handler that returns a non-null result is used.
     * If no handler delivers a result, an empty result is returned.
     * @param resourceResolver Resource resolver
     * @param handler Handler
     * @throws IllegalStateException If the given resource resolver is not based on resourceresolver-mock
     */
    public static void addFindResourceHandler(
            @NotNull ResourceResolver resourceResolver, @NotNull MockFindResourcesHandler handler) {
        toMockResourceResolver(resourceResolver).addFindResourceHandlerInternal(handler);
    }

    /**
     * Adds a handler that can provide a mocked query resources result. You can add multiple handlers which are called
     * in the order they were added when calling {@link ResourceResolver#queryResources(String, String)}.
     * The result of the first handler that returns a non-null result is used.
     * If no handler delivers a result, an empty result is returned.
     * @param resourceResolver Resource resolver
     * @param handler Handler
     * @throws IllegalStateException If the given resource resolver is not based on resourceresolver-mock
     */
    public static void addQueryResourceHandler(
            @NotNull ResourceResolver resourceResolver, @NotNull MockQueryResourceHandler handler) {
        toMockResourceResolver(resourceResolver).addQueryResourceHandlerInternal(handler);
    }

    private static @NotNull MockResourceResolver toMockResourceResolver(@NotNull ResourceResolver resourceResolver) {
        MockResourceResolver mockResourceResolver = null;
        if (resourceResolver instanceof MockResourceResolver) {
            mockResourceResolver = (MockResourceResolver) resourceResolver;
        } else {
            mockResourceResolver = resourceResolver.adaptTo(MockResourceResolver.class);
        }
        if (mockResourceResolver == null) {
            throw new IllegalStateException("The given resource resolver is not based on resourceresolver-mock.");
        }
        return mockResourceResolver;
    }
}
