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

import java.text.ParseException;

import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author denis
 */
public class NumericSpinner extends JSpinner {

    public NumericSpinner( final int min ) {
        addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged( ChangeEvent e ) {
                if (((Integer) getValue()) <= min - 1) {
                    setValue(min);
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
