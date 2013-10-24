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

package org.vaadin.netbeans.model.gwt;

import org.netbeans.modules.xml.xam.ModelSource;
import org.vaadin.netbeans.model.gwt.impl.GwtModelImpl;

/**
 * @author denis
 */
public final class GwtModelFactory extends
        org.netbeans.modules.xml.xam.AbstractModelFactory<GwtModel>
{

    private static final GwtModelFactory INSTANCE = new GwtModelFactory();

    private GwtModelFactory() {
    }

    public static GwtModelFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public GwtModel getModel( ModelSource source ) {
        return super.getModel(source);
    }

    @Override
    protected GwtModel createModel( ModelSource source ) {
        return new GwtModelImpl(source);
    }

}
