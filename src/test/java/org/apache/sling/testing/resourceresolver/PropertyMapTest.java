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

import java.io.Closeable;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PropertyMapTest {

    @Test
    public void testGetPropertyMap() throws Exception {
        // not having a map must not change the behavior
        MockResourceResolver resolver =
                (MockResourceResolver) new MockResourceResolverFactory().getResourceResolver(null);
        resolver.close(); // must not throw an exception

        // use the propertyMap
        resolver = (MockResourceResolver) new MockResourceResolverFactory().getResourceResolver(null);
        Object value1 = new String("value1");
        Closeable value2 = Mockito.spy(new Closeable() {
            @Override
            public void close() {
                // do nothing
            }
        });
        Closeable valueWithException = Mockito.spy(new Closeable() {
            @Override
            public void close() {
                throw new RuntimeException("RuntimeExceptions in close must be handled");
            }
        });
        assertNotNull(resolver.getPropertyMap());
        resolver.getPropertyMap().put("key1", value1);
        resolver.getPropertyMap().put("key2", value2);
        resolver.getPropertyMap().put("key3", valueWithException);

        resolver.close();
        assertNotNull(resolver.getPropertyMap());
        assertTrue(resolver.getPropertyMap().isEmpty());
        Mockito.verify(value2, Mockito.times(1)).close();
        Mockito.verify(valueWithException, Mockito.times(1)).close();
    }
}
