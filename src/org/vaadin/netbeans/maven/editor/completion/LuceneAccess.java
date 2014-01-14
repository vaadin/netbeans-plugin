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
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * @author denis
 */
abstract class LuceneAccess {

    LuceneAccess() {
        myLock = new ReentrantReadWriteLock();

    }

    protected abstract Logger getLogger();

    protected abstract File getIndexDir();

    protected abstract Collection<Document> createDocument( AddOn addon, int id );

    protected Fieldable createStoredField( String key, String value ) {
        return new StoredField(key, value);
    }

    protected Fieldable createIndexedField( String key, String value,
            boolean stored )
    {
        return new IndexedField(key, value, stored);
    }

    protected Fieldable createIndexedField( String key, String value ) {
        return new IndexedField(key, value);
    }

    protected boolean add( Document doc, Fieldable field ) {
        if (field instanceof StoredField) {
            StoredField stored = (StoredField) field;
            if (!stored.isEmpty()) {
                doc.add(field);
                return true;
            }
        }
        else {
            doc.add(field);
            return true;
        }
        return false;
    }

    protected ReentrantReadWriteLock getLock() {
        return myLock;
    }

    protected String getField( String name, Document document ) {
        return document.get(name);
    }

    protected void doIndex( Collection<AddOn> addons ) {
        Directory directory = null;
        IndexWriter writer = null;
        Analyzer analyzer = null;
        try {
            if (!getIndexDir().exists() && !getIndexDir().mkdirs()) {
                getLogger().log(Level.WARNING,
                        "Unable to create index directory"); // NOI18N
                return;
            }
            analyzer = new StandardAnalyzer(Version.LUCENE_35);

            directory = FSDirectory.open(getIndexDir());
            IndexWriterConfig conf =
                    new IndexWriterConfig(Version.LUCENE_35, analyzer);
            conf.setOpenMode(OpenMode.CREATE);
            writer = new IndexWriter(directory, conf);
            writer.deleteAll();

            int i = 0;
            for (AddOn addon : addons) {
                Collection<Document> docs = createDocument(addon, i);
                for (Document document : docs) {
                    writer.addDocument(document);
                }
                writer.commit();
                i++;
            }
        }
        catch (IOException e) {
            getLogger().log(Level.INFO, null, e);
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    getLogger().log(Level.FINE, null, e);
                }
            }
            if (directory != null) {
                try {
                    directory.close();
                }
                catch (IOException e) {
                    getLogger().log(Level.FINE, null, e);
                }
            }
            if (analyzer != null) {
                analyzer.close();
            }
        }
    }

    protected void closeReadIndex( IndexReader reader, IndexSearcher searcher,
            Directory directory, Analyzer analyzer )
    {
        if (reader != null) {
            try {
                reader.close();
            }
            catch (IOException e) {
                getLogger().log(Level.FINE, null, e);
            }
        }
        if (searcher != null) {
            try {
                searcher.close();
            }
            catch (IOException e) {
                getLogger().log(Level.FINE, null, e);
            }
        }
        if (directory != null) {
            try {
                directory.close();
            }
            catch (IOException e) {
                getLogger().log(Level.FINE, null, e);
            }
        }
        if (analyzer != null) {
            analyzer.close();
        }
    }

    private ReentrantReadWriteLock myLock;

    private static class StoredField extends AbstractField {

        StoredField( String name, String value ) {
            this.name = name;
            fieldsData = value;

            isStored = true;
        }

        @Override
        public Reader readerValue() {
            return null;
        }

        @Override
        public String stringValue() {
            return fieldsData.toString();
        }

        @Override
        public TokenStream tokenStreamValue() {
            return null;
        }

        boolean isEmpty() {
            return fieldsData == null;
        }

    }

    private static class IndexedField extends StoredField {

        IndexedField( String name, String value ) {
            super(name, value);

            isIndexed = true;
            isTokenized = true;
        }

        IndexedField( String name, String value, boolean stored ) {
            this(name, value);

            isStored = stored;
        }

    }
}
