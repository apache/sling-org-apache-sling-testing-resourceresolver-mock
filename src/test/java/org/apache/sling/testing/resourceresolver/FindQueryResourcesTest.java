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

import static javax.jcr.query.Query.JCR_SQL2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Tests finding/querying for resources.
 */
@SuppressWarnings("null")
public class FindQueryResourcesTest {

    private ResourceResolver resourceResolver;
    private Resource resource1;
    private Resource resource2;

    @Before
    public void setUp() throws Exception {
        resourceResolver = createResourceResolver();

        MockHelper.create(resourceResolver)
            .resource("/resource1").p("prop1", "value1")
            .resource("/resource2").p("prop1", "value2")
            .commit();
        resource1 = resourceResolver.getResource("/resource1");
        resource2 = resourceResolver.getResource("/resource2");
    }

    protected ResourceResolver createResourceResolver() throws LoginException {
        return new MockResourceResolverFactory().getResourceResolver(null);
    }

    @Test
    public void testFindResourcesNoHandler() {
        Iterator<Resource> result = resourceResolver.findResources("any-query", JCR_SQL2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFindResourcesSingleHandler() {
        List<Resource> expected = ImmutableList.of(resource1, resource2);
        MockFindQueryResources.addFindResourceHandler(resourceResolver, (query, language) -> expected.iterator());

        assertResources(expected, resourceResolver.findResources("any-query", JCR_SQL2));
    }

    @Test
    public void testFindResourcesMultipleHandlers() {
        List<Resource> expected1 = ImmutableList.of(resource1);
        MockFindQueryResources.addFindResourceHandler(resourceResolver, (query, language) ->
            StringUtils.equals(query, "q1") ? expected1.iterator() : null);

        List<Resource> expected2 = ImmutableList.of(resource2);
        MockFindQueryResources.addFindResourceHandler(resourceResolver, (query, language) ->
            StringUtils.equals(query, "q2") ? expected2.iterator() : null);

        assertResources(expected1, resourceResolver.findResources("q1", JCR_SQL2));
        assertResources(expected2, resourceResolver.findResources("q2", JCR_SQL2));
    }

    @Test
    public void testQueryResourcesNoHandler() {
        Iterator<Map<String,Object>> result = resourceResolver.queryResources("any-query", JCR_SQL2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testQueryResourcesSingleHandler() {
        List<Map<String,Object>> expected = ImmutableList.of(resource1.getValueMap(), resource2.getValueMap());
        MockFindQueryResources.addQueryResourceHandler(resourceResolver, (query, language) -> expected.iterator());

        assertEquals(expected, ImmutableList.copyOf(resourceResolver.queryResources("any-query", JCR_SQL2)));
    }

    @Test
    public void testQueryResourcesMultipleHandlers() {
        List<Map<String,Object>> expected1 = ImmutableList.of(resource1.getValueMap());
        MockFindQueryResources.addQueryResourceHandler(resourceResolver, (query, language) ->
            StringUtils.equals(query, "q1") ? expected1.iterator() : null);

        List<Map<String,Object>> expected2 = ImmutableList.of(resource2.getValueMap());
        MockFindQueryResources.addQueryResourceHandler(resourceResolver, (query, language) ->
            StringUtils.equals(query, "q2") ? expected2.iterator() : null);

        assertEquals(expected1, ImmutableList.copyOf(resourceResolver.queryResources("q1", JCR_SQL2)));
        assertEquals(expected2, ImmutableList.copyOf(resourceResolver.queryResources("q2", JCR_SQL2)));
    }

    private void assertResources(List<Resource> expected, Iterator<Resource> actual) {
        Map<String,ValueMap> expectedData = expected.stream()
                .collect(Collectors.toMap(Resource::getPath, Resource::getValueMap));
        Map<String,ValueMap> actualData = StreamSupport.stream(Spliterators.spliteratorUnknownSize(actual, Spliterator.ORDERED), false)
                .collect(Collectors.toMap(Resource::getPath, Resource::getValueMap));
        assertEquals(expectedData, actualData);
    }

}
