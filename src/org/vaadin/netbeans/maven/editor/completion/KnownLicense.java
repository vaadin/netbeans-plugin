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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openide.filesystems.FileUtil;

/**
 * @author denis
 */
public enum KnownLicense {

    APACHE_2_0("Apache License 2.0", "resources/apache-2.0.txt"), // NOI18N
    CVAL_2_0("CVAL 2.0", "resources/cval-2.0.txt"), // NOI18N
    CVAL_3_0("CVAL 3.0", "resources/cval-3.0.txt"), // NOI18N
    GPL_3_0("GPL 3.0 ", "resources/gpl-3.0.txt"), // NOI18N
    AGPL_3_0("AGPL 3.0 ", "resources/agpl-3.0.txt"), // NOI18N
    EPL_1_0("EPL 1.0", "resources/epl-1.0.txt"), ; // NOI18N

    private static final String UTF_8 = "UTF-8"; // NOI18N

    private KnownLicense( String name, String file ) {
        myName = name;
        myFileName = file;
    }

    String getName() {
        return myName;
    }

    String getText() {
        InputStream stream = KnownLicense.class.getResourceAsStream(myFileName);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            FileUtil.copy(stream, bytes);
            return bytes.toString(UTF_8);
        }
        catch (IOException e) {
            return null;
        }
    }

    static KnownLicense forString( String name ) {
        for (KnownLicense license : values()) {
            if (license.getName().equalsIgnoreCase(name)) {
                return license;
            }
        }
        return null;
    }

    private String myName;

    private String myFileName;
}
