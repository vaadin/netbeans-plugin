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

import org.netbeans.lib.editor.util.StringEscapeUtils;

/**
 * @author denis
 */
public final class EscapeUtils {

    private static final String QUOTE = "'"; // NOI18N

    private static final String ESCAPED_QUOTE = "%27"; // NOI18N

    private EscapeUtils() {
    }

    public static String escapeHtml( String text ) {
        return StringEscapeUtils.escapeHtml(text);
    }

    public static String escapeAttribute( String text ) {
        String escaped = StringEscapeUtils.escapeHtml(text);
        return escaped.replaceAll(QUOTE, ESCAPED_QUOTE);
    }
}
