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
import java.util.Collections;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.vaadin.netbeans.model.SourceDescendantsStrategy;

/**
 * @author denis
 */
class EmptyStrategy implements SourceDescendantsStrategy {

    @Override
    public Collection<TypeElement> getSourceSubclasses( TypeElement type,
            CompilationInfo info )
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<TypeElement> getSourceSubInterfaces( TypeElement type,
            CompilationInfo info )
    {
        return Collections.emptyList();
    }

}
