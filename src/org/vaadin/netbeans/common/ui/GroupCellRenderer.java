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
package org.vaadin.netbeans.common.ui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.UIResource;

import org.netbeans.api.project.SourceGroup;

public class GroupCellRenderer extends JLabel implements ListCellRenderer,
        UIResource
{

    public GroupCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent( JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus )
    {
        if (!(value instanceof SourceGroup)) {
            return this;
        }
        // #89393: GTK needs name to render cell renderer "natively"
        setName("ComboBox.listRenderer"); // NOI18N

        setText(((SourceGroup) value).getDisplayName());
        setIcon(((SourceGroup) value).getIcon(false));

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }
}