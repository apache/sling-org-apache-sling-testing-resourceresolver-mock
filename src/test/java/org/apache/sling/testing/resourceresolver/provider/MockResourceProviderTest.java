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
package org.apache.sling.testing.resourceresolver.provider;

import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.resourceresolver.MockResourceProvider;
import org.apache.sling.testing.resourceresolver.MockResourceResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MockResourceProviderTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.NONE);

    private Resource testRoot;

    @Before
    @SuppressWarnings("null")
    public void setUp() throws Exception {
        context.registerInjectActivateService(MockResourceProvider.class);

        Resource root = context.resourceResolver().getResource("/");
        testRoot = context.resourceResolver().create(root, "test", ValueMap.EMPTY);
    }

    @Test
    public void testResourceResolverFromResource() {
        assertFalse("testRoot.resourceResolver is not MockResourceResolver",
                testRoot.getResourceResolver() instanceof MockResourceResolver);
    }

    @Test
    public void testResourceResolverFromChildren() throws Exception {
        context.resourceResolver().create(testRoot, "r1", ValueMap.EMPTY);
        context.resourceResolver().create(testRoot, "r2", ValueMap.EMPTY);

        Iterator<Resource> children = testRoot.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            assertFalse("testRoot.resourceResolver is not MockResourceResolver",
                    child.getResourceResolver() instanceof MockResourceResolver);
        }
    }


}
