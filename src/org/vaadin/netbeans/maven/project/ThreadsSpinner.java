/**
 *
 */
package org.vaadin.netbeans.maven.project;

import java.text.ParseException;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author denis
 */
public class ThreadsSpinner extends JSpinner {

    ThreadsSpinner() {
        addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged( ChangeEvent e ) {
                if (((Integer) getValue()) <= 0) {
                    setValue(1);
                }
            }
        });
    }

    @Override
    public void commitEdit() {
        try {
            super.commitEdit();
        }
        catch (ParseException e) {
            setValue(0);
        }
    }

}
