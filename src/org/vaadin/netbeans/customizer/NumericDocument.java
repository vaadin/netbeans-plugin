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
package org.vaadin.netbeans.customizer;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * @author denis
 */
public class NumericDocument extends PlainDocument {

    public NumericDocument( JTextComponent textComponent, int length ) {
        myTextComponent = textComponent;
        myLength = length;
    }

    @Override
    public void insertString( int offs, String str, AttributeSet a )
            throws BadLocationException
    {
        if (myLength >= 0 && getLength() + str.length() > myLength) {
            str = str.substring(0, myLength - getLength());
            getComponent().getToolkit().beep();
        }

        try {
            Integer.parseInt(str);
        }
        catch (NumberFormatException e) {
            getComponent().getToolkit().beep();
            return;
        }

        super.insertString(offs, str, a);
    }

    protected JTextComponent getComponent() {
        return myTextComponent;
    }

    private JTextComponent myTextComponent;

    private int myLength;

}
