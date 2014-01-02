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

import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.vaadin.netbeans.editor.analyzer.ConnectorAnalyzer;

import com.sun.source.tree.Tree.Kind;

/**
 * @author denis
 */

public class Connector {

    @Hint(displayName = "#DN_Connector", description = "#DESC_Connector",
            category = "vaadin", options = Options.QUERY,
            severity = Severity.ERROR)
    @TriggerTreeKind(Kind.CLASS)
    public static ErrorDescription checkConnectorClass( HintContext context ) {
        ConnectorAnalyzer analyzer = new ConnectorAnalyzer(context, false);
        analyzer.analyze();
        return analyzer.getBadConnectorClass();
    }

    @Hint(displayName = "#DN_Connector", description = "#DESC_Connector",
            category = "vaadin", options = Options.QUERY,
            severity = Severity.ERROR)
    @TriggerTreeKind(Kind.CLASS)
    public static ErrorDescription checkConnectorValue( HintContext context ) {
        ConnectorAnalyzer analyzer = new ConnectorAnalyzer(context, false);
        analyzer.analyze();
        return analyzer.getBadConnectValue();
    }

    @Hint(displayName = "#DN_Connection", description = "#DESC_Connection",
            category = "vaadin", options = Options.QUERY,
            severity = Severity.WARNING)
    @TriggerTreeKind(Kind.CLASS)
    public static ErrorDescription checkConnection( HintContext context ) {
        ConnectorAnalyzer analyzer = new ConnectorAnalyzer(context, false);
        analyzer.analyze();
        return analyzer.getNoConnectAnnotation();
    }
}
