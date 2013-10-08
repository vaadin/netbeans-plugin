package org.vaadin.netbeans.editor.analyzer;

import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 * @author denis
 */
public class CreateClientRpcFix extends AbstractJavaFix {

    private static final String CLIENT_RPC_TEMPLATE = "Templates/Vaadin/ClientRpcStub.java"; // NOI18N

    protected CreateClientRpcFix( FileObject fileObject,
            ElementHandle<TypeElement> handle )
    {
        super(fileObject);
        myHandle = handle;
    }

    @NbBundle.Messages("createClientRpc=Create Client RPC interface and use it")
    @Override
    public String getText() {
        return Bundle.createClientRpc();
    }

    @Override
    public ChangeInfo implement() throws Exception {
        return null;
    }

    private ElementHandle<TypeElement> myHandle;

}
