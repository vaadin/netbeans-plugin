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
package org.vaadin.netbeans.editor.hints;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.HintContext;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.VaadinSupport;

import com.sun.source.util.TreePath;

/**
 * @author denis
 */
public abstract class Analyzer {

    protected Analyzer( HintContext context ) {
        myContext = context;
        myDescriptions = new LinkedList<>();
    }

    public abstract void analyze();

    protected boolean isEnabled() {
        VaadinSupport support = getSupport();
        return support != null;
    }

    protected VaadinSupport getSupport() {
        FileObject fileObject = getInfo().getFileObject();
        Project project = FileOwnerQuery.getOwner(fileObject);
        if (project == null) {
            return null;
        }
        VaadinSupport support = project.getLookup().lookup(VaadinSupport.class);
        if (support == null || !support.isEnabled()) {
            return null;
        }
        return support;
    }

    protected boolean isCanceled() {
        return myContext.isCanceled();
    }

    protected CompilationInfo getInfo() {
        return myContext.getInfo();
    }

    protected TypeElement getType() {
        if (myContext == null) {
            return null;
        }
        CompilationInfo info = myContext.getInfo();
        TreePath path = myContext.getPath();
        Element element = info.getTrees().getElement(path);
        if (element instanceof TypeElement) {
            return (TypeElement) element;
        }
        else {
            return null;
        }
    }

    protected Severity getSeverity( Severity defaultValue ) {
        return myContext == null ? defaultValue : myContext.getSeverity();
    }

    protected Collection<ErrorDescription> getDescriptions() {
        return myDescriptions;
    }

    private HintContext myContext;

    private List<ErrorDescription> myDescriptions;
}
