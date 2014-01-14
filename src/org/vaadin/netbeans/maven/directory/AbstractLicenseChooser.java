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
package org.vaadin.netbeans.maven.directory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.vaadin.netbeans.customizer.VaadinConfiguration;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;

/**
 * @author denis
 */
@NbBundle.Messages({ "ok=OK", "cancel=Cancel" })
public abstract class AbstractLicenseChooser {

    protected boolean requireAcceptFreeLicense() {
        return VaadinConfiguration.getInstance()
                .freeAddonRequiresConfirmation();
    }

    protected License getLicense( AbstractAddOn addon ) {
        List<License> licenses = addon.getLicenses();
        if (licenses.size() == 1) {
            if (acceptLicense(licenses.get(0))) {
                return licenses.get(0);
            }
        }
        else {
            Map<String, String> map = new LinkedHashMap<>();
            Map<String, License> licenseMap = new HashMap<>();
            for (License license : licenses) {
                map.put(license.getName(), license.getUrl());
                licenseMap.put(license.getName(), license);
            }
            String license = chooseLicense(map);
            if (license != null) {
                License selected = licenseMap.get(license);
                if (acceptLicense(selected)) {
                    return selected;
                }
            }
        }
        return null;
    }

    protected boolean acceptLicense( License license ) {
        if (!license.isFree() || requireAcceptFreeLicense()) {
            KnownLicense knownLicense =
                    KnownLicense.forString(license.getName());
            String text = null;
            if (knownLicense != null) {
                text = knownLicense.getText();
            }
            return confirm(text, license.getName(), license.getUrl());
        }
        return true;
    }

    @NbBundle.Messages("selectLicense=Select License")
    protected String chooseLicense( final Map<String, String> map ) {
        return Mutex.EVENT.readAccess(new Mutex.Action<String>() {

            @Override
            public String run() {
                JButton ok = new JButton(Bundle.ok());
                ok.setEnabled(false);
                LicenseSelectPanel panel = new LicenseSelectPanel(map, ok);
                JButton cancel = new JButton(Bundle.cancel());
                DialogDescriptor descriptor =
                        new DialogDescriptor(panel, Bundle.selectLicense(),
                                true, new Object[] { ok, cancel }, ok,
                                DialogDescriptor.DEFAULT_ALIGN, null, null);
                Object result = DialogDisplayer.getDefault().notify(descriptor);
                if (ok.equals(result)) {
                    return panel.getSelectedLicense();
                }
                else {
                    return null;
                }
            }

        });
    }

    @NbBundle.Messages("acceptLicense=Accept License Agreement")
    protected boolean confirm( final String text, final String name,
            final String url )
    {
        return Mutex.EVENT.readAccess(new Mutex.Action<Boolean>() {

            @Override
            public Boolean run() {
                JButton ok = new JButton(Bundle.ok());
                ok.setEnabled(false);
                Object panel = null;
                if (text == null) {
                    panel = new LicenseUrlConfirmationPanel(name, url, ok);
                }
                else {
                    panel = new LicenseConfirmationPanel(text, ok);
                }
                JButton cancel = new JButton(Bundle.cancel());
                DialogDescriptor descriptor =
                        new DialogDescriptor(panel, Bundle.acceptLicense(),
                                true, new Object[] { ok, cancel }, ok,
                                DialogDescriptor.DEFAULT_ALIGN, null, null);
                Object result = DialogDisplayer.getDefault().notify(descriptor);
                return ok.equals(result);
            }

        });
    }
}
