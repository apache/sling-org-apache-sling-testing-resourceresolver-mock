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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests finding/querying for resources.
 */
@SuppressWarnings("null")
public class FindQueryResourcesTest {

    protected ResourceResolverFactory resourceResolverFactory = new MockResourceResolverFactory();
    protected ResourceResolver resourceResolver;
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
        resourceResolver.commit();
    }

    protected ResourceResolver createResourceResolver() throws LoginException {
        return resourceResolverFactory.getResourceResolver(null);
    }

    protected ResourceResolver createResourceResolver_addFindResourceHandlers(MockFindResourcesHandler... handlers) throws LoginException {
        // set handler directly on newly created resource resolver
        ResourceResolver resourceResolver = createResourceResolver();
        Arrays.stream(handlers).forEach(handler -> MockFindQueryResources.addFindResourceHandler(resourceResolver, handler));
        return resourceResolver;
    }

    protected ResourceResolver createResourceResolver_addQueryResourceHandlers(MockQueryResourceHandler... handlers) throws LoginException {
        // set handler directly on newly created resource resolver
        ResourceResolver resourceResolver = createResourceResolver();
        Arrays.stream(handlers).forEach(handler -> MockFindQueryResources.addQueryResourceHandler(resourceResolver, handler));
        return resourceResolver;
    }

    @Test
    public void testFindResourcesNoHandler() throws Exception {
        ResourceResolver resourceResolver = createResourceResolver_addFindResourceHandlers();
        Iterator<Resource> result = resourceResolver.findResources("any-query", JCR_SQL2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testFindResourcesSingleHandler() throws Exception {
        List<Resource> expected = List.of(resource1, resource2);
        ResourceResolver resourceResolver = createResourceResolver_addFindResourceHandlers(
                (query, language) -> expected.iterator());

        assertResources(expected, resourceResolver.findResources("any-query", JCR_SQL2));
    }

    @Test
    public void testFindResourcesMultipleHandlers() throws Exception {
        List<Resource> expected1 = List.of(resource1);
        List<Resource> expected2 = List.of(resource2);

        ResourceResolver resourceResolver = createResourceResolver_addFindResourceHandlers(
                (query, language) -> StringUtils.equals(query, "q1") ? expected1.iterator() : null,
                (query, language) -> StringUtils.equals(query, "q2") ? expected2.iterator() : null);

        assertResources(expected1, resourceResolver.findResources("q1", JCR_SQL2));
        assertResources(expected2, resourceResolver.findResources("q2", JCR_SQL2));
    }

    @Test
    public void testQueryResourcesNoHandler() throws Exception {
        ResourceResolver resourceResolver = createResourceResolver_addQueryResourceHandlers();
        Iterator<Map<String,Object>> result = resourceResolver.queryResources("any-query", JCR_SQL2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testQueryResourcesSingleHandler() throws Exception {
        List<Map<String,Object>> expected = List.of(resource1.getValueMap(), resource2.getValueMap());
        ResourceResolver resourceResolver = createResourceResolver_addQueryResourceHandlers(
                (query, language) -> expected.iterator());

        assertEquals(expected, IteratorUtils.toList(resourceResolver.queryResources("any-query", JCR_SQL2)));
    }

    @Test
    public void testQueryResourcesMultipleHandlers() throws Exception {
        List<Map<String,Object>> expected1 = List.of(resource1.getValueMap());

        List<Map<String,Object>> expected2 = List.of(resource2.getValueMap());
        ResourceResolver resourceResolver = createResourceResolver_addQueryResourceHandlers(
                (query, language) -> StringUtils.equals(query, "q1") ? expected1.iterator() : null,
                (query, language) -> StringUtils.equals(query, "q2") ? expected2.iterator() : null);

        assertEquals(expected1, IteratorUtils.toList(resourceResolver.queryResources("q1", JCR_SQL2)));
        assertEquals(expected2, IteratorUtils.toList(resourceResolver.queryResources("q2", JCR_SQL2)));
    }

    private void assertResources(List<Resource> expected, Iterator<Resource> actual) {
        Map<String,ValueMap> expectedData = expected.stream()
                .collect(Collectors.toMap(Resource::getPath, Resource::getValueMap));
        Map<String,ValueMap> actualData = StreamSupport.stream(Spliterators.spliteratorUnknownSize(actual, Spliterator.ORDERED), false)
                .collect(Collectors.toMap(Resource::getPath, Resource::getValueMap));
        assertEquals(expectedData, actualData);
    }

}
