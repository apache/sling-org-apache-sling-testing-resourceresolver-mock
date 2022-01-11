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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class MockResourceResolver extends SlingAdaptable implements ResourceResolver {

    private final Map<String, Map<String, Object>> resources;

    private final Map<String, Map<String, Object>> temporaryResources = new LinkedHashMap<String, Map<String,Object>>();

    private final Set<String> deletedResources = new HashSet<String>();

    private final MockResourceResolverFactoryOptions options;

    private final MockResourceResolverFactory factory;

    private final Map<String,Object> attributes;
    
    private Map<String,Object> propertyMap;

    private final List<MockFindResourcesHandler> findResourcesHandlers = new ArrayList<>();
    private final List<MockQueryResourceHandler> queryResourcesHandlers = new ArrayList<>();

    public MockResourceResolver(final MockResourceResolverFactoryOptions options,
            final MockResourceResolverFactory factory,
            final Map<String, Map<String, Object>> resources) {
        this(options, factory, resources, Collections.<String,Object>emptyMap());
    }

    public MockResourceResolver(final MockResourceResolverFactoryOptions options,
            final MockResourceResolverFactory factory,
            final Map<String, Map<String, Object>> resources,
            final Map<String,Object> attributes) {
        this.factory = factory;
        this.options = options;
        this.resources = resources;
        this.attributes = attributes;
    }

    @Override
    @SuppressWarnings("unused")
    public @NotNull Resource resolve(final @NotNull HttpServletRequest request, final @NotNull String absPath) {
        String path = absPath;
        if (path == null) {
            path = "/";
        }

        // split off query string or fragment that may be appendend to the URL
        String urlRemainder = null;
        int urlRemainderPos = Math.min(path.indexOf('?'), path.indexOf('#'));
        if (urlRemainderPos >= 0) {
          urlRemainder = path.substring(urlRemainderPos);
          path = path.substring(0, urlRemainderPos);
        }

        // unmangle namespaces
        if (options.isMangleNamespacePrefixes()) {
            path = NamespaceMangler.unmangleNamespaces(path);
        }

        // build full path again
        path = path + (urlRemainder != null ? urlRemainder : "");

        Resource resource = this.getResource(path);
        if (resource == null) {
            resource = new NonExistingResource(this, absPath);
        }
        return resource;
    }

    @Override
    @SuppressWarnings("null")
    public @NotNull Resource resolve(final @NotNull String absPath) {
        return resolve(null, absPath);
    }

    @Override
    @SuppressWarnings("null")
    public @NotNull String map(final @NotNull String resourcePath) {
        return map(null, resourcePath);
    }

    @Override
    public String map(final @NotNull HttpServletRequest request, final @NotNull String resourcePath) {
        String path = resourcePath;

        // split off query string or fragment that may be appendend to the URL
        String urlRemainder = null;
        int urlRemainderPos = Math.min(path.indexOf('?'), path.indexOf('#'));
        if (urlRemainderPos >= 0) {
          urlRemainder = path.substring(urlRemainderPos);
          path = path.substring(0, urlRemainderPos);
        }

        // mangle namespaces
        if (options.isMangleNamespacePrefixes()) {
            path = NamespaceMangler.mangleNamespaces(path);
        }

        // build full path again
        return path + (urlRemainder != null ? urlRemainder : "");
    }

    @Override
    public Resource getResource(final @NotNull String path) {
        Resource resource = getResourceInternal(path);

        // if not resource found check if this is a reference to a property
        if (resource == null && path != null) {
            String parentPath = ResourceUtil.getParent(path);
            if (parentPath != null) {
                String name = ResourceUtil.getName(path);
                Resource parentResource = getResourceInternal(parentPath);
                if (parentResource!=null) {
                    ValueMap props = ResourceUtil.getValueMap(parentResource);
                    if (props.containsKey(name)) {
                        return new MockPropertyResource(path, props, this);
                    }
                }
            }
        }

        return resource;
    }

    private Resource getResourceInternal(final String path) {
        if (path == null) {
            return null;
        }

        String normalizedPath = ResourceUtil.normalize(path);
        if (normalizedPath == null) {
            return null;
        } else if ( normalizedPath.startsWith("/") ) {
            if ( this.deletedResources.contains(normalizedPath) ) {
                return null;
            }
            final Map<String, Object> tempProps = this.temporaryResources.get(normalizedPath);
            if ( tempProps != null ) {
                return newMockResource(normalizedPath, tempProps, this);
            }
            synchronized ( this.resources ) {
                final Map<String, Object> props = this.resources.get(normalizedPath);
                if ( props != null ) {
                    return newMockResource(normalizedPath, props, this);
                }
            }
        } else {
            for(final String s : this.getSearchPath() ) {
                final Resource rsrc = this.getResource(s + '/' + normalizedPath);
                if ( rsrc != null ) {
                    return rsrc;
                }
            }
        }
        return null;
    }

    @Override
    public Resource getResource(Resource base, @NotNull String path) {
        if ( path == null || path.length() == 0 ) {
            path = "/";
        }
        if ( path.startsWith("/") ) {
            return getResource(path);
        }
        if ( base.getPath().equals("/") ) {
            return getResource(base.getPath() + path);
        }
        return getResource(base.getPath() + '/' + path);
    }

    @Override
    public String @NotNull [] getSearchPath() {
        return this.options.getSearchPaths();
    }

    @Override
    public @NotNull Iterator<Resource> listChildren(final @NotNull Resource parent) {
        final String pathPrefix = "/".equals(parent.getPath()) ? "" : parent.getPath();
        final Pattern childPathMatcher = Pattern.compile("^" + Pattern.quote(pathPrefix) + "/[^/]+$");
        final Map<String, Map<String, Object>> candidates = new LinkedHashMap<String, Map<String,Object>>();
        synchronized ( this.resources ) {
            for(final Map.Entry<String, Map<String, Object>> e : this.resources.entrySet()) {
                if (childPathMatcher.matcher(e.getKey()).matches()) {
                    if ( !this.deletedResources.contains(e.getKey()) ) {
                        candidates.put(e.getKey(), e.getValue());
                    }
                }
            }
            for(final Map.Entry<String, Map<String, Object>> e : this.temporaryResources.entrySet()) {
                if (childPathMatcher.matcher(e.getKey()).matches()) {
                    if ( !this.deletedResources.contains(e.getKey()) ) {
                        candidates.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        final List<Resource> children = new ArrayList<Resource>();
        for(final Map.Entry<String, Map<String, Object>> e : candidates.entrySet()) {
            children.add(newMockResource(e.getKey(), e.getValue(), this));
        }
        return children.iterator();
    }

    private Resource newMockResource(final String path,
            final Map<String, Object> properties,
            final ResourceResolver resolver) {
        return this.options.getMockResourceFactory()
                    .newMockResource(path, properties, resolver);
    }

    @Override
    public @NotNull Iterable<Resource> getChildren(final @NotNull Resource parent) {
        return new Iterable<Resource>() {
            @Override
            public Iterator<Resource> iterator() {
                return listChildren(parent);
            }
        };
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public void close() {
        clearPropertyMap();
        this.factory.closed(this);
    }

    private void clearPropertyMap(){
        if (propertyMap != null) {
            for (Entry<String, Object> entry : propertyMap.entrySet()) {
                if (entry.getValue()  instanceof Closeable) {
                    try {
                        ((Closeable) entry.getValue()).close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            propertyMap.clear();
        }
    }

    @Override
    public String getUserID() {
        return null;
    }

    @Override
    public @NotNull Iterator<String> getAttributeNames() {
        return attributes.keySet().iterator();
    }

    @Override
    public Object getAttribute(final @NotNull String name) {
        return attributes.get(name);
    }

    @Override
    public void delete(final @NotNull Resource resource) throws PersistenceException {
        this.deletedResources.add(resource.getPath());
        this.temporaryResources.remove(resource.getPath());
        final String prefixPath = resource.getPath() + '/';
        synchronized ( this.resources ) {
            for(final Map.Entry<String, Map<String, Object>> e : this.resources.entrySet()) {
                if (e.getKey().startsWith(prefixPath)) {
                    this.deletedResources.add(e.getKey());
                }
            }
            final Iterator<Map.Entry<String, Map<String, Object>>> i = this.temporaryResources.entrySet().iterator();
            while ( i.hasNext() ) {
                final Map.Entry<String, Map<String, Object>> e = i.next();
                if (e.getKey().startsWith(prefixPath) ) {
                    i.remove();
                }
            }
        }
    }

    @Override
    public @NotNull Resource create(@NotNull Resource parent, @NotNull String name,
            Map<String, Object> properties) throws PersistenceException {
        final String path = (parent.getPath().equals("/") ? parent.getPath() + name : parent.getPath() + '/' + name);
        if ( this.temporaryResources.containsKey(path) ) {
            throw new PersistenceException("Path already exists: " + path);
        }
        synchronized ( this.resources ) {
            if ( this.resources.containsKey(path) && !this.deletedResources.contains(path) ) {
                throw new PersistenceException("Path already exists: " + path);
            }
        }
        this.deletedResources.remove(path);
        if ( properties == null ) {
            properties = new HashMap<String, Object>();
        }

        Resource mockResource = newMockResource(path, properties, this);
        this.temporaryResources.put(path, ResourceUtil.getValueMap(mockResource));
        return mockResource;
    }

    @Override
    public void revert() {
        this.deletedResources.clear();
        this.temporaryResources.clear();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void commit() throws PersistenceException {
        EventAdmin eventAdmin = this.options.getEventAdmin();
        synchronized ( this.resources ) {
            for(final String path : this.deletedResources ) {
                if ( this.resources.remove(path) != null && eventAdmin != null ) {
                    final Dictionary<String, Object> props = new Hashtable<String, Object>();
                    props.put(SlingConstants.PROPERTY_PATH, path);
                    final Event e = new Event(SlingConstants.TOPIC_RESOURCE_REMOVED, props);
                    eventAdmin.sendEvent(e);
                }
                this.temporaryResources.remove(path);
            }
            for(final String path : this.temporaryResources.keySet() ) {
                final boolean changed = this.resources.containsKey(path);
                this.resources.put(path, this.temporaryResources.get(path));
                if ( eventAdmin != null ) {
                    final Dictionary<String, Object> props = new Hashtable<String, Object>();
                    props.put(SlingConstants.PROPERTY_PATH, path);
                    if ( this.resources.get(path).get(ResourceResolver.PROPERTY_RESOURCE_TYPE) != null ) {
                        props.put(SlingConstants.PROPERTY_RESOURCE_TYPE, this.resources.get(path).get(ResourceResolver.PROPERTY_RESOURCE_TYPE));
                    }
                    final Event e = new Event(changed ? SlingConstants.TOPIC_RESOURCE_CHANGED : SlingConstants.TOPIC_RESOURCE_ADDED, props);
                    eventAdmin.sendEvent(e);
                }
            }
        }
        this.revert();
    }

    @Override
    public boolean hasChanges() {
        return this.temporaryResources.size() > 0 || this.deletedResources.size() > 0;
    }

    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        boolean result = false;
        if ( resource != null && resourceType != null ) {
             // Check if the resource is of the given type. This method first checks the
             // resource type of the resource, then its super resource type and continues
             //  to go up the resource super type hierarchy.
             if (ResourceTypeUtil.areResourceTypesEqual(resourceType, resource.getResourceType(), getSearchPath())) {
                 result = true;
             } else {
                 Set<String> superTypesChecked = new HashSet<>();
                 String superType = this.getParentResourceType(resource);
                 while (!result && superType != null) {
                     if (ResourceTypeUtil.areResourceTypesEqual(resourceType, superType, getSearchPath())) {
                         result = true;
                     } else {
                         superTypesChecked.add(superType);
                         superType = this.getParentResourceType(superType);
                         if (superType != null && superTypesChecked.contains(superType)) {
                             throw new SlingException("Cyclic dependency for resourceSuperType hierarchy detected on resource " + resource.getPath()) {
                                // anonymous class to avoid problem with null cause
                                private static final long serialVersionUID = 1L;
                             };
                         }
                     }
                 }
             }

        }
        return result;
    }

    @Override
    public void refresh() {
        // nothing to do
    }

    public void addChanged(final String path, final Map<String, Object> props) {
        this.temporaryResources.put(path, props);
    }

    @Override
    public String getParentResourceType(Resource resource) {
        String resourceSuperType = null;
        if ( resource != null ) {
            resourceSuperType = resource.getResourceSuperType();
            if (resourceSuperType == null) {
                resourceSuperType = this.getParentResourceType(resource.getResourceType());
            }
        }
        return resourceSuperType;
    }

    @Override
    public String getParentResourceType(String resourceType) {
        // normalize resource type to a path string
        final String rtPath = (resourceType == null ? null : ResourceUtil.resourceTypeToPath(resourceType));
        // get the resource type resource and check its super type
        String resourceSuperType = null;
        if ( rtPath != null ) {
            final Resource rtResource = getResource(rtPath);
            if (rtResource != null) {
                resourceSuperType = rtResource.getResourceSuperType();
            }
        }
        return resourceSuperType;
    }

    @Override
    public boolean hasChildren(@NotNull Resource resource) {
        return this.listChildren(resource).hasNext();
    }

    @Override
    public Resource getParent(@NotNull Resource child) {
        final String parentPath = ResourceUtil.getParent(child.getPath());
        if (parentPath == null) {
            return null;
        }
        return this.getResource(parentPath);
    }

    @Override
    @SuppressWarnings("null")
    public @NotNull Iterator<Resource> findResources(final @NotNull String query, final String language) {
        return findResourcesHandlers.stream()
            .map(handler -> handler.findResources(query, language))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(Collections.emptyIterator());
    }

    /**
     * Adds a handler that can provide a mocked find resources result. You can add multiple handlers which are called
     * in the order they were added when calling {@link #findResources(String, String)}.
     * The result of the first handler that returns a non-null result is used.
     * If no handler delivers a result, an empty result is returned.
     * @param handler Handler
     */
    public void addFindResourceHandler(@NotNull MockFindResourcesHandler handler) {
        findResourcesHandlers.add(handler);
    }

    @Override
    @SuppressWarnings("null")
    public @NotNull Iterator<Map<String, Object>> queryResources(@NotNull String query, String language) {
        return queryResourcesHandlers.stream()
                .map(handler -> handler.queryResources(query, language))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(Collections.emptyIterator());
    }

    /**
     * Adds a handler that can provide a mocked query resources result. You can add multiple handlers which are called
     * in the order they were added when calling {@link #queryResources(String, String)}.
     * The result of the first handler that returns a non-null result is used.
     * If no handler delivers a result, an empty result is returned.
     * @param handler Handler
     */
    public void addQueryResourceHandler(@NotNull MockQueryResourceHandler handler) {
        queryResourcesHandlers.add(handler);
    }

    // Sling API 2.24.0
    public @NotNull Map<String, Object> getPropertyMap() {
        if (propertyMap == null) {
            propertyMap = new HashMap<>();
        }
        return propertyMap;
    }

    // --- unsupported operations ---

    @Override
    @Deprecated
    public @NotNull Resource resolve(final @NotNull HttpServletRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        throw new UnsupportedOperationException();
    }

    // Sling API 2.24.0
    public boolean orderBefore(@NotNull Resource parent, @NotNull String name,
            @Nullable String followingSiblingName) throws UnsupportedOperationException, PersistenceException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }



}
