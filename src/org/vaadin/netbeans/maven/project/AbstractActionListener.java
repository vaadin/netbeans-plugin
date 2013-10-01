/**
 *
 */
package org.vaadin.netbeans.maven.project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;

import javax.swing.SwingUtilities;

import org.openide.util.RequestProcessor;

abstract class AbstractActionListener implements ActionListener, Runnable {

    private static final ExecutorService REQUEST_PROCESSOR = new RequestProcessor(
            VaadinAction.class);

    @Override
    public void actionPerformed( ActionEvent e ) {
        if (SwingUtilities.isEventDispatchThread()) {
            REQUEST_PROCESSOR.execute(this);
        }
        else {
            run();
        }
    }
}