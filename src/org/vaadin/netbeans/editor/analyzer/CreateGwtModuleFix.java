/**
 *
 */
package org.vaadin.netbeans.editor.analyzer;

import java.io.File;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.code.generator.JavaUtils;
import org.vaadin.netbeans.code.generator.XmlUtils;
import org.vaadin.netbeans.editor.VaadinTaskFactory;

/**
 * @author denis
 */
public class CreateGwtModuleFix implements Fix {

    public CreateGwtModuleFix( String widgetsetFqn, FileObject fileObject,
            VaadinTaskFactory factory )
    {
        myWidgetsetFqn = widgetsetFqn;
        myFactory = factory;
        myFileObject = fileObject;
    }

    @NbBundle.Messages({ "# {0} - moduleFqn",
            "createGwtModule=Create GWT Module {0}" })
    @Override
    public String getText() {
        return Bundle.createGwtModule(myWidgetsetFqn);
    }

    @Override
    public ChangeInfo implement() throws Exception {
        SourceGroup[] sourceGroups = JavaUtils
                .getJavaSourceGroups(FileOwnerQuery.getOwner(myFileObject));
        FileObject root = null;
        for (SourceGroup sourceGroup : sourceGroups) {
            root = sourceGroup.getRootFolder();
            if (FileUtil.isParentOf(myFileObject, root)) {
                break;
            }
        }
        if (root == null) {
            return null;
        }
        String widgetPath = myWidgetsetFqn.replace('.', '/');
        File file = new File(FileUtil.toFile(root), widgetPath);

        String widgetsetName = file.getName() + XmlUtils.GWT;

        file = file.getParentFile();
        FileObject folder = FileUtil.createFolder(file);
        if (folder != null) {
            XmlUtils.createGwtXml(folder, widgetsetName);
            myFactory.restart(myFileObject);
        }

        return null;
    }

    private String myWidgetsetFqn;

    private final FileObject myFileObject;

    private final VaadinTaskFactory myFactory;
}
