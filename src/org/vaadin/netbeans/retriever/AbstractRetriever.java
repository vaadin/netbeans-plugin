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
package org.vaadin.netbeans.retriever;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openide.filesystems.FileUtil;
import org.openide.modules.Places;

/**
 * @author denis
 */
public abstract class AbstractRetriever {

    public static final String VAADIN = "vaadin"; // NOI18N

    protected static final String UTF_8 = "UTF-8"; // NOI18N

    private static final Logger LOG = Logger.getLogger(AbstractRetriever.class
            .getName()); // NOI18N  

    protected AbstractRetriever() {
    }

    protected void requestData() {
        InputStream inputStream = null;
        try {
            URL url = new URL(getUrl());
            URLConnection connection = url.openConnection();
            connection.connect();
            inputStream = connection.getInputStream();
            File temp = File.createTempFile(VAADIN, getCachedFileName());
            temp.deleteOnExit();
            save(inputStream, temp);

            File versionsFile = getCachedFile();
            versionsFile.createNewFile();
            copy(temp, versionsFile);
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    LOG.log(Level.INFO, null, e);
                }
            }
        }
    }

    protected abstract String getCachedFileName();

    protected abstract String getUrl();

    protected abstract Object getFileLock( File file );

    protected File getCachedFile() {
        File cache = Places.getCacheSubdirectory(VAADIN);
        File versionsFile = new File(cache, getCachedFileName());
        return versionsFile;
    }

    protected void save( InputStream inputStream, File file, boolean lock )
            throws IOException
    {
        if (lock) {
            synchronized (getFileLock(file)) {
                save(inputStream, file);
            }
        }
        else {
            save(inputStream, file);
        }
    }

    protected void initCache( InputStream stream ) {
        File cached = getCachedFile();
        try {
            synchronized (getFileLock(cached)) {
                if (!cached.exists()) {
                    cached.createNewFile();
                    save(stream, cached);
                }
            }
        }
        catch (IOException e) {
            LOG.log(Level.INFO, null, e);
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException e) {
                    LOG.log(Level.INFO, null, e);
                }
            }
        }
    }

    protected void save( InputStream inputStream, File file )
            throws IOException
    {
        FileOutputStream stream = new FileOutputStream(file);
        try {
            FileUtil.copy(inputStream, stream);
        }
        finally {
            try {
                stream.close();
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
    }

    protected void copy( File source, File dest ) throws IOException {
        FileInputStream inputStream = new FileInputStream(source);
        try {
            save(inputStream, dest, true);
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
    }

    protected String readFile( File file ) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        try {
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(inputStream, UTF_8));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                LOG.log(Level.INFO, null, e);
            }
        }
    }

}
