/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.fighterfish.test.util;

import static java.lang.management.ManagementFactory.getThreadMXBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * This object represents the current stack traces of all threads in the system
 * similar to the output of jstack command line tool.
 * Its {@link #toString()} returns the stack traces of all the threads by
 * calling underlying {@link ThreadMXBean}.
 */
public final class JStack {

    @Override
    public String toString() {
        return getAllStack(getThreadMXBean().dumpAllThreads(true, true));
    }

    /**
     * Get all stack.
     * @param threadInfos thread infos
     * @return stack descriptions
     */
    private static String getAllStack(ThreadInfo[] threadInfos) {
        if (threadInfos == null) {
            return "null";
        }
        
        StringBuilder stackBuilder = new StringBuilder("[");
        for (ThreadInfo threadInfo : threadInfos) {
            stackBuilder.append("\n [").append(getStack(threadInfo)).append(" ]");
            if (threadInfo != threadInfos[threadInfos.length - 1]) {
                stackBuilder.append(",");
            }
        }
        
        stackBuilder.append("\n]");
        
        return stackBuilder.toString();
    }

    /**
     * Get a single stack.
     * @param ti thread info
     * @return stack description
     */
    private static String getStack(final ThreadInfo ti) {
        // This method has been largely copied from ThreadInfo.java as
        // toString() of ThreadInfo
        StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\""
                + " Id=" + ti.getThreadId() + " "
                + ti.getThreadState());
        if (ti.getLockName() != null) {
            sb.append(" on ").append(ti.getLockName());
        }
        if (ti.getLockOwnerName() != null) {
            sb.append(" owned by \"")
                    .append(ti.getLockOwnerName())
                    .append("\" Id=")
                    .append(ti.getLockOwnerId());
        }
        if (ti.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (ti.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');
        int i = 0;
        StackTraceElement[] stackTrace = ti.getStackTrace();
        for (; i < stackTrace.length; i++) {
            StackTraceElement ste = stackTrace[i];
            sb.append("\t\tat ")
                    .append(ste.toString())
                    .append('\n');
            if (i == 0 && ti.getLockInfo() != null) {
                Thread.State ts = ti.getThreadState();
                switch (ts) {
                    case BLOCKED:
                        sb.append("\t-  blocked on ")
                                .append(ti.getLockInfo())
                                .append('\n');
                        break;
                    case WAITING:
                        sb.append("\t-  waiting on ")
                                .append(ti.getLockInfo())
                                .append('\n');
                        break;
                    case TIMED_WAITING:
                        sb.append("\t-  waiting on ")
                                .append(ti.getLockInfo())
                                .append('\n');
                        break;
                    default:
                }
            }

            for (MonitorInfo mi : ti.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked ")
                            .append(mi)
                            .append('\n');
                }
            }
        }

        LockInfo[] locks = ti.getLockedSynchronizers();
        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ")
                    .append(locks.length)
                    .append('\n');
            for (LockInfo li : locks) {
                sb.append("\t- ")
                        .append(li)
                        .append('\n');
            }
        }
        sb.append('\n');
        return sb.toString();

    }

    /**
     * Print a stack trace to a file.
     */
    public void printStackTrace() {
        File f = new File(System.getProperty("user.home"), "jstack.txt");
        System.out.println("JStack written out to " + f.getAbsolutePath());
        try {
            final FileOutputStream out = new FileOutputStream(f, true);
            printStackTrace(out);
            out.close();
        } catch (IOException e) {
            // TODO(Sahoo): Proper Exception Handling
            throw new RuntimeException(e);
        }
    }

    /**
     * Print a stack trace to the given output stream.
     * @param out output stream
     */
    public void printStackTrace(final OutputStream out) {
        final String s = toString();
        final PrintWriter printWriter = new PrintWriter(
                new OutputStreamWriter(out, Charset.defaultCharset()));
        printWriter.println("Stack trace generated at " + new Date()
                + "\n" + s);
        printWriter.flush();
    }

    /**
     * Main method.
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        new Thread() {
            {
                setDaemon(false);
            }

            @Override
            public void run() {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // TODO(Sahoo): Proper Exception Handling
                        throw new RuntimeException(e);
                    }
                }
            }
        }.start();
        final JStack x = new JStack();
        System.out.println(x);
        x.printStackTrace();
    }
}
