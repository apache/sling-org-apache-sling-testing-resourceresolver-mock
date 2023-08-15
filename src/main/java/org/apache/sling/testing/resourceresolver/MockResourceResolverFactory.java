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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.event.EventAdmin;

/**
 * Simple resource resolver factory
 */
public class MockResourceResolverFactory implements ResourceResolverFactory {

    private static final String ROOT_PRIMARY_TYPE="rep:root";

    /** We use a linked hash map to preserve creation order. */
    private final Map<String, Map<String, Object>> resources = new LinkedHashMap<String, Map<String, Object>>();

    private final MockResourceResolverFactoryOptions options;

    private final List<MockFindResourcesHandler> findResourcesHandlers = new ArrayList<>();
    private final List<MockQueryResourceHandler> queryResourcesHandlers = new ArrayList<>();

    /**
     * Create a new resource resolver factory
     * @param eventAdmin All resource events are sent to this event admin
     */
    public MockResourceResolverFactory(@Nullable final EventAdmin eventAdmin) {
        this(new MockResourceResolverFactoryOptions().setEventAdmin(eventAdmin));
    }

    /**
     * Create a new resource resolver factory.
     */
    public MockResourceResolverFactory() {
        this(new MockResourceResolverFactoryOptions());
    }

    /**
     * Create a new resource resolver factory.
     * @param options Options
     */
    public MockResourceResolverFactory(@NotNull final MockResourceResolverFactoryOptions options) {
        this.options = options;
        Map<String, Object> props= new HashMap<String,Object>();
        props.put(MockResource.JCR_PRIMARYTYPE, ROOT_PRIMARY_TYPE);
        resources.put("/", props);
    }

    @Override
    public @NotNull ResourceResolver getResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {

        // put user name in resolver attributes
        Map<String,Object> attributes = new HashMap<String, Object>();
        if (authenticationInfo!=null) {
            attributes.put(ResourceResolverFactory.USER, authenticationInfo.get(ResourceResolverFactory.USER));
        }

        final ResourceResolver result = addFindQueryResourceHandlers(new MockResourceResolver(options, this, resources, attributes));
        Stack<ResourceResolver> resolverStack = resolverStackHolder.get();
        if ( resolverStack == null ) {
            resolverStack = new Stack<>();
            resolverStackHolder.set(resolverStack);
        }
        resolverStack.push(result);
        return result;
    }

    @Override
    public @NotNull ResourceResolver getAdministrativeResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return addFindQueryResourceHandlers(new MockResourceResolver(options, this, resources));
    }

    @Override
    public @NotNull ResourceResolver getServiceResourceResolver(
            Map<String, Object> authenticationInfo) throws LoginException {
        return addFindQueryResourceHandlers(new MockResourceResolver(options, this, resources));
    }

    /**
     * Thread local holding the resource resolver stack
     */
    private ThreadLocal<Stack<ResourceResolver>> resolverStackHolder = new ThreadLocal<Stack<ResourceResolver>>();

    @Override
    public ResourceResolver getThreadResourceResolver() {
        ResourceResolver result = null;
        final Stack<ResourceResolver> resolverStack = resolverStackHolder.get();
        if ( resolverStack != null && !resolverStack.isEmpty() ) {
            result = resolverStack.peek();
        }
        return result;
    }

    /**
     * Inform about a closed resource resolver.
     * Make sure to remove it from the current thread context.
     * @param resolver Resource resolver
     */
    public void closed(@NotNull final ResourceResolver resolver) {
        final Stack<ResourceResolver> resolverStack = resolverStackHolder.get();
        if ( resolverStack != null ) {
            resolverStack.remove(resolver);
        }
    }

    // Sling API 2.24.0
    public @NotNull List<String> getSearchPath() {
        return Arrays.asList(this.options.getSearchPaths());
    }

    void addFindResourceHandlerInternal(@NotNull MockFindResourcesHandler handler) {
        findResourcesHandlers.add(handler);
    }

    void addQueryResourceHandlerInternal(@NotNull MockQueryResourceHandler handler) {
        queryResourcesHandlers.add(handler);
    }

    private MockResourceResolver addFindQueryResourceHandlers(@NotNull MockResourceResolver resourceResolver) {
        findResourcesHandlers.forEach(resourceResolver::addFindResourceHandlerInternal);
        queryResourcesHandlers.forEach(resourceResolver::addQueryResourceHandlerInternal);
        return resourceResolver;
    }

}
