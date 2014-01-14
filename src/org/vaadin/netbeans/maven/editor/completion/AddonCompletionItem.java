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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.vaadin.netbeans.maven.directory.AbstractLicenseChooser;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;
import org.vaadin.netbeans.utils.POMUtils;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;

/**
 * @author denis
 */
class AddonCompletionItem extends AbstractLicenseChooser implements
        CompletionItem
{

    private static final String TEST = "test"; // NOI18N

    private static final String COMPILE = "compile"; // NOI18N

    private static final String CLASS_FONT = "<font color=#00b4f0>"; // NOI18N

    private static final String CLOSE_FONT = "</font>"; // NOI18N

    private static final String PKG_FONT = "<font color=#aad9d9>"; // NOI18N

    private static ImageIcon ICON = createIcon();

    private static final RequestProcessor REQUEST_PROCESSOR =
            new RequestProcessor(AddonCompletionItem.class);

    AddonCompletionItem( int offset, AddOnClass clazz ) {
        myClass = clazz;
        myOffset = offset;
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return new AsyncCompletionTask(new AddonDocQuery(myClass),
                EditorRegistry.lastFocusedComponent());
    }

    @Override
    public CompletionTask createToolTipTask() {
        return null;
    }

    @Override
    public void defaultAction( JTextComponent component ) {
        if (component != null) {
            Completion.get().hideDocumentation();
            Completion.get().hideCompletion();
            int caretOffset = component.getSelectionEnd();

            if (addDependency(component.getDocument())) {
                substituteText(component, myOffset, caretOffset - myOffset);
                addImport(component.getDocument(), myClass.getQualifiedName());
            }
        }
    }

    @Override
    public CharSequence getInsertPrefix() {
        return myClass.getName();
    }

    @Override
    public int getPreferredWidth( Graphics graphics, Font font ) {
        return CompletionUtilities.getPreferredWidth(getLeftHtmlText(),
                getRightHtmlText(), graphics, font);
    }

    @Override
    public int getSortPriority() {
        /*
         *  700 corresponds to common case of Java Class completion items 
         *  (LazyTypeCompletionItem) which are used in plain Java CC.
         */
        return 700;
    }

    @Override
    public CharSequence getSortText() {
        return myClass.getName();
    }

    @Override
    public boolean instantSubstitution( JTextComponent component ) {
        return false;
    }

    @Override
    public void processKeyEvent( KeyEvent event ) {
    }

    @Override
    public void render( Graphics graphics, Font font, Color color,
            Color bgColor, int width, int height, boolean selected )
    {
        CompletionUtilities.renderHtml(ICON, getLeftHtmlText(),
                getRightHtmlText(), graphics, font, color, width, height,
                selected);

    }

    AddOnClass getAddOnClass() {
        return myClass;
    }

    private void substituteText( JTextComponent component, final int offset,
            final int length )
    {
        final BaseDocument document = (BaseDocument) component.getDocument();
        final String text = getInsertPrefix().toString();
        if (text != null) {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    try {
                        String textToReplace = document.getText(offset, length);
                        if (text.equals(textToReplace)) {
                            return;
                        }
                        Position position = document.createPosition(offset);
                        document.remove(offset, length);
                        document.insertString(position.getOffset(), text, null);
                    }
                    catch (BadLocationException ignore) {
                    }
                }
            };
            document.runAtomic(runnable);
        }
    }

    @NbBundle.Messages({ "# {0} - plugin",
            "addOnIsNotFound=Add-On ''{0}'' is not found in Vaadin directory" })
    private boolean addDependency( final Document document ) {
        final License license =
                getLicense(AddOnProvider.getInstance().getDoc(myClass));
        if (license == null) {
            return false;
        }

        REQUEST_PROCESSOR.post(new Runnable() {

            @Override
            public void run() {
                String scope = COMPILE;
                if (SourceType.TEST.equals(myClass.getType())) {
                    scope = TEST;
                }
                FileObject fileObject =
                        NbEditorUtilities.getFileObject(document);
                Project project = FileOwnerQuery.getOwner(fileObject);

                POMUtils.addDependency(project, license.getGroupId(),
                        license.getArtifactId(), license.getVersion(), scope);
            }
        });
        return true;
    }

    private void addImport( Document document, String fqn ) {
        JavaSource javaSource = JavaSource.forDocument(document);
        if (javaSource == null) {
            return;
        }
        try {
            ModificationResult task =
                    javaSource.runModificationTask(new Task<WorkingCopy>() {

                        @Override
                        public void run( WorkingCopy copy ) throws Exception {
                            copy.toPhase(Phase.ELEMENTS_RESOLVED);

                            TreeMaker treeMaker = copy.getTreeMaker();
                            ImportTree tree =
                                    treeMaker.Import(treeMaker
                                            .QualIdent(myClass
                                                    .getQualifiedName()), false);
                            CompilationUnitTree newTree =
                                    treeMaker.addCompUnitImport(
                                            copy.getCompilationUnit(), tree);
                            copy.rewrite(copy.getCompilationUnit(), newTree);
                        }
                    });
            task.commit();
        }
        catch (IOException e) {
            Logger.getLogger(AddonCompletionItem.class.getName()).log(
                    Level.INFO, null, e);
        }
    }

    @NbBundle.Messages({ "# {0} - addon name", "addonInfo=<b>{0}</b> Add-On" })
    private String getRightHtmlText() {
        return Bundle.addonInfo(myClass.getAddOnName());
    }

    private String getLeftHtmlText() {
        String pkg = myClass.getQualifiedName();
        if (pkg.endsWith(myClass.getName())) {
            pkg = pkg.substring(0, pkg.length() - myClass.getName().length());
        }
        if (pkg.endsWith(".")) { // NOI18N
            pkg = pkg.substring(0, pkg.length() - 1);
        }
        StringBuilder builder = new StringBuilder(CLASS_FONT);
        builder.append(myClass.getName());
        builder.append(CLOSE_FONT);
        builder.append(' ');
        builder.append(PKG_FONT);
        builder.append('(');
        builder.append(pkg);
        builder.append(')');
        builder.append(CLOSE_FONT);
        return builder.toString();
    }

    private static ImageIcon createIcon() {
        ImageIcon vaadin =
                ImageUtilities.loadImageIcon(
                        "org/vaadin/netbeans/maven/ui/resources/vaadin.png", // NOI18N
                        false);
        ImageIcon classIcon =
                ImageUtilities.loadImageIcon(
                        "org/vaadin/netbeans/resources/disabled-class.png", // NOI18N
                        false);
        return new ImageIcon(ImageUtilities.mergeImages(classIcon.getImage(),
                vaadin.getImage(), 0, 0));
    }

    private int myOffset;

    private AddOnClass myClass;

}
