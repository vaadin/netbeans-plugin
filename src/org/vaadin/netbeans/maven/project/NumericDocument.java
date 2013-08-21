/**
 *
 */
package org.vaadin.netbeans.maven.project;

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
            myTextComponent.getToolkit().beep();
        }

        try {
            Integer.parseInt(str);
        }
        catch (NumberFormatException e) {
            myTextComponent.getToolkit().beep();
            return;
        }

        super.insertString(offs, str, a);
    }

    private JTextComponent myTextComponent;

    private int myLength;

}
