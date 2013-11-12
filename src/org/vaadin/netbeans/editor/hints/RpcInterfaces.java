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

import java.util.List;

import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.Hint.Options;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.vaadin.netbeans.editor.analyzer.AbstractJavaBeanAnalyzer.Mode;
import org.vaadin.netbeans.editor.analyzer.RpcInterfacesAnalyzer;

import com.sun.source.tree.Tree.Kind;

/**
 * @author denis
 */
public class RpcInterfaces {

    @Hint(displayName = "#DN_RpcParametersSer",
            description = "#DESC_RpcParametersSer", category = "vaadin",
            options = Options.QUERY, severity = Severity.WARNING)
    @TriggerTreeKind({ Kind.CLASS, Kind.INTERFACE })
    public static List<ErrorDescription> checkRpcParametersSerializability(
            HintContext context )
    {
        RpcInterfacesAnalyzer analyzer =
                new RpcInterfacesAnalyzer(context, Mode.SERIALIZABLE);
        analyzer.analyze();
        return analyzer.getNonSerializables();
    }

    @Hint(displayName = "#DN_RpcParametersJavaBeans",
            description = "#DESC_RpcParametersJavaBeans", category = "vaadin",
            options = Options.QUERY, severity = Severity.WARNING)
    @TriggerTreeKind({ Kind.CLASS, Kind.INTERFACE })
    public static List<ErrorDescription> checkRpcParametersJavaBeans(
            HintContext context )
    {
        RpcInterfacesAnalyzer analyzer =
                new RpcInterfacesAnalyzer(context, Mode.ACCESSORS);
        analyzer.analyze();
        return analyzer.getNoAccessors();
    }

    @Hint(displayName = "#DN_RpcParameterHasTypeVar",
            description = "#DESC_RpcParameterHasTypeVar", category = "vaadin",
            options = Options.QUERY, severity = Severity.ERROR)
    @TriggerTreeKind({ Kind.CLASS, Kind.INTERFACE })
    public static List<ErrorDescription> checkRpcParameterHasTypeVar(
            HintContext context )
    {
        RpcInterfacesAnalyzer analyzer = new RpcInterfacesAnalyzer(context);
        analyzer.analyze();
        return analyzer.getTypeVarParameterDeclarations();
    }

    @Hint(displayName = "#DN_RpcParameterHasWildcard",
            description = "#DESC_RpcParameterHasWildcard", category = "vaadin",
            options = Options.QUERY, severity = Severity.ERROR)
    @TriggerTreeKind({ Kind.CLASS, Kind.INTERFACE })
    public static List<ErrorDescription> checkRpcParameterHasWildcard(
            HintContext context )
    {
        RpcInterfacesAnalyzer analyzer = new RpcInterfacesAnalyzer(context);
        analyzer.analyze();
        return analyzer.getWildcardParameterDeclarations();
    }

    @Hint(displayName = "#DN_RpcDuplicateMethods",
            description = "#DESC_RpcDuplicateMethods", category = "vaadin",
            options = Options.QUERY, severity = Severity.ERROR)
    @TriggerTreeKind({ Kind.CLASS, Kind.INTERFACE })
    public static List<ErrorDescription> rpcDuplicateMethods(
            HintContext context )
    {
        RpcInterfacesAnalyzer analyzer = new RpcInterfacesAnalyzer(context);
        analyzer.analyze();
        return analyzer.getDuplicateRpcMethods();
    }

    @Hint(displayName = "#DN_RpcVoidReturn",
            description = "#DESC_RpcVoidReturn", category = "vaadin",
            options = Options.QUERY, severity = Severity.WARNING)
    @TriggerTreeKind({ Kind.CLASS, Kind.INTERFACE })
    public static List<ErrorDescription> rpcReturnType( HintContext context ) {
        RpcInterfacesAnalyzer analyzer = new RpcInterfacesAnalyzer(context);
        analyzer.analyze();
        return analyzer.getNonVoidMethods();
    }
}
