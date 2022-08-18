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

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.event.EventAdmin;

/**
 * This is a wrapper around {@link MockResourceResolver} to act as resource provider.
 * All resources returned by this provider return the resolver from the resolve context instead of the {@link MockResourceResolver}.
 */
@Component(service = ResourceProvider.class, property = {
        ResourceProvider.PROPERTY_NAME + "=MockResourceProvider",
        ResourceProvider.PROPERTY_ROOT + "=/",
        ResourceProvider.PROPERTY_MODIFIABLE + ":Boolean=true",
        ResourceProvider.PROPERTY_ADAPTABLE + ":Boolean=true",
        // although we do not really support authentication, it's required for a modifiable resource provider
        ResourceProvider.PROPERTY_AUTHENTICATE + "=" + ResourceProvider.AUTHENTICATE_REQUIRED
})
public final class MockResourceProvider extends ResourceProvider<Void> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private EventAdmin eventAdmin;

    private MockResourceResolver mockResourceResolver;
    private MockQueryLanguageProvider mockQueryLanguageProvider;

    @Activate
    private void activate() {
        MockResourceResolverFactoryOptions options = new MockResourceResolverFactoryOptions();
        options.setMangleNamespacePrefixes(true);
        options.setEventAdmin(eventAdmin);
        ResourceResolverFactory resourceResolverFactory = new MockResourceResolverFactory(options);
        try {
            this.mockResourceResolver = (MockResourceResolver)resourceResolverFactory.getResourceResolver(null);
        }
        catch (LoginException ex) {
            throw new RuntimeException(ex);
        }
        this.mockQueryLanguageProvider = new MockQueryLanguageProvider(mockResourceResolver);
    }

    @Override
    public @Nullable Resource getResource(@NotNull ResolveContext<Void> ctx,
            @NotNull String path, @NotNull ResourceContext resourceContext,
            @Nullable Resource parent) {
        Resource resource = mockResourceResolver.getResource(path);
        if (resource != null) {
            return attachResource(ctx, resource);
        }
        else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("null")
    public @Nullable Iterator<Resource> listChildren(
            @NotNull ResolveContext<Void> ctx, @NotNull Resource parent) {
        Iterator<Resource> children = mockResourceResolver.listChildren(parent);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(children, Spliterator.ORDERED), false)
                .map(resource -> attachResource(ctx, resource))
                .iterator();
    }

    @Override
    public @NotNull Resource create(@NotNull ResolveContext<Void> ctx, String path,
            Map<String, Object> properties) throws PersistenceException {
        String parentPath = ResourceUtil.getParent(path);
        String name = ResourceUtil.getName(path);
        if (parentPath == null) {
            throw new PersistenceException("Invalid path: " + path);
        }
        Resource parent = mockResourceResolver.getResource(parentPath);
        if (parent == null) {
            throw new PersistenceException("Parent does not exist: " + parentPath);
        }
        Resource newResource = mockResourceResolver.create(parent, name, properties);
        return attachResource(ctx, newResource);
    }

    @Override
    public void delete(@NotNull ResolveContext<Void> ctx, @NotNull Resource resource) throws PersistenceException {
        mockResourceResolver.delete(resource);
    }

    @Override
    public void revert(@NotNull ResolveContext<Void> ctx) {
        mockResourceResolver.revert();
    }

    @Override
    public void commit(@NotNull ResolveContext<Void> ctx) throws PersistenceException {
        mockResourceResolver.commit();
    }

    @Override
    public boolean hasChanges(@NotNull ResolveContext<Void> ctx) {
        return mockResourceResolver.hasChanges();
    }

    @Override
    public @Nullable QueryLanguageProvider<Void> getQueryLanguageProvider() {
        return mockQueryLanguageProvider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <AdapterType> @Nullable AdapterType adaptTo(@NotNull ResolveContext<Void> ctx, @NotNull Class<AdapterType> type) {
        if (type == MockResourceResolver.class) {
            return (AdapterType)mockResourceResolver;
        }
        return super.adaptTo(ctx, type);
    }

    private @NotNull Resource attachResource(@NotNull ResolveContext<Void> ctx, @NotNull Resource resource) {
        if (resource instanceof MockResource) {
            return ((MockResource)resource).forResourceProvider(ctx.getResourceResolver());
        }
        else if (resource instanceof MockPropertyResource) {
            return ((MockPropertyResource)resource).forResourceProvider(ctx.getResourceResolver());
        }
        else {
            return resource;
        }
    }

}
