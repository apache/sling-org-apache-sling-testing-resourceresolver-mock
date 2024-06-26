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

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;

/**
 * Resource that wraps a property value.
 */
class MockPropertyResource extends AbstractResource {

    private final String path;
    private final ValueMap props;
    private final String key;
    private final ResourceResolver resolver;
    private final ResourceMetadata rm;

    public MockPropertyResource(final String path, final ValueMap props, final ResourceResolver resolver) {
        this.path = path;
        this.props = props;
        this.key = ResourceUtil.getName(path);
        this.rm = new ResourceMetadata();
        this.resolver = resolver;
    }

    private MockPropertyResource(
            String path, ValueMap props, String key, ResourceMetadata rm, ResourceResolver resolver) {
        this.path = path;
        this.props = props;
        this.key = key;
        this.rm = rm;
        this.resolver = resolver;
    }

    @Override
    public @NotNull String getPath() {
        return this.path;
    }

    @Override
    @SuppressWarnings("null")
    public @NotNull String getResourceType() {
        // TODO: we should return a resource type here!
        return null;
    }

    @Override
    public String getResourceSuperType() {
        return null;
    }

    @Override
    public @NotNull ResourceMetadata getResourceMetadata() {
        return rm;
    }

    @Override
    public @NotNull ResourceResolver getResourceResolver() {
        return this.resolver;
    }

    @Override
    @SuppressWarnings("null")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        AdapterType value = props.get(key, type);
        if (value != null) {
            return value;
        }
        return super.adaptTo(type);
    }

    /**
     * Creates a new instance for this mock resource with referencing the given resoruce resolver (instead of the initial MockResourceResolver).
     * @param resourceResolver Resource resolver
     * @return Same resource with different resource resolver
     */
    Resource forResourceProvider(ResourceResolver resourceResolver) {
        return new MockPropertyResource(this.path, this.props, this.key, this.rm, resourceResolver);
    }
}
