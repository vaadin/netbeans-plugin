/**
 *
 */
package org.vaadin.netbeans.editor.analyzer;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

/**
 * @author denis
 */
public interface TypeAnalyzer {

    void analyze( TypeElement type, CompilationInfo info,
            Collection<ErrorDescription> descriptions,
            VaadinTaskFactory factory, AtomicBoolean cancel );
}
