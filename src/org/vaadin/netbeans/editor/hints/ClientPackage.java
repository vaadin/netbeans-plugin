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

import java.util.ArrayList;
import java.util.List;

import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.vaadin.netbeans.editor.analyzer.AbstractJavaBeanAnalyzer.Mode;
import org.vaadin.netbeans.editor.analyzer.ClientClassAnalyzer;
import org.vaadin.netbeans.editor.analyzer.ConnectorAnalyzer;
import org.vaadin.netbeans.editor.analyzer.GwtClassesAnalyzer;
import org.vaadin.netbeans.editor.analyzer.SharedStateAnalyzer;

import com.sun.source.tree.Tree.Kind;

/**
 * @author denis
 */
public class ClientPackage {

    @Hint(displayName = "#DN_ClientPackage",
            description = "#DESC_ClientPackage", category = "vaadin",
            options = Options.QUERY, severity = Severity.ERROR)
    @TriggerTreeKind({ Kind.CLASS, Kind.INTERFACE })
    public static ErrorDescription checkClientPackage( HintContext context ) {
        for (ClientClassAnalyzer analyzer : getPackageAnalyzers(context)) {
            analyzer.analyze();
            if (analyzer.getNotClientPackage() != null) {
                return analyzer.getNotClientPackage();
            }
        }
        return null;
    }

    @Hint(displayName = "#DN_ClientNoGwtModule",
            description = "#DESC_ClientNoGwtModule", category = "vaadin",
            options = Options.QUERY, severity = Severity.ERROR)
    @TriggerTreeKind({ Kind.CLASS, Kind.INTERFACE })
    public static ErrorDescription checkGwtModule( HintContext context ) {
        for (ClientClassAnalyzer analyzer : getPackageAnalyzers(context)) {
            analyzer.analyze();
            if (analyzer.getNoGwtModule() != null) {
                return analyzer.getNoGwtModule();
            }
        }
        return null;
    }

    private static List<ClientClassAnalyzer> getPackageAnalyzers(
            HintContext context )
    {
        List<ClientClassAnalyzer> analyzers = new ArrayList<>();
        analyzers.add(new ConnectorAnalyzer(context, true));
        analyzers.add(new GwtClassesAnalyzer(context));
        analyzers.add(new SharedStateAnalyzer(context, Mode.PACKAGE));
        return analyzers;
    }
}
