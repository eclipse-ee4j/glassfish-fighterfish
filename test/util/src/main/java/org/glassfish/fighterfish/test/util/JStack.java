/*
 * Copyright (c) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.management.*;
import java.util.Date;

/**
 * This object represents the current stack traces of all threads in the system
 * similar to the output of jstack command line tool.
 * Its {@link #toString()} returns the stack traces of all the threads by
 * calling underlying {@link ThreadMXBean}.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class JStack {

    @Override
    public String toString() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return getAllStack(threadMXBean.dumpAllThreads(true, true));
    }

    private static String getAllStack(ThreadInfo[] tis) {
        if (tis == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder("[");
        for (ThreadInfo ti : tis) {
            b.append("\n [").append(getStack(ti)).append(" ]");
            if (ti != tis[tis.length - 1]) {
                b.append(",");
            }
        }
        b.append("\n]");
        return b.toString();
    }

    private static String getStack(ThreadInfo ti) {
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

    public void printStackTrace(OutputStream out) {
        final String s = toString();
        final PrintWriter printWriter = new PrintWriter(out);
        printWriter.println("Stack trace generated at " + new Date()
                + "\n" + s);
        printWriter.flush();
    }

    public static void main(String[] args) {
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
