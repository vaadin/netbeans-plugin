/*
 * Copyright 2000-2013 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.netbeans.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.IsInSourceQuery;
import org.vaadin.netbeans.model.SourceDescendantsStrategy;
import org.vaadin.netbeans.utils.JavaUtils;

/**
 * @author denis
 */
class RecursiveStrategy implements SourceDescendantsStrategy {

    public static final String VAADIN_PKG = "com.vaadin."; // NOI18N

    @Override
    public Collection<TypeElement> getSourceSubclasses( TypeElement type,
            CompilationInfo info ) throws InterruptedException
    {
        return filterDescendants(type, JavaUtils.getSubclasses(type, info),
                info);
    }

    @Override
    public Collection<TypeElement> getSourceSubInterfaces( TypeElement type,
            CompilationInfo info ) throws InterruptedException
    {
        return filterDescendants(type, JavaUtils.getSubinterfaces(type, info),
                info);
    }

    private boolean isVaadinClass( TypeElement type ) {
        String fqn = type.getQualifiedName().toString();
        return fqn.startsWith(VAADIN_PKG);
    }

    private Collection<TypeElement> filterDescendants( TypeElement type,
            Set<TypeElement> descendants, CompilationInfo info )
            throws InterruptedException
    {
        Set<FileObject> sourceRoots = IsInSourceQuery.getSourceRoots(info);
        for (Iterator<TypeElement> iterator = descendants.iterator(); iterator
                .hasNext();)
        {
            TypeElement clazz = iterator.next();
            if (isVaadinClass(clazz)
                    || !IsInSourceQuery.isInSourceRoots(clazz, info,
                            sourceRoots))
            {
                iterator.remove();
            }
        }
        return descendants;
    }

}
