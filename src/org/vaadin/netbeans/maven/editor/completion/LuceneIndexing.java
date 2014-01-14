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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.openide.modules.Places;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.License;
import org.vaadin.netbeans.maven.editor.completion.AbstractAddOn.Maturity;
import org.vaadin.netbeans.maven.editor.completion.SourceClass.SourceType;
import org.vaadin.netbeans.retriever.AbstractRetriever;

/**
 * @author denis
 */
abstract class LuceneIndexing extends LuceneAccess {

    private static final String ADD_ON_INDEX = "addOnIndex"; // NOI18N

    LuceneIndexing() {
        File vaadinFolder =
                Places.getCacheSubdirectory(AbstractRetriever.VAADIN);
        myIndexDir = new File(vaadinFolder, ADD_ON_INDEX);
    }

    @Override
    protected File getIndexDir() {
        return myIndexDir;
    }

    @Override
    protected Collection<Document> createDocument( AddOn addon, int id ) {
        List<Document> result = new LinkedList<>();

        List<SourceClass> classes = addon.getClasses();
        for (SourceClass clazz : classes) {
            result.add(createClassDocument(addon, clazz, id));
        }

        List<License> licenses = addon.getLicenses();

        boolean hasMavenArtifactsDefined = false;
        boolean hasFree = false;
        boolean hasCommercial = false;

        for (License license : licenses) {
            if (license.isFree()) {
                hasFree = true;
            }
            else {
                hasCommercial = true;
            }
            Document document = createLicenseDocument(addon, license, id);
            if (document != null) {
                result.add(document);
                hasMavenArtifactsDefined = true;
            }
        }

        if (hasMavenArtifactsDefined) {
            Document doc = createInfoDocument(addon, id);
            add(doc,
                    createIndexedField(QuerySupport.HAS_FREE_LICENSE,
                            String.valueOf(hasFree)));
            add(doc,
                    createIndexedField(QuerySupport.HAS_COMMERCIAL_LICENSE,
                            String.valueOf(hasCommercial)));
            result.add(doc);
        }
        else {
            return Collections.emptyList();
        }

        return result;
    }

    protected int getSessionSeq() {
        return mySessionSeq;
    }

    void index( Collection<AddOn> addons ) {
        getLock().writeLock().lock();
        try {
            nextSessionSeq();
            doIndex(addons);
        }
        finally {
            getLock().writeLock().unlock();
        }
    }

    boolean isInitialized() {
        return myIndexDir.exists();
    }

    private Document createInfoDocument( AddOn addon, int id ) {
        Document doc = new Document();

        String artifactId = addon.getArtifactId();
        String groupId = addon.getGroupId();
        String description = addon.getDescription();
        Maturity maturity = addon.getMaturity();
        String version = addon.getMavenVersion();
        String rating = addon.getRating();
        String url = addon.getUrl();
        String lastUpdate = addon.getLastUpdate();

        add(doc, createIndexedField(QuerySupport.NAME, addon.getName()));
        add(doc,
                createIndexedField(QuerySupport.NAME_HASH,
                        QuerySupport.getHash(addon.getName()), false));
        add(doc, createIndexedField(QuerySupport.ID, String.valueOf(id)));
        add(doc, createStoredField(QuerySupport.ARTIFACT_ID, artifactId));
        add(doc, createStoredField(QuerySupport.GROUP_ID, groupId));
        add(doc, createIndexedField(QuerySupport.DESCRIPTION, description));
        add(doc,
                createIndexedField(QuerySupport.MATURITY,
                        maturity == null ? null : maturity.toString()));
        add(doc, createStoredField(QuerySupport.VERSION, version));
        add(doc, createStoredField(QuerySupport.RATING, rating));
        add(doc, createStoredField(QuerySupport.URL, url));
        add(doc, createStoredField(QuerySupport.LAST_UPDATE, lastUpdate));
        add(doc,
                createIndexedField(QuerySupport.DOC_TYPE,
                        QuerySupport.DocType.INFO.toString()));

        return doc;
    }

    private Document createLicenseDocument( AddOn addon, License license, int id )
    {
        String name = license.getName();
        String url = license.getUrl();

        String artifactId = license.getArtifactId();
        String groupId = license.getGroupId();
        boolean isFree = license.isFree();

        Document doc = new Document();

        add(doc, createIndexedField(QuerySupport.ID, String.valueOf(id), false));
        add(doc, createStoredField(QuerySupport.NAME, name));
        add(doc, createStoredField(QuerySupport.URL, url));
        if (!add(doc, createStoredField(QuerySupport.ARTIFACT_ID, artifactId))
                && addon.getArtifactId() == null)
        {
            return null;
        }
        if (!add(doc, createStoredField(QuerySupport.GROUP_ID, groupId))
                && addon.getGroupId() == null)
        {
            return null;
        }
        add(doc,
                createIndexedField(QuerySupport.FREE, Boolean.valueOf(isFree)
                        .toString()));
        add(doc,
                createIndexedField(QuerySupport.DOC_TYPE,
                        QuerySupport.DocType.LICENSE.toString()));

        return doc;
    }

    private Document createClassDocument( AddOn addon, SourceClass clazz, int id )
    {
        String name = clazz.getName();
        String fqn = clazz.getQualifiedName();
        SourceType type = clazz.getType();
        Document doc = new Document();

        add(doc, createIndexedField(QuerySupport.ID, String.valueOf(id)));
        add(doc, createStoredField(QuerySupport.NAME, addon.getName()));
        add(doc, createIndexedField(QuerySupport.CLASS_NAME, name));
        add(doc, createIndexedField(QuerySupport.CLASS_FQN, fqn));
        add(doc,
                createIndexedField(QuerySupport.DOC_TYPE,
                        QuerySupport.DocType.CLASS.toString()));
        add(doc,
                createIndexedField(QuerySupport.SOURCE_TYPE,
                        type == null ? null : type.toString()));
        return doc;
    }

    private int nextSessionSeq() {
        assert getLock().isWriteLockedByCurrentThread();
        mySessionSeq++;
        return mySessionSeq;
    }

    private int mySessionSeq;

    private final File myIndexDir;

}
