/*
 * Copyright (c) 2009, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.osgijavaeebase;

import java.io.*;
import java.net.URLConnection;
import java.net.URI;
import java.util.jar.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

/**
 * A utility class to help reading/writing content of JarFile from/to stream.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class JarHelper {

    private static final Logger LOGGER = Logger.getLogger(
            JarHelper.class.getPackage().getName());

    public static interface Visitor {

        void visit(JarEntry je);
    }

    public static void accept(JarInputStream jis, Visitor visitor)
            throws IOException {

        JarEntry je;
        while ((je = jis.getNextJarEntry()) != null) {
            LOGGER.logp(Level.FINE, "JarHelper", "accept", "je = ${0}",
                    new Object[]{je});
            visitor.visit(je);
        }
    }

    /**
     * A utility method which reads contents from a URLConnection and writes it
     * out a Jar output stream. It reads everything except manifest from the
     * input. Closing of output stream is caller's responsibility.
     *
     * @param con URLConnection to be used as input
     * @param os Output stream to write to
     * @param m Manifest to be written out - cannot be null
     */
    public static void write(URLConnection con, OutputStream os, Manifest m) {
        try {
            InputStream in = con.getInputStream();
            JarInputStream jis = null;
            JarOutputStream jos = null;
            try {
                // We can assume the underlying stream is a JarInputStream.
                jis = new JarInputStream(in);
                jos = new JarOutputStream(os, m);
                write(jis, jos);
            } finally {
                try {
                    if (jos != null) {
                        jos.close();
                    }
                } catch (IOException ioe) {
                }
                try {
                    if (jis != null) {
                        jis.close();
                    }
                } catch (IOException ioe) {
                }
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * A utility method to help write content of a Jar input stream to a Jar
     * output stream. It reads everything except manifest from the supplied
     * InputStream. Closing of streams is caller's responsibility.
     *
     * @param jis input stream to read from
     * @param jos output stream to write to
     * @throws IOException
     */
    public static void write(JarInputStream jis, JarOutputStream jos)
            throws IOException {

        // Copy each entry from input to output
        // The manifest.mf is automatically excluded,
        // as JarInputStream.getNextEntry never returns that.
        ByteBuffer byteBuffer = ByteBuffer.allocate(10240);
        ZipEntry ze;
        while ((ze = jis.getNextEntry()) != null) {
            LOGGER.logp(Level.FINE, "JarHelper", "write", "ze = {0}",
                    new Object[]{ze});
            jos.putNextEntry(ze);
            copy(jis, jos, byteBuffer);
            jos.closeEntry();
        }
    }

    /**
     * A utility method to make a JarInputStream out of the contents of a
     * directory. It uses a Pipe and a separate thread to write the contents to
     * avoid deadlock. It accepts a Runnable to take action once the spawned
     * thread has finished writing. It can be used to delete the directory.
     *
     * @param dir Directory which contains the exploded bits
     * @param action A runnable to be called after output has been written
     * @return a InputStream
     * @throws IOException
     */
    public static InputStream makeJar(final File dir, final Runnable action)
            throws IOException {

        final PipedOutputStream pos = new PipedOutputStream();
        final PipedInputStream pis = new PipedInputStream(pos);
        new Thread() {
            @Override
            public void run() {
                try {
                    Manifest m;
                    File mf = new File(dir, JarFile.MANIFEST_NAME);
                    if (mf.exists()) {
                        FileInputStream mfis = new FileInputStream(mf);
                        try {
                            m = new Manifest(mfis);
                        } finally {
                            mfis.close();
                        }
                    } else {
                        m = new Manifest();
                        m.getMainAttributes().putValue(
                                Attributes.Name.MANIFEST_VERSION.toString(),
                                "1.0");
                    }
                    final JarOutputStream jos = new JarOutputStream(pos, m);
                    final ByteBuffer buf = ByteBuffer.allocate(10240);
                    final URI baseURI = dir.toURI();
                    dir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            try {
                                URI entryURI = f.toURI();
                                String entryPath = baseURI
                                        .relativize(entryURI).getPath();
                                if (entryPath.equals(JarFile.MANIFEST_NAME)) {
                                    return false;
                                }
                                jos.putNextEntry(new JarEntry(entryPath));
                                if (f.isDirectory()) {
                                    f.listFiles(this); // recursion
                                } else {
                                    FileInputStream in = new FileInputStream(f);
                                    try {
                                        copy(in, jos, buf);
                                    } finally {
                                        try {
                                            in.close();
                                        } catch (IOException e) {
                                            // ignore
                                        }
                                    }
                                }
                                jos.closeEntry();
                            } catch (IOException e) {
                                LOGGER.logp(Level.WARNING, "JarHelper",
                                        "makeJar", "Exception occurred", e);
                                // TODO(Sahoo): Proper Exception Handling
                                throw new RuntimeException(e);
                            }
                            return false;
                        }
                    });
                    jos.close();
                    pos.close();
                    if (action != null) {
                        action.run();
                    }
                } catch (IOException e) {
                    // TODO(Sahoo): Proper Exception Handling
                    throw new RuntimeException(e);
                }
            }
        }.start();
        return pis;
    }

    /**
     * Copies input to output. To avoid unnecessary allocation of byte buffers,
     * this method takes a byte buffer as argument. It clears the byte buffer at
     * the end of the operation.
     *
     * @param in
     * @param out
     * @param byteBuffer
     * @throws IOException
     */
    public static void copy(InputStream in, OutputStream out, ByteBuffer byteBuffer)
            throws IOException {

        try {
            ReadableByteChannel inChannel = Channels.newChannel(in);
            WritableByteChannel outChannel = Channels.newChannel(out);

            int read;
            do {
                read = inChannel.read(byteBuffer);
                if (read > 0) {
                    byteBuffer.limit(byteBuffer.position());
                    byteBuffer.rewind();
                    int written = 0;
                    while ((written += outChannel.write(byteBuffer)) < read) {
                    }
                    LOGGER.logp(Level.FINE, "JarHelper", "write",
                            "Copied {0} bytes", new Object[]{read});
                    byteBuffer.clear();
                }
            } while (read != -1);
        } finally {
            byteBuffer.clear();
        }
    }
}
