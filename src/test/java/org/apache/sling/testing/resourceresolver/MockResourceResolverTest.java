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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class MockResourceResolverTest {

    private ResourceResolver resourceResolver;

    @Before
    public void setUp() throws LoginException {
        MockResourceResolverFactory factory = new MockResourceResolverFactory();
        resourceResolver = factory.getResourceResolver(null);
    }

    /**
     * Test method for {@link org.apache.sling.testing.resourceresolver.MockResourceResolver#resolve(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     * @deprecated Use {@link #testResolveJakartaHttpServletRequestString1()} instead.
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testResolveJavaxHttpServletRequestString() {
        javax.servlet.http.HttpServletRequest javaxRequest = Mockito.mock(javax.servlet.http.HttpServletRequest.class);
        Resource r = resourceResolver.resolve(javaxRequest, "/path?k1=v1");
        assertNotNull(r);
        assertEquals("/path?k1=v1", r.getPath());
    }

    /**
     * Test method for {@link org.apache.sling.testing.resourceresolver.MockResourceResolver#resolve(jakarta.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void testResolveJakartaHttpServletRequestString() {
        HttpServletRequest jakartaRequest = Mockito.mock(HttpServletRequest.class);
        Resource r = resourceResolver.resolve(jakartaRequest, "/path#k1=v1");
        assertNotNull(r);
        assertEquals("/path#k1=v1", r.getPath());
    }

    /**
     * Test method for {@link org.apache.sling.testing.resourceresolver.MockResourceResolver#map(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     * @deprecated Use {@link #testMapJakartaHttpServletRequestString()} instead.
     */
    @Deprecated(since = "2.0.0")
    @Test
    public void testMapJavaxHttpServletRequestString() {
        javax.servlet.http.HttpServletRequest javaxRequest = Mockito.mock(javax.servlet.http.HttpServletRequest.class);
        String map = resourceResolver.map(javaxRequest, "/path#k1=v1");
        assertEquals("/path#k1=v1", map);
    }

    /**
     * Test method for {@link org.apache.sling.testing.resourceresolver.MockResourceResolver#map(jakarta.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void testMapJakartaHttpServletRequestString() {
        HttpServletRequest jakartaRequest = Mockito.mock(HttpServletRequest.class);
        String map = resourceResolver.map(jakartaRequest, "/path?k1=v1");
        assertEquals("/path?k1=v1", map);
    }
}
