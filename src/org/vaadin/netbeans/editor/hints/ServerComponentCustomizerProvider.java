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

import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.netbeans.spi.java.hints.CustomizerProvider;
import org.vaadin.netbeans.editor.analyzer.ui.UIComponentHint;

/**
 * @author denis
 */
class ServerComponentCustomizerProvider implements CustomizerProvider {

    ServerComponentCustomizerProvider( String uiPreferenceKey ) {
        myUiKey = uiPreferenceKey;
    }

    @Override
    public JComponent getCustomizer( Preferences prefs ) {
        return new UIComponentHint(prefs, myUiKey);
    }

    private String myUiKey;

}
