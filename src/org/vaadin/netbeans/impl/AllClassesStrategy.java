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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ClassIndex.NameKind;
import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.vaadin.netbeans.model.SourceDescendantsStrategy;

/**
 * @author denis
 */
class AllClassesStrategy implements SourceDescendantsStrategy {

    @Override
    public synchronized Collection<TypeElement> getSourceSubclasses(
            TypeElement type, CompilationInfo info )
            throws InterruptedException
    {
        return collectDescendants(type, info, true);
    }

    @Override
    public synchronized Collection<TypeElement> getSourceSubInterfaces(
            TypeElement type, CompilationInfo info )
            throws InterruptedException
    {
        return collectDescendants(type, info, false);
    }

    private Collection<TypeElement> collectDescendants( TypeElement type,
            CompilationInfo info, boolean isClasses )
            throws InterruptedException
    {
        initTypes(info);
        List<ElementHandle<TypeElement>> types = null;
        if (isClasses) {
            types = myClasses;
        }
        else {
            types = myInterfaces;
        }
        return collectDescendants(type, info, types);
    }

    private Collection<TypeElement> collectDescendants( TypeElement type,
            CompilationInfo info, List<ElementHandle<TypeElement>> types )
    {
        List<TypeElement> classes = new ArrayList<>();
        for (ElementHandle<TypeElement> handle : types) {
            TypeElement clazz = handle.resolve(info);
            if (clazz != null
                    && info.getTypes().isSubtype(clazz.asType(), type.asType()))
            {
                classes.add(clazz);
            }
        }
        return classes;
    }

    private void initTypes( CompilationInfo info ) throws InterruptedException {
        if (myClasses == null) {
            Set<TypeElement> allTypes =
                    findAllTypes(info, EnumSet.allOf(ElementKind.class));
            myClasses = new ArrayList<>(allTypes.size());
            myInterfaces = new ArrayList<>(allTypes.size());
            for (TypeElement typeElement : allTypes) {
                if (typeElement.getKind().equals(ElementKind.INTERFACE)) {
                    myInterfaces.add(ElementHandle.create(typeElement));
                }
                else if (typeElement.getKind().equals(ElementKind.CLASS)) {
                    myClasses.add(ElementHandle.create(typeElement));
                }
            }
        }
    }

    static Set<TypeElement> findAllTypes( CompilationInfo info,
            Set<ElementKind> kinds, SearchScope... scopes )
            throws InterruptedException
    {
        Set<TypeElement> result = new HashSet<>();
        Set<ElementHandle<TypeElement>> handles =
                info.getClasspathInfo()
                        .getClassIndex()
                        .getDeclaredTypes("", NameKind.PREFIX,
                                EnumSet.of(SearchScope.SOURCE, scopes));
        if (handles == null) {
            throw new InterruptedException(
                    "ClassIndex.getDeclaredTypes() was interrupted"); // NOI18N
        }
        for (ElementHandle<TypeElement> elementHandle : handles) {
            TypeElement element = elementHandle.resolve(info);
            if (element == null || !kinds.contains(element.getKind())) {
                continue;
            }
            result.add(element);
        }
        return result;
    }

    private List<ElementHandle<TypeElement>> myClasses;

    private List<ElementHandle<TypeElement>> myInterfaces;
}
