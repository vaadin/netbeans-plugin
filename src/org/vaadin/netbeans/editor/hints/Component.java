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
import org.vaadin.netbeans.editor.analyzer.ComponentAnalyzer;

import com.sun.source.tree.Tree.Kind;

/**
 * @author denis
 */

public class Component {

    @Hint(displayName = "#DN_ComponentHasNoConnector",
            description = "#DESC_ComponentHasNoConnector", category = "vaadin",
            options = Options.QUERY, severity = Severity.HINT,
            hintKind = Hint.Kind.ACTION)
    @TriggerTreeKind(Kind.CLASS)
    public static ErrorDescription checkConnector( HintContext context ) {
        ComponentAnalyzer analyzer = new ComponentAnalyzer(context);
        analyzer.analyze();
        return analyzer.getNoConnector();
    }
}
