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
package org.vaadin.netbeans.editor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.vaadin.netbeans.editor.analyzer.ConnectorAnalyzer;
import org.vaadin.netbeans.editor.analyzer.RpcInterfacesAnalyzer;
import org.vaadin.netbeans.editor.analyzer.RpcRegistrationAnalyzer;
import org.vaadin.netbeans.editor.analyzer.SharedStateAnalyzer;
import org.vaadin.netbeans.editor.analyzer.ThemeAnalyzer;
import org.vaadin.netbeans.editor.analyzer.TypeAnalyzer;
import org.vaadin.netbeans.editor.analyzer.VaadinServletConfigurationAnalyzer;
import org.vaadin.netbeans.editor.analyzer.WebServletAnalyzer;
import org.vaadin.netbeans.editor.analyzer.GwtClassesAnalyzer;

/**
 * @author denis
 */
class VaadinScanTask {

    private static final List<TypeAnalyzer> ANALYZERS = new LinkedList<>();

    VaadinScanTask( CompilationInfo info, VaadinTaskFactory factory ) {
        myInfo = info;
        myDescriptions = new LinkedList<>();
        stop = new AtomicBoolean();
        myFactory = factory;
    }

    void run() {
        List<? extends TypeElement> classes = myInfo.getTopLevelElements();
        for (TypeElement clazz : classes) {
            if (stop.get()) {
                return;
            }
            handle(clazz);
        }
    }

    Collection<ErrorDescription> getDescriptions() {
        return myDescriptions;
    }

    void stop() {
        stop.set(true);
    }

    private void handle( TypeElement clazz ) {
        List<TypeElement> typess = ElementFilter.typesIn(clazz
                .getEnclosedElements());
        for (TypeElement typeElement : typess) {
            handle(typeElement);
        }
        doHandle(clazz);
    }

    private void doHandle( TypeElement clazz ) {
        for (TypeAnalyzer analyzer : ANALYZERS) {
            if (stop.get()) {
                return;
            }
            analyzer.analyze(clazz, myInfo, getDescriptions(), myFactory, stop);
        }
    }

    private final Collection<ErrorDescription> myDescriptions;

    private final AtomicBoolean stop;

    private final CompilationInfo myInfo;

    private final VaadinTaskFactory myFactory;

    static {
        ANALYZERS.add(new ThemeAnalyzer());
        ANALYZERS.add(new VaadinServletConfigurationAnalyzer());
        ANALYZERS.add(new WebServletAnalyzer());
        ANALYZERS.add(new ConnectorAnalyzer());
        ANALYZERS.add(new SharedStateAnalyzer());
        ANALYZERS.add(new RpcInterfacesAnalyzer());
        ANALYZERS.add(new GwtClassesAnalyzer());
        ANALYZERS.add(new RpcRegistrationAnalyzer());
    }
}
