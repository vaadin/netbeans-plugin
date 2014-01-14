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
package org.vaadin.netbeans.maven.editor.completion;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Action;

import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.vaadin.netbeans.maven.directory.EscapeUtils;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;

/**
 * @author denis
 */
class AddonCompletionDoc implements CompletionDocumentation {

    private static final String CLOSE_P = "</p>"; // NOI18N

    private static final String PERMALINK =
            "</p><p><b>Permalink to this add-on:</b><br>"; // NOI18N

    private static final String OVERVIEW = "<p><b>Overview</b><br>"; // NOI18N

    private static final String RATING_ROW =
            "</td></tr><tr><td  color='rgb(140, 139, 126)'>Rating</td>"
                    + "<td style='padding-left:15px'>"; // NOI18N

    private static final String LICENSE_ROW =
            "</td></tr><tr><td  color='rgb(140, 139, 126)'>License</td>"
                    + "<td style='padding-left:15px;color:rgb(102, 210, 246);'>"; // NOI18N

    private static final String MATURITY_ROW =
            "</td></tr><tr><td color='rgb(140, 139, 126)'>Maturity</td>"
                    + "<td style='padding-left:15px;color:rgb(102, 210, 246);'>"; // NOI18N

    private static final String TABLE_PROPERTIES =
            "<table><tr><td>Version</td><td style='padding-left:15px'>"; // NOI18N

    private static final String SUFFIX_NAME = "</b> Vaadin Add-on</font></p>"; // NOI18N

    private static final String PREFIX_NAME = "<p><font size='+2'><b>"; // NOI18N

    private static final String CLOSE_TABLE = "</td></tr></table>"; // NOI18N

    private static final String TD_TD = "</td><td>"; // NOI18N

    private static final String VAADIN_LOGO =
            "<p><img "
                    + "src='nbresloc:/org/vaadin/netbeans/resources/vaadin-logo-color.png'>"
                    + CLOSE_P; // NOI18N

    private static final String PKG_PREFIX =
    /*"<table width='100%'><tr><td>"*/"<p><font size='+0'><b>"; // NOI18N

    private static final String PKG_SUFFIX = "</b></font></p>"; // NOI18N

    private static final String CLASS_PREFIX =
    /*"<br>"*/"<p><tt>public class "; // NOI18N

    private static final String CLASS_SUFFIX = "</tt></p>"; // NOI18N

    AddonCompletionDoc( AddOnClass clazz ) {
        myClass = clazz;

        AddOnDoc doc = AddOnProvider.getInstance().getDoc(clazz);
        if (doc != null) {
            myText = buildText(doc);
            myUrl = doc.getUrl();
        }
    }

    @Override
    public Action getGotoSourceAction() {
        return null;
    }

    @Override
    public String getText() {
        return myText;
    }

    @Override
    public URL getURL() {
        try {
            return new URL(myUrl);
        }
        catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public CompletionDocumentation resolveLink( String link ) {
        return null;
    }

    private String buildText( AddOnDoc doc ) {
        String pkg = myClass.getQualifiedName();
        if (pkg.endsWith(myClass.getName())) {
            pkg = pkg.substring(0, pkg.length() - myClass.getName().length());
        }
        if (pkg.endsWith(".")) {
            pkg = pkg.substring(0, pkg.length() - 1);
        }
        StringBuilder builder = new StringBuilder(PKG_PREFIX);
        builder.append(pkg);
        builder.append(PKG_SUFFIX);
        builder.append(CLASS_PREFIX);
        builder.append(myClass.getName());
        builder.append(CLASS_SUFFIX);

        //builder.append(TD_TD);

        //builder.append(VAADIN_LOGO);

        //builder.append(CLOSE_TABLE);

        builder.append(PREFIX_NAME);
        builder.append(EscapeUtils.escapeHtml(doc.getName()));
        builder.append(SUFFIX_NAME);

        builder.append(TABLE_PROPERTIES);
        builder.append(EscapeUtils.escapeHtml(doc.getMavenVersion()));
        builder.append(MATURITY_ROW);
        builder.append(doc.getMaturity());

        builder.append(LICENSE_ROW);
        for (License license : doc.getLicenses()) {
            builder.append(license.getName());
            builder.append(", "); // NOI18N
        }
        if (doc.getLicenses().size() > 0) {
            builder.replace(builder.length() - 2, builder.length(), "");
        }
        builder.append(RATING_ROW);
        builder.append(EscapeUtils.escapeHtml(doc.getRating()));
        builder.append(CLOSE_TABLE);

        builder.append(OVERVIEW);
        builder.append(EscapeUtils.escapeHtml(doc.getDescription()));

        builder.append(PERMALINK);
        builder.append(EscapeUtils.escapeHtml(doc.getUrl()));
        builder.append(CLOSE_P);

        return builder.toString();
    }

    private AddOnClass myClass;

    private String myUrl;

    private String myText = "";
}
