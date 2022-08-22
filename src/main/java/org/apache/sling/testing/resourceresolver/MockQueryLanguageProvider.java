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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.jetbrains.annotations.NotNull;

class MockQueryLanguageProvider implements QueryLanguageProvider<Void> {

    private final MockResourceResolver mockResourceResolver;

    private static final String[] SUPPORTED_LANGUAGES = {
            "xpath",
            "sql",
            "JCR-SQL2",
            "JCR-JQOM"
    };

    MockQueryLanguageProvider(MockResourceResolver mockResourceResolver) {
        this.mockResourceResolver = mockResourceResolver;
    }

    @Override
    public String[] getSupportedLanguages(@NotNull ResolveContext<Void> ctx) {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public Iterator<Resource> findResources(@NotNull ResolveContext<Void> ctx, String query, String language) {
        return unlockResourceMetadata(mockResourceResolver.findResources(query, language));
    }

    @Override
    public Iterator<ValueMap> queryResources(@NotNull ResolveContext<Void> ctx, String query, String language) {
        Iterator<Map<String,Object>> result = mockResourceResolver.queryResources(query, language);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(result, Spliterator.ORDERED), false)
                .map(MockQueryLanguageProvider::toValueMap)
                .iterator();
    }

    private static ValueMap toValueMap(Map<String,Object> item) {
        if (item instanceof ValueMap) {
            return (ValueMap)item;
        }
        else {
            return new ValueMapDecorator(item);
        }
    }

    private static Iterator<Resource> unlockResourceMetadata(Iterator<Resource> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .map(MockQueryLanguageProvider::unlockResourceMetadata)
                .iterator();
    }

    /**
     * ResourceResolver locks resources returned by ResourceProvider. If those resources are used to mock up
     * the query result, it fails because during processing the query result the resolution path in the query metdata
     * is set again. So we need to wrap the resources and unlock the resource metadata here.
     * @param resource Resource
     * @return Wrapped resource with unlocked metadata
     */
    private static Resource unlockResourceMetadata(Resource resource) {
        ResourceMetadata rm = new ResourceMetadata();
        rm.putAll(resource.getResourceMetadata());
        return new ResourceWrapper(resource) {
            @Override
            public ResourceMetadata getResourceMetadata() {
                return rm;
            }
        };
    }

}
