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
import org.vaadin.netbeans.editor.analyzer.RpcRegistrationAnalyzer;

import com.sun.source.tree.Tree.Kind;

/**
 * @author denis
 */
public class RpcUsage {

    @Hint(displayName = "#DN_ClientRpcInterface",
            description = "#DESC_ClientRpcInterface", category = "vaadin",
            options = Options.QUERY, severity = Severity.HINT,
            hintKind = Hint.Kind.ACTION)
    @TriggerTreeKind(Kind.CLASS)
    public static ErrorDescription checkClientRpc( HintContext context ) {
        RpcRegistrationAnalyzer analyzer =
                new RpcRegistrationAnalyzer(context, false);
        analyzer.analyze();
        return analyzer.getNoClientRpc();
    }

    @Hint(displayName = "#DN_ServerRpcInterface",
            description = "#DESC_ServerRpcInterface", category = "vaadin",
            options = Options.QUERY, severity = Severity.HINT,
            hintKind = Hint.Kind.ACTION,
            customizerProvider = UIServerRpcCustomizer.class)
    @TriggerTreeKind({ Kind.CLASS })
    public static ErrorDescription checkServerRpc( HintContext context ) {
        if (Analyzer.isEnabled(context, UIServerRpcCustomizer.SERVER_RPC_UI)) {
            RpcRegistrationAnalyzer analyzer =
                    new RpcRegistrationAnalyzer(context, false);
            analyzer.analyze();
            return analyzer.getNoServerRpc();
        }
        return null;
    }

    @Hint(displayName = "#DN_ClientRpcProxyInterface",
            description = "#DESC_ClientRpcProxyInterface", category = "vaadin",
            options = Options.QUERY, severity = Severity.HINT,
            hintKind = Hint.Kind.ACTION,
            customizerProvider = UIClientProxyRpcCustomizer.class)
    @TriggerTreeKind({ Kind.CLASS })
    public static ErrorDescription checkClientRpcProxy( HintContext context ) {
        if (Analyzer.isEnabled(context,
                UIClientProxyRpcCustomizer.CLIENT_PROXY_RPC_UI))
        {
            RpcRegistrationAnalyzer analyzer =
                    new RpcRegistrationAnalyzer(context, true);
            analyzer.analyze();
            return analyzer.getNoClientRpcProxy();
        }
        return null;
    }

    @Hint(displayName = "#DN_ServerRpcProxyInterface",
            description = "#DESC_ServerRpcProxyInterface", category = "vaadin",
            options = Options.QUERY, severity = Severity.HINT,
            hintKind = Hint.Kind.ACTION)
    @TriggerTreeKind({ Kind.CLASS })
    public static ErrorDescription checkServerRpcProxy( HintContext context ) {
        RpcRegistrationAnalyzer analyzer =
                new RpcRegistrationAnalyzer(context, true);
        analyzer.analyze();
        return analyzer.getNoServerRpcProxy();
    }

}
