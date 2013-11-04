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
package org.vaadin.netbeans.refactoring;

import org.netbeans.api.java.classpath.ClassPath;
import org.openide.filesystems.FileObject;
import org.vaadin.netbeans.refactoring.AbstractRefactoringPlugin.GwtModuleAcceptor;
import org.vaadin.netbeans.utils.XmlUtils;

/**
 * @author denis
 */
class GwtNameAcceptor implements GwtModuleAcceptor {

    GwtNameAcceptor( FileObject gwtXml ) {
        if (gwtXml == null) {
            return;
        }
        ClassPath classPath = ClassPath.getClassPath(gwtXml, ClassPath.SOURCE);
        String name = classPath.getResourceName(gwtXml, '.', true);
        if (name.endsWith(XmlUtils.GWT_XML)) {
            myGwtXmlFqn = name.substring(0,
                    name.length() - XmlUtils.GWT_XML.length());
        }
    }

    @Override
    public boolean accept( String moduleFqn ) {
        return moduleFqn != null && moduleFqn.equals(myGwtXmlFqn);
    }

    String getGwtFqn() {
        return myGwtXmlFqn;
    }

    private String myGwtXmlFqn;

}
