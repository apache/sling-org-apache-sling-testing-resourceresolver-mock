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

import java.util.*;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests copy and move operation on resources
 */
public class CopyMoveResourceResolverTest {

    private ResourceResolver resourceResolver;
    private Resource testRoot;
    private Resource testDestination;

    @Before
    @SuppressWarnings("null")
    public final void setUp() throws Exception {
        resourceResolver = createResourceResolver();
        Resource root = resourceResolver.getResource("/");
        testRoot = resourceResolver.create(root, "test", ValueMap.EMPTY);
        testDestination = resourceResolver.create(root, "test-dest", ValueMap.EMPTY);
    }

    protected ResourceResolver createResourceResolver() throws LoginException {
        return new MockResourceResolverFactory().getResourceResolver(null);
    }

    @Test
    public void testSimpleCopy() throws PersistenceException {
        Resource testSource = resourceResolver.create(testRoot, "simple-copy-resource", Map.of("testprop", "testval"));
        assertNotNull(testSource);

        Resource destination = resourceResolver.copy(testSource.getPath(), testDestination.getPath());
        testSource = resourceResolver.getResource(testSource.getPath());
        assertNotNull(testSource);
        assertNotNull(destination);
        final ValueMap destinationProperties = destination.getValueMap();
        assertEquals(testSource.getValueMap().size(), destinationProperties.size());
        testSource.getValueMap().forEach((key, value) -> assertEquals(value, destinationProperties.get(key)));

        assertTrue(resourceResolver.hasChanges());
    }

    @Test
    public void testComplexCopy() throws PersistenceException {
        Resource testSource = resourceResolver.create(testRoot, "complex-copy-resource", createUniqueProperties());
        Resource child1 = resourceResolver.create(testSource, "child-1", createUniqueProperties());
        resourceResolver.create(child1, "grand-child-1-1", createUniqueProperties());
        Resource child2 = resourceResolver.create(testSource, "child-2", createUniqueProperties());
        resourceResolver.create(child2, "grand-child-2-1", createUniqueProperties());
        Resource grandChild22 = resourceResolver.create(child2, "grand-child-2-2", createUniqueProperties());
        resourceResolver.create(grandChild22, "great-grand-child-2-2-1", createUniqueProperties());
        resourceResolver.create(grandChild22, "great-grand-child-2-2-2", createUniqueProperties());
        resourceResolver.create(grandChild22, "great-grand-child-2-2-3", createUniqueProperties());

        final String[] pathsToCheck = {
            "/test-dest/complex-copy-resource",
            "/test-dest/complex-copy-resource/child-1",
            "/test-dest/complex-copy-resource/child-1/grand-child-1-1",
            "/test-dest/complex-copy-resource/child-2",
            "/test-dest/complex-copy-resource/child-2/grand-child-2-1",
            "/test-dest/complex-copy-resource/child-2/grand-child-2-2",
            "/test-dest/complex-copy-resource/child-2/grand-child-2-2/great-grand-child-2-2-1",
            "/test-dest/complex-copy-resource/child-2/grand-child-2-2/great-grand-child-2-2-2",
            "/test-dest/complex-copy-resource/child-2/grand-child-2-2/great-grand-child-2-2-3"
        };

        Resource destination = resourceResolver.copy(testSource.getPath(), testDestination.getPath());
        assertNotNull(destination);
        Arrays.stream(pathsToCheck).forEach(path -> {
            Resource copiedResource = resourceResolver.getResource(path);
            assertNotNull(copiedResource);
            Resource relSource = resourceResolver.getResource(path.replace("/test-dest", "/test"));
            assertNotNull(relSource);
            final ValueMap copiedProps = copiedResource.getValueMap();
            assertEquals(relSource.getValueMap().size(), copiedProps.size());
            relSource.getValueMap().forEach((key, value) -> assertEquals(value, copiedProps.get(key)));
        });

        assertTrue(resourceResolver.hasChanges());
    }

    @Test(expected = PersistenceException.class)
    public void testFailedCopy_DestinationExists() throws PersistenceException {
        Resource testSource = resourceResolver.create(testRoot, "failed-copy-resource", ValueMap.EMPTY);
        resourceResolver.copy(testSource.getPath(), testSource.getParent().getPath());
    }

    @Test(expected = PersistenceException.class)
    public void testFailedCopy_SourceDoesntExist() throws PersistenceException {
        Resource testSource = resourceResolver.create(testRoot, "failed-copy-resource", ValueMap.EMPTY);
        resourceResolver.copy(testSource.getPath(), "/non-existing-parent");
    }

    @Test(expected = PersistenceException.class)
    public void testFailedCopy_DestinationParentDoesntExist() throws PersistenceException {
        resourceResolver.copy("/non-existing-path", "/test");
    }

    @Test
    public void testSimpleMove() throws PersistenceException {
        Resource testSource = resourceResolver.create(testRoot, "simple-move-resource", Map.of("testprop", "testval"));
        assertNotNull(testSource);

        Resource destination = resourceResolver.move(testSource.getPath(), testDestination.getPath());
        assertNull(resourceResolver.getResource(testSource.getPath()));
        assertNotNull(destination);
        final ValueMap destinationProperties = destination.getValueMap();
        assertEquals(testSource.getValueMap().size(), destinationProperties.size());
        testSource.getValueMap().forEach((key, value) -> assertEquals(value, destinationProperties.get(key)));

        assertTrue(resourceResolver.hasChanges());
    }

    @Test
    public void testComplexMove() throws PersistenceException {
        Resource testSource = resourceResolver.create(testRoot, "complex-copy-resource", createUniqueProperties());
        Resource child1 = resourceResolver.create(testSource, "child-1", createUniqueProperties());
        Resource grandChild11 = resourceResolver.create(child1, "grand-child-1-1", createUniqueProperties());
        Resource child2 = resourceResolver.create(testSource, "child-2", createUniqueProperties());
        Resource grandChild21 = resourceResolver.create(child2, "grand-child-2-1", createUniqueProperties());
        Resource grandChild22 = resourceResolver.create(child2, "grand-child-2-2", createUniqueProperties());
        Resource greatGrandChild221 =
                resourceResolver.create(grandChild22, "great-grand-child-2-2-1", createUniqueProperties());
        Resource greatGrandChild222 =
                resourceResolver.create(grandChild22, "great-grand-child-2-2-2", createUniqueProperties());
        Resource greatGrandChild223 =
                resourceResolver.create(grandChild22, "great-grand-child-2-2-3", createUniqueProperties());

        assertResourceMovedCorrectly(
                testSource, resourceResolver.move(testSource.getPath(), testDestination.getPath()));
        assertResourceMovedCorrectly(child1, resourceResolver.getResource("/test-dest/complex-copy-resource/child-1"));
        assertResourceMovedCorrectly(
                grandChild11, resourceResolver.getResource("/test-dest/complex-copy-resource/child-1/grand-child-1-1"));
        assertResourceMovedCorrectly(child2, resourceResolver.getResource("/test-dest/complex-copy-resource/child-2"));
        assertResourceMovedCorrectly(
                grandChild21, resourceResolver.getResource("/test-dest/complex-copy-resource/child-2/grand-child-2-1"));
        assertResourceMovedCorrectly(
                grandChild22, resourceResolver.getResource("/test-dest/complex-copy-resource/child-2/grand-child-2-2"));
        assertResourceMovedCorrectly(
                greatGrandChild221,
                resourceResolver.getResource(
                        "/test-dest/complex-copy-resource/child-2/grand-child-2-2/great-grand-child-2-2-1"));
        assertResourceMovedCorrectly(
                greatGrandChild222,
                resourceResolver.getResource(
                        "/test-dest/complex-copy-resource/child-2/grand-child-2-2/great-grand-child-2-2-2"));
        assertResourceMovedCorrectly(
                greatGrandChild223,
                resourceResolver.getResource(
                        "/test-dest/complex-copy-resource/child-2/grand-child-2-2/great-grand-child-2-2-3"));

        assertTrue(resourceResolver.hasChanges());
    }

    @Test(expected = PersistenceException.class)
    public void testFailedMove_DestinationExists() throws PersistenceException {
        Resource testSource = resourceResolver.create(testRoot, "failed-move-resource", ValueMap.EMPTY);
        resourceResolver.move(testSource.getPath(), testSource.getParent().getPath());
    }

    @Test(expected = PersistenceException.class)
    public void testFailedMove_SourceDoesntExist() throws PersistenceException {
        Resource testSource = resourceResolver.create(testRoot, "failed-copy-resource", ValueMap.EMPTY);
        resourceResolver.move(testSource.getPath(), "/non-existing-parent");
    }

    @Test(expected = PersistenceException.class)
    public void testFailedMove_DestinationParentDoesntExist() throws PersistenceException {
        resourceResolver.move("/non-existing-path", "/test");
    }

    private void assertResourceMovedCorrectly(Resource originalResource, Resource movedResource) {
        assertNotNull(movedResource);
        assertNull(resourceResolver.getResource(originalResource.getPath()));
        final ValueMap movedProps = movedResource.getValueMap();
        assertEquals(originalResource.getValueMap().size(), movedProps.size());
        originalResource.getValueMap().forEach((key, value) -> assertEquals(value, movedProps.get(key)));
    }

    private Map<String, Object> createUniqueProperties() {
        return Map.of(
                JcrConstants.JCR_PRIMARYTYPE,
                "my:Type",
                SlingConstants.PROPERTY_RESOURCE_TYPE,
                "test/my-resource-type",
                "string-value",
                UUID.randomUUID(),
                "int-value",
                new Random().nextInt(10000),
                "date-value",
                Calendar.getInstance(),
                "long-value",
                System.nanoTime(),
                "bool-value",
                Math.random() < 0.5,
                "array-value",
                new String[] {"string 1", "string 2", "string 3"},
                "jcr:mixinTypes",
                new String[] {"rep:AccessControllable", "mix:versionable"});
    }
}
