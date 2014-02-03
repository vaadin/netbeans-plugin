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
package org.vaadin.netbeans.model;

import java.util.Collection;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;

/**
 * @author denis
 */
public interface SourceDescendantsStrategy {

    Collection<TypeElement> getSourceSubclasses( TypeElement type,
            CompilationInfo info ) throws InterruptedException;

    Collection<TypeElement> getSourceSubInterfaces( TypeElement type,
            CompilationInfo info ) throws InterruptedException;
}
