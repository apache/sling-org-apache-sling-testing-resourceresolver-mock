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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
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
 */
@Component(service = ResourceProvider.class, property = {
        ResourceProvider.PROPERTY_NAME + "=MockResourceProvider",
        ResourceProvider.PROPERTY_ROOT + "=/",
        ResourceProvider.PROPERTY_MODIFIABLE + ":Boolean=true",
        // although we do not really support authentication, it's required for a modifiable resource provider
        ResourceProvider.PROPERTY_AUTHENTICATE + "=" + ResourceProvider.AUTHENTICATE_REQUIRED
})
public final class MockResourceProvider extends ResourceProvider<Void> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private EventAdmin eventAdmin;

    private ResourceResolver resourceResolver;

    @Activate
    private void activate() {
        MockResourceResolverFactoryOptions options = new MockResourceResolverFactoryOptions();
        options.setMangleNamespacePrefixes(true);
        options.setEventAdmin(eventAdmin);
        ResourceResolverFactory resourceResolverFactory = new MockResourceResolverFactory(options);
        try {
            this.resourceResolver = resourceResolverFactory.getResourceResolver(null);
        }
        catch (LoginException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public @Nullable Resource getResource(@NotNull ResolveContext<Void> ctx,
            @NotNull String path, @NotNull ResourceContext resourceContext,
            @Nullable Resource parent) {
        return resourceResolver.getResource(path);
    }

    @Override
    public @Nullable Iterator<Resource> listChildren(
            @NotNull ResolveContext<Void> ctx, @NotNull Resource parent) {
        return resourceResolver.listChildren(parent);
    }

    @Override
    public @NotNull Resource create(@NotNull ResolveContext<Void> ctx, String path,
            Map<String, Object> properties) throws PersistenceException {
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath == null) {
            throw new PersistenceException("Invalid path: " + path);
        }
        Resource parent = resourceResolver.getResource(parentPath);
        if (parent == null) {
            throw new PersistenceException("Parent does not exist: " + parentPath);
        }
        return resourceResolver.create(parent, path, properties);
    }

    @Override
    public void delete(@NotNull ResolveContext<Void> ctx, @NotNull Resource resource) throws PersistenceException {
        resourceResolver.delete(resource);
    }

    @Override
    public void revert(@NotNull ResolveContext<Void> ctx) {
        resourceResolver.revert();
    }

    @Override
    public void commit(@NotNull ResolveContext<Void> ctx) throws PersistenceException {
        resourceResolver.commit();
    }

    @Override
    public boolean hasChanges(@NotNull ResolveContext<Void> ctx) {
        return resourceResolver.hasChanges();
    }

}
