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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.event.EventAdmin;

/**
 * Options for the factory
 */
public class MockResourceResolverFactoryOptions {

    private EventAdmin eventAdmin;

    private String[] searchPaths = new String[] {"/apps/", "/libs/"};
    
    private boolean mangleNamespacePrefixes;

    public @Nullable EventAdmin getEventAdmin() {
        return eventAdmin;
    }

    public @NotNull MockResourceResolverFactoryOptions setEventAdmin(@Nullable EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
        return this;
    }

    public @NotNull String @NotNull [] getSearchPaths() {
        return searchPaths;
    }

    public @NotNull MockResourceResolverFactoryOptions setSearchPaths(@NotNull String @Nullable [] searchPaths) {
        if ( searchPaths == null ) {
            searchPaths = new String[] {};
        }
        this.searchPaths = searchPaths;
        return this;
    }

    public boolean isMangleNamespacePrefixes() {
        return mangleNamespacePrefixes;
    }

    public @NotNull MockResourceResolverFactoryOptions setMangleNamespacePrefixes(boolean mangleNamespacePrefixes) {
        this.mangleNamespacePrefixes = mangleNamespacePrefixes;
        return this;
    }
    
}
