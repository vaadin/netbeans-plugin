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
package org.vaadin.netbeans.maven.directory;

import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.openide.awt.HtmlBrowser;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
import org.vaadin.netbeans.maven.editor.completion.AddOn;

/**
 * @author denis
 */
class AddOnDocPane extends JEditorPane {

    private static final String START_A = "<a href='"; // NOI18N

    private static final String CLOSE_ATTR_VALUE_TAG = "'>"; // NOI18N

    private static final String CLOSE_A = "</a>"; // NOI18N

    private static final String CLOSE_P = CLOSE_A + "</p>"; // NOI18N

    private static final String PERMALINK =
            "</p><p><b>Permalink to this add-on:</b><br><a href='"; // NOI18N

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

    private static final String PREFIX_NAME = "<font size='+2'><b>"; // NOI18N

    private static final String CLOSE_TABLE = "</td></tr></table>"; // NOI18N

    AddOnDocPane() {
        addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate( HyperlinkEvent e ) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    URL url = e.getURL();
                    HtmlBrowser.URLDisplayer.getDefault().showURL(url);
                }
            }
        });
    }

    void setAddOn( AddOn addOn ) {
        if (addOn == null) {
            setText("");
        }
        else {
            setText(getHtml(addOn));
        }
    }

    private String getHtml( AddOn addOn ) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX_NAME);
        builder.append(EscapeUtils.escapeHtml(addOn.getName()));
        builder.append(SUFFIX_NAME);

        builder.append(TABLE_PROPERTIES);
        builder.append(EscapeUtils.escapeHtml(addOn.getMavenVersion()));
        builder.append(MATURITY_ROW);
        builder.append(addOn.getMaturity());

        builder.append(LICENSE_ROW);
        for (License license : addOn.getLicenses()) {
            builder.append(START_A);
            builder.append(EscapeUtils.escapeAttribute(license.getUrl()));
            builder.append(CLOSE_ATTR_VALUE_TAG);
            builder.append(EscapeUtils.escapeAttribute(license.getName()));
            builder.append(CLOSE_A);
            builder.append(", "); // NOI18N
        }
        if (addOn.getLicenses().size() > 0) {
            builder.replace(builder.length() - 2, builder.length(), "");
        }
        builder.append(RATING_ROW);
        builder.append(EscapeUtils.escapeHtml(addOn.getRating()));
        builder.append(CLOSE_TABLE);

        builder.append(OVERVIEW);
        builder.append(EscapeUtils.escapeHtml(addOn.getDescription()));

        builder.append(PERMALINK);
        builder.append(EscapeUtils.escapeAttribute(addOn.getUrl()));
        builder.append(CLOSE_ATTR_VALUE_TAG); // NOI18N
        builder.append(EscapeUtils.escapeHtml(addOn.getUrl()));
        builder.append(CLOSE_P);

        return builder.toString();
    }
}
