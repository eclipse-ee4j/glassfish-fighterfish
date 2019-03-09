/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.obrbuilder;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

import javax.inject.Inject;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.CapabilityImpl;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.bundlerepository.impl.RepositoryImpl;
import org.apache.felix.bundlerepository.impl.RequirementImpl;
import org.apache.felix.bundlerepository.impl.ResourceImpl;
import org.apache.felix.utils.log.Logger;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import static org.glassfish.obrbuilder.Logger.LOGGER;

/**
 * OBR handler service implementation.
 */
final class ObrHandlerServiceImpl implements ObrHandlerService {

    /**
     * Repository list used during resolution process. That way, we are not
     * affected by any repository added by user to a shared instance of
     * repository admin.
     */
    private final List<Repository> repositories = new ArrayList<Repository>();

    /**
     * Future of the list of HK2 annotation descriptor.
     */
    private Future<List<HK2AnnotationDescriptor>> futureHK2AnnotDesc = null;

    /**
     * Bundle context.
     */
    private final BundleContext context;

    /**
     * Repository admin.
     */
    private RepositoryAdmin repoAdmin;

    /**
     * Create a new instance.
     * @param bndCtx bundle context
     */
    ObrHandlerServiceImpl(final BundleContext bndCtx) {
        this.context = bndCtx;
    }

    @Override
    public RepositoryAdmin getRepositoryAdmin() {
        Logger logger = new Logger(null);
        if (repoAdmin == null) {
            repoAdmin = new RepositoryAdminImpl(context, logger);
            repositories.add(repoAdmin.getSystemRepository());
        }
        return repoAdmin;
    }

    @Override
    public synchronized void addRepository(final URI obrUri)
            throws Exception {

        if (isDirectory(obrUri)) {
            setupRepository(new File(obrUri), isSynchronous());
        } else {
            // TangYong Modified
            // If not Directory, we still need to generate obr xml file and
            // defaultly, generated obr xml file name is obr.xml
            Repository repo = getRepositoryAdmin().getHelper().repository(
                    obrUri.toURL());
            saveRepository(getRepositoryFile(null), repo);
            repositories.add(repo);
        }
    }

    /**
     * Test if the given URI is a directory.
     * @param obrUri URI to test
     * @return {@code true} if the URI is a directory, {@code false} otherwise
     */
    private static boolean isDirectory(final URI obrUri) {
        try {
            return new File(obrUri).isDirectory();
        } catch (Exception e) {
        }

        return false;
    }

    /**
     * Setup the repository directory.
     * @param repoDir repository directory
     * @param synchronous asynchronous flag
     * @throws Exception if an error occurs
     */
    private void setupRepository(final File repoDir, final boolean synchronous)
            throws Exception {

        if (synchronous) {
            doSetupRepository(repoDir);
        } else {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        doSetupRepository(repoDir);
                    } catch (Exception e) {
                        throw new RuntimeException(e); // TODO(Sahoo): Proper
                        // Exception Handling
                    }
                }
            });
        }
    }

    /**
     * Get the asynchronous configuration value.
     * @return boolean
     */
    private boolean isSynchronous() {
        String property = context
                .getProperty(Constants.INITIALIZE_OBR_SYNCHRONOUSLY);
        // default is synchronous as we are not sure if we have covered every
        // race condition in asynchronous path
        return property == null
                || Boolean.TRUE.toString().equalsIgnoreCase(property);
    }

    /**
     * Do the actual work of setting up the OBR repository directory.
     * @param repoDir directory
     * @throws Exception if an error occurs
     */
    private synchronized void doSetupRepository(final File repoDir)
            throws Exception {

        File repoFile = getRepositoryFile(repoDir);
        final long tid = Thread.currentThread().getId();
        if (repoFile != null && repoFile.exists()) {
            long t = System.currentTimeMillis();
            updateRepository(repoFile, repoDir);
            long t2 = System.currentTimeMillis();
            LOGGER.logp(Level.INFO, "ObrHandlerServiceImpl",
                    "_setupRepository",
                    "Thread #{0}: updateRepository took {1} ms", new Object[]{
                        tid, t2 - t});
        } else {
            // Scanning the whole repo dir and finding hk2 related annotations
            futureHK2AnnotDesc = Executors
                    .newSingleThreadExecutor()
                    .submit(new Callable<List<HK2AnnotationDescriptor>>() {

                        @Override
                        public List<HK2AnnotationDescriptor> call() {
                            try {
                                List<File> repoFiles = findAllJars(repoDir);
                                return scanHK2Annotations(repoFiles,
                                        buildRepoClassLoader(repoFiles));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

            long t = System.currentTimeMillis();
            repoFile.createNewFile();
            createRepository(repoFile, repoDir);
            long t2 = System.currentTimeMillis();
            LOGGER.logp(Level.INFO, "ObrHandlerServiceImpl",
                    "_setupRepository",
                    "Thread #{0}: createRepository took {1} ms", new Object[]{
                        tid, t2 - t});
        }
    }

    /**
     * Add HK2 dependencies to resources.
     */
    private void addHK2DepsToResources() {
        if (futureHK2AnnotDesc != null) {
            List<HK2AnnotationDescriptor> hK2AnnotationDescriptor;
            try {
                hK2AnnotationDescriptor = futureHK2AnnotDesc.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

            if (!hK2AnnotationDescriptor.isEmpty()) {
                for (HK2AnnotationDescriptor hk2AnnoDesc
                        : hK2AnnotationDescriptor) {
                    String bundleSymbolicName = hk2AnnoDesc
                            .getTargetBundleSymbolicName();
                    String bundleVersion = hk2AnnoDesc.getTargetBundleVersion();
                    Resource resource = findResource(bundleSymbolicName,
                            bundleVersion);
                    if (resource != null) {
                        // adding export-service
                        List<String> serviceClasses = hk2AnnoDesc
                                .getContractClassNames();
                        for (String serviceClazz : serviceClasses) {
                            CapabilityImpl capability = new CapabilityImpl(
                                    Capability.SERVICE);
                            capability
                                    .addProperty(Capability.SERVICE,
                                            serviceClazz);
                            ((ResourceImpl) resource).addCapability(capability);
                        }

                        // adding import-service
                        List<HK2InjectMetadata> injectionFieldMetaDatas
                                = hk2AnnoDesc.getInjectionFieldMetaDatas();
                        for (HK2InjectMetadata injectionFieldMetaData
                                : injectionFieldMetaDatas) {

                            RequirementImpl ri = new RequirementImpl(
                                    Capability.SERVICE);
                            String injectionFieldClassName
                                    = injectionFieldMetaData
                                            .getInjectionFieldClassName();
                            ri.setFilter(createServiceFilter(
                                    injectionFieldClassName));
                            ri.addText("Import Service "
                                    + injectionFieldClassName);
                            ri.setOptional(injectionFieldMetaData
                                    .getOptional());
                            // in the future, will discuss and improve it
                            String mult = "";
                            ri.setMultiple(!"false".equalsIgnoreCase(mult));
                            ((ResourceImpl) resource).addRequire(ri);
                        }
                    }
                }
            }
        }
    }

    /**
     * Create an OSGi service filter for the given class.
     * @param injectClazz class to filter
     * @return String
     */
    private static String createServiceFilter(final String injectClazz) {
        StringBuilder filter = new StringBuilder();
        filter.append("(&(");
        filter.append(Capability.SERVICE);
        filter.append("=");
        filter.append(injectClazz);
        filter.append("))");
        return filter.toString();
    }

    /**
     * Get the repository descriptor.
     * @param repoDir repository directory
     * @return File
     */
    private File getRepositoryFile(final  File repoDir) {
        String extn = ".xml";
        String cacheDir = context.getProperty(Constants.HK2_CACHE_DIR);
        if (cacheDir == null) {
            return null; // caching is disabled, so don't do it.
        }

        // Defaultly, if not specifying repoDir, we will use obr.xml file
        if (repoDir == null) {
            return new File(cacheDir, "obr" + extn);
        }

        return new File(cacheDir, Constants.OBR_FILE_NAME_PREFIX
                + repoDir.getName() + extn);
    }

    /**
     * Create a new Repository from a directory by recursively traversing all
     * the jar files found there.
     *
     * @param repoFile repository descriptor
     * @param repoDir repository directory
     * @throws IOException if an error occurs
     */
    private void createRepository(final File repoFile, final File repoDir)
            throws IOException {
        createRepository(repoFile, repoDir, true);
    }

    // TangYong Added a overridden method to use for deploying subsystem
    /**
     * Create a new Repository from a directory by recursively traversing all
     * the jar files found there.
     *
     * @param repoFile repository descriptor
     * @param repoDir repository directory
     * @param save flag to indicate if the repository should be saved to disk
     * @throws IOException if an error occurs
     */
    private void createRepository(final File repoFile, final File repoDir,
            final boolean save) throws IOException {

        DataModelHelper dmh = getRepositoryAdmin().getHelper();
        List<Resource> resources = new ArrayList<Resource>();
        for (File jar : findAllJars(repoDir)) {
            Resource r = dmh.createResource(jar.toURI().toURL());

            if (r == null) {
                LOGGER.logp(Level.WARNING, "ObrHandlerServiceImpl",
                        "createRepository", "{0} not an OSGi bundle", jar
                                .toURI().toURL());
            } else {
                resources.add(r);
            }
        }
        Repository repository = dmh.repository(resources
                .toArray(new Resource[resources.size()]));
        LOGGER.logp(Level.INFO, "ObrHandlerServiceImpl", "createRepository",
                "Created {0} containing {1} resources.", new Object[]{
                    repoFile, resources.size()});

        repositories.add(repository);
        addHK2DepsToResources();
        if (repoFile != null && save) {
            saveRepository(repoFile, repository);
        }
    }

    /**
     * Scan HK2 annotations.
     * @param repoFiles repository files
     * @param cls class-loader
     * @return list of annotation descriptor
     */
    private List<HK2AnnotationDescriptor> scanHK2Annotations(
            final List<File> repoFiles, final ClassLoader cls) {

        // building class loader
        List<HK2AnnotationDescriptor> hK2AnnotationDescriptor = null;
        String bundleSymbolicName;
        String bundleVersion;

        for (File file : repoFiles) {
            try {
                JarFile jarFile = new JarFile(file);

                Manifest mf = jarFile.getManifest();
                if (mf == null) {
                    // not a valid jar
                    break;
                }

                bundleSymbolicName = mf.getMainAttributes().getValue(
                        "Bundle-SymbolicName");
                if (bundleSymbolicName == null) {
                    // not a valid OSGi bundle
                    break;
                }

                bundleVersion = mf.getMainAttributes().getValue(
                        "Bundle-Version");
                if (bundleVersion == null) {
                    // not a valid OSGi bundle
                    break;
                }

                Enumeration<?> e = jarFile.entries();
                HK2AnnotationDescriptor hk2AnnoDesc = null;

                while (e.hasMoreElements()) {
                    JarEntry je = (JarEntry) e.nextElement();

                    if (je.isDirectory() || !je.getName().endsWith(".class")) {
                        continue;
                    }

                    // -6 because of .class
                    String className = je.getName().substring(0,
                            je.getName().length() - ".class".length());
                    className = className.replaceAll("/", "\\.");
                    Class<?> clazz = null;
                    try {
                        clazz = cls.loadClass(className);
                    } catch (ClassNotFoundException ex) {
                        LOGGER.logp(Level.WARNING, "ObrHandlerServiceImpl",
                                "scanHK2Annotations",
                                "Loading Class: {0} from Bundle: {1} failed.",
                                new Object[]{className, bundleSymbolicName});
                        continue;
                    } catch (NoClassDefFoundError ex1) {
                        LOGGER.logp(Level.WARNING, "ObrHandlerServiceImpl",
                                "scanHK2Annotations",
                                "Loading Class: {0} from Bundle: {1} failed.",
                                new Object[]{className, bundleSymbolicName});
                        continue;
                    }

                    // first, scanning class
                    Annotation hk2ServiceAnnotation = clazz
                            .getAnnotation(Service.class);

                    if (hk2ServiceAnnotation == null) {
                        // scanning must live in hk2 world, otherwise,
                        // we ignore it.
                        continue;
                    }

                    // the class has a @Service annotation
                    // and we build a HK2AnnotationDescriptor instance
                    if (hk2AnnoDesc == null) {
                        hk2AnnoDesc = new HK2AnnotationDescriptor(
                                bundleSymbolicName, bundleVersion);
                    }

                    hk2AnnoDesc.getContractClassNames().add(
                            clazz.getCanonicalName());

                    // then, scanning fields for @Inject and @Optional
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        Annotation[] injectAnnotations = field.getAnnotations();
                        boolean isInject = false;
                        boolean isOptional = false;
                        for (Annotation annotation : injectAnnotations) {
                            if (annotation.annotationType()
                                    .equals(Inject.class)) {
                                isInject = true;
                            } else {
                                if (annotation.annotationType()
                                        .equals(Optional.class)) {
                                    isOptional = true;
                                }
                            }
                        }

                        if (isInject) {
                            HK2InjectMetadata injectMetadata
                                    = new HK2InjectMetadata(
                                            field.getType().getCanonicalName(),
                                            isOptional);
                            hk2AnnoDesc.getInjectionFieldMetaDatas()
                                    .add(injectMetadata);
                        }
                    }
                }

                if (hk2AnnoDesc != null) {
                    if (hK2AnnotationDescriptor == null) {
                        hK2AnnotationDescriptor
                                = new ArrayList<HK2AnnotationDescriptor>();
                    }
                    hK2AnnotationDescriptor.add(hk2AnnoDesc);
                }

                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return hK2AnnotationDescriptor;
    }

    /**
     * Build the class-loader for the repository.
     * @param repoFiles repository files
     * @return URLClassLoader
     */
    private URLClassLoader buildRepoClassLoader(final List<File> repoFiles) {
        URL[] urls = new URL[repoFiles.size()];
        URLClassLoader cls = null;
        for (int i = 0; i < repoFiles.size(); i++) {
            try {
                urls[i] = new URL("jar:file:"
                        + repoFiles.get(i).getAbsolutePath() + "!/");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            cls = URLClassLoader.newInstance(urls, this.getClass()
                    .getClassLoader());
        }
        return cls;
    }

    /**
     * Save the repository.
     * @param repoFile repository descriptor
     * @param repository repository instance
     * @throws IOException if an error occurs
     */
    private void saveRepository(final File repoFile,
            final Repository repository) throws IOException {

        assert (repoFile != null);
        final FileWriter writer = new FileWriter(repoFile);
        getRepositoryAdmin().getHelper().writeRepository(repository, writer);
        writer.flush();
    }

    /**
     * Load a repository from a descriptor.
     * @param repoFile repository descriptor
     * @return Repository
     * @throws Exception if an error occurs
     */
    private Repository loadRepository(final File repoFile) throws Exception {
        assert (repoFile != null);
        return getRepositoryAdmin().getHelper().repository(
                repoFile.toURI().toURL());
    }

    /**
     * Update the repository.
     * @param repoFile repository descriptor
     * @param repoDir repository directory
     * @throws Exception if an error occurs
     */
    private void updateRepository(final File repoFile, final File repoDir)
            throws Exception {

        Repository repository = loadRepository(repoFile);
        repositories.add(repository);
        if (isObsoleteRepo(repository, repoFile, repoDir)) {
            //scanning obsoleted jars and new jars in this repo
            final List<File> updatedJarList = obtainUpdatedJars(repository,
                    repoFile, repoDir);

            //Scanning the updatedJars and finding hk2 related annotations
            futureHK2AnnotDesc = Executors
                    .newSingleThreadExecutor()
                    .submit(new Callable<List<HK2AnnotationDescriptor>>() {

                        @Override
                        public List<HK2AnnotationDescriptor> call() {
                            try {
                                List<File> repoFiles = findAllJars(repoDir);
                                return scanHK2Annotations(updatedJarList,
                                        buildRepoClassLoader(repoFiles));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

            if (!repoFile.delete()) {
                throw new IOException("Failed to delete "
                        + repoFile.getAbsolutePath());
            }
            LOGGER.logp(Level.INFO, "ObrHandlerServiceImpl",
                    "updateRepository", "Recreating {0}",
                    new Object[]{repoFile});

            DataModelHelper dmh = getRepositoryAdmin().getHelper();

            if (!updatedJarList.isEmpty()) {
                for (File updatedJar : updatedJarList) {
                    JarFile jarFile;
                    Manifest mf;
                    try {
                        jarFile = new JarFile(updatedJar);
                        mf = jarFile.getManifest();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                    if (mf == null) {
                        // not a valid jar
                        break;
                    }
                    String bundleSymbolicName = mf.getMainAttributes()
                            .getValue("Bundle-SymbolicName");
                    if (bundleSymbolicName == null) {
                        // not a valid OSGi bundle
                        break;
                    }
                    String bundleVersion = mf.getMainAttributes().getValue(
                            "Bundle-Version");
                    if (bundleVersion == null) {
                        // not a valid OSGi bundle
                        break;
                    }

                    //First, we try to find whether having existing resource
                    // with the same Bundle-SymbolicName and Bundle-Version
                    Resource resource = findResource(bundleSymbolicName,
                            bundleVersion);
                    if (resource != null) {
                        // we must remove the resource and create a new resource
                        // based on bundle url
                        // because RepositoryImpl and API have not offered such
                        // an api
                        // only using java reflect api to get private
                        // m_resourceSet field
                        // lately, I will file an request into felix community
                        Field field = repository.getClass()
                                .getDeclaredField("m_resourceSet");
                        field.setAccessible(true);
                        HashSet<?> resourceSet = (HashSet<?>) field
                                .get(repository);
                        resourceSet.remove(resource);
                    }

                    //we create a new resource
                    Resource newResource = dmh.createResource(updatedJar
                            .toURI().toURL());
                    ((RepositoryImpl) repository).addResource(newResource);
                }

                //Then, adding hk2 deps into resources
                addHK2DepsToResources();
            }

            // finally, we must synchronize with repo dir in case that
            // 1: some resources have left repo dir
            // 2: some resources have been stale resources although bundle
            // jar name is not changed
            synchronizeWithRepoDir(repoDir, repository, updatedJarList);

            repoFile.createNewFile();

            //finally, save Repository
            saveRepository(repoFile, repository);
        }
    }

    /**
     * Synchronize with the repository directory.
     * @param repoDir repository directory
     * @param repository repository instance
     * @param updatedJarList the updated JAR file list
     */
    private void synchronizeWithRepoDir(final File repoDir,
            final Repository repository, final List<File> updatedJarList) {

        Resource[] resources = repository.getResources();
        for (int resIdx = 0; (resources != null)
                && (resIdx < resources.length); resIdx++) {
            Resource resource = resources[resIdx];
            String path = resource.getURI();
            // here, we must build a new URI because path obtained from obr xml
            // file is not a valid path uri.
            File file = null;
            boolean isDeleted = false;
            try {
                file = new File(new URI(path));
                if (!file.exists()) {
                    //remove the resource
                    isDeleted = true;
                } else {
                    String bundleSymbolicName = resource.getSymbolicName();
                    String bundleVersion = resource.getVersion().toString();

                    //we find whether match in updated jar list
                    for (File updatedJar : updatedJarList) {
                        JarFile jarFile = new JarFile(updatedJar);
                        Manifest mf = jarFile.getManifest();
                        String bsn = mf.getMainAttributes().getValue(
                                "Bundle-SymbolicName");
                        String bv = mf.getMainAttributes().getValue(
                                "Bundle-Version");

                        if (updatedJar.getAbsolutePath().equals(file
                                .getAbsolutePath())) {
                            if (bsn.equals(bundleSymbolicName)
                                    && bv.equals(bundleVersion)) {
                                isDeleted = false;
                                break;
                            }

                            isDeleted = true;
                        }
                    }
                }
            } catch (URISyntaxException e) {
                isDeleted = true;
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }

            if (isDeleted) {
                try {
                    Field field = repository.getClass()
                            .getDeclaredField("m_resourceSet");
                    field.setAccessible(true);
                    HashSet<?> resourceSet = (HashSet<?>) field
                            .get(repository);
                    resourceSet.remove(resource);
                    field = repository.getClass()
                            .getDeclaredField("m_resources");
                    field.setAccessible(true);
                    field.set(repository, null);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

    }

    /**
     * Get the list of updated JAR files.
     * @param repository repository instance
     * @param repoFile the repository descriptor
     * @param repoDir the repository directory
     * @return list of JAR files
     */
    private List<File> obtainUpdatedJars(final Repository repository,
            final File repoFile, final File repoDir) {

        List<File> updatedJarList = new ArrayList<File>();
        long lastModifiedTime = repoFile.lastModified();
        for (File jar : findAllJars(repoDir)) {
            if (jar.lastModified() > lastModifiedTime) {
                updatedJarList.add(jar);
            } else {
                //comparing size
                JarFile jarFile;
                Manifest mf;
                try {
                    jarFile = new JarFile(jar);
                    mf = jarFile.getManifest();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                if (mf == null) {
                    // not a valid jar
                    break;
                }
                String bundleSymbolicName = mf.getMainAttributes().getValue(
                        "Bundle-SymbolicName");
                if (bundleSymbolicName == null) {
                    // not a valid OSGi bundle
                    break;
                }
                String bundleVersion = mf.getMainAttributes().getValue(
                        "Bundle-Version");
                if (bundleVersion == null) {
                    // not a valid OSGi bundle
                    break;
                }

                Resource resource = findResource(bundleSymbolicName,
                        bundleVersion);
                if (resource == null) {
                    //1: having more higher version bundle
                    //2: new bundle is added into repo
                    updatedJarList.add(jar);
                } else {
                    //comparing size
                    if (jar.length() != resource.getSize()) {
                        updatedJarList.add(jar);
                    }
                }
            }
        }

        return updatedJarList;
    }

    /**
     * Test if the repository is obsolete.
     * @param repository repository instance
     * @param repoFile repository descriptor
     * @param repoDir repository directory
     * @return {@code true} if obsolete, {@code false} otherwise
     */
    private static boolean isObsoleteRepo(final Repository repository,
            final File repoFile, final File repoDir) {

        // TODO(Sahoo): Revisit this...
        // This method assumes that the cached repoFile has been created before
        // a newer jar is created.
        // So, this method does not always detect stale repoFile. Imagine the
        // following situation:
        // time t1: v1 version of jar is released.
        // time t2: v2 version of jar is released.
        // time t3: repo.xml is populated using v1 version of jar, so repo.xml
        // records a timestamp of t3 > t2.
        // time t4: v2 version of jar is unzipped on modules/ and unzip
        // maintains the timestamp of jar as t2.
        // Next time when we compare timestamp, we will see that repo.xml is
        // newer than this jar, when it is not.
        // So, we include a size check. We go for the total size check...

        long lastModifiedTime = repoFile.lastModified();
        // optimistic: see if the repoDir has been touched. dir timestamp
        // changes when files are added or removed.
        if (repoDir.lastModified() > lastModifiedTime) {
            return true;
        }

        long totalSize = 0;
        // now compare timestamp of each jar and take a sum of size of all jars.
        for (File jar : findAllJars(repoDir)) {
            if (jar.lastModified() > lastModifiedTime) {
                LOGGER.logp(Level.INFO, "ObrHandlerServiceImpl",
                        "isObsoleteRepo", "{0} is newer than {1}",
                        new Object[]{jar, repoFile});
                return true;
            }
            totalSize += jar.length();
        }
        // time stamps didn't identify any difference, so check sizes. The
        // probabibility of sizes of all jars being same
        // when some jars have changed is very very low.
        for (Resource r : repository.getResources()) {
            totalSize -= r.getSize();
        }
        if (totalSize != 0) {
            LOGGER.logp(Level.INFO, "ObrHandlerServiceImpl", "isObsoleteRepo",
                    "Change in size detected by {0} bytes",
                    new Object[]{totalSize});
            return true;
        }
        return false;
    }

    /**
     * Find all JAR files in the given directory.
     * @param repo directory
     * @return list of JAR files
     */
    private static List<File> findAllJars(final File repo) {
        final List<File> files = new ArrayList<File>();
        repo.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                if (pathname.isDirectory()) {
                    pathname.listFiles(this);
                } else if (pathname.getName().endsWith("jar")) {
                    files.add(pathname);
                }
                return true;
            }
        });
        return files;
    }

    /**
     * Resolve the given resource with the given resolver.
     * @param resolver resolver to use
     * @param resource resource to resolve
     * @return {@code true} if resolved, {@code false} otherwise
     */
    private static boolean resolve(final Resolver resolver,
            final Resource resource) {

        resolver.add(resource);
        boolean resolved = resolver.resolve();
        LOGGER.logp(Level.INFO, "ObrHandlerServiceImpl", "resolve",
                "At the end of first pass, resolver outcome is \n: {0}",
                new Object[]{getResolverOutput(resolver)});

        return resolved;
    }

    /**
     * Get the bundle for a given resource.
     * @param resource resource to match
     * @return Bundle
     */
    private Bundle getBundle(final Resource resource) {
        for (Bundle b : context.getBundles()) {
            final String bsn = b.getSymbolicName();
            final Version bv = b.getVersion();
            final String rsn = resource.getSymbolicName();
            final Version rv = resource.getVersion();
            boolean versionMatching = (rv == bv)
                    || (rv != null && rv.equals(bv));
            boolean nameMatching = (bsn == rsn)
                    || (bsn != null && bsn.equals(rsn));
            if (nameMatching && versionMatching) {
                return b;
            }
        }
        return null;
    }

    /**
     * Find the bundle resource for a given bundle and version.
     * @param name bundle symbolic name
     * @param version bundle version
     * @return Resource
     */
    private Resource findResource(final String name, final String version) {
        final RepositoryAdmin repositoryAdmin = getRepositoryAdmin();
        if (repositoryAdmin == null) {
            LOGGER.logp(
                    Level.WARNING,
                    "ObrHandlerServiceImpl",
                    "findResource",
                    "OBR is not yet available, so can't find resource with name"
                            + " = {0} and version = {1} from repository",
                    new Object[]{name, version});
            return null;
        }
        String s1 = "(symbolicname=" + name + ")";
        String s2 = "(version=" + version + ")";
        String query;
        if (version != null) {
            query = "(&" + s1 + s2 + ")";
        } else {
            query = s1;
        }
        try {
            Resource[] resources = discoverResources(query);
            LOGGER.logp(
                    Level.INFO,
                    "ObrHandlerServiceImpl",
                    "findResource",
                    "Using the first one from the list of {0} discovered"
                            + " bundles shown below: {1}",
                    new Object[]{resources.length, Arrays.toString(resources)});
            if (resources.length > 0) {
                return resources[0];
            }
            return null;
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e); // TODO(Sahoo): Proper Exception
            // Handling
        }
    }

    /**
     * Discover the resources in a repository.
     * @param filterExpr filter expression
     * @return Resource[]
     * @throws InvalidSyntaxException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private Resource[] discoverResources(final String filterExpr)
            throws InvalidSyntaxException {

        // TODO(Sahoo): File a bug against Obr to add a suitable method to
        // Repository interface.
        // We can't use the following method, because we can't rely on the
        // RepositoryAdmin to have the correct
        // list of repositories. So, we do the discovery ourselves.
        // return getRepositoryAdmin().discoverResources(query);
        Filter filter;
        if (filterExpr != null) {
            filter = getRepositoryAdmin().getHelper().filter(filterExpr);
        } else {
            filter = null;
        }
        Resource[] resources;
        Repository[] repos = getRepositories();
        List<Resource> matchList = new ArrayList<Resource>();
        for (int repoIdx = 0; (repos != null)
                && (repoIdx < repos.length); repoIdx++) {

            resources = repos[repoIdx].getResources();
            for (int resIdx = 0; (resources != null)
                    && (resIdx < resources.length); resIdx++) {
                Properties dict = new Properties();
                dict.putAll(resources[resIdx].getProperties());
                if (filter == null || filter.match((Dictionary) dict)) {
                    matchList.add(resources[resIdx]);
                }
            }
        }
        return matchList.toArray(new Resource[matchList.size()]);
    }

    /**
     * Pretty print the resolver output.
     * @param resolver resolver to print
     * @return StringBuilder
     */
    private static StringBuilder getResolverOutput(final Resolver resolver) {

        Resource[] addedResources = resolver.getAddedResources();
        Resource[] requiredResources = resolver.getRequiredResources();
        Resource[] optionalResources = resolver.getOptionalResources();
        Reason[] unsatisfiedRequirements = resolver
                .getUnsatisfiedRequirements();
        StringBuilder sb = new StringBuilder("Added resources: [");
        for (Resource r : addedResources) {
            sb.append("\n").append(r.getSymbolicName()).append(", ")
                    .append(r.getVersion()).append(", ").append(r.getURI());
        }
        sb.append("]\nRequired Resources: [");
        for (Resource r : requiredResources) {
            sb.append("\n").append(r.getURI());
        }

        for (Resource r : optionalResources) {
            sb.append("\n").append(r.getURI());
        }
        sb.append("]\nUnsatisfied requirements: [");
        for (Reason r : unsatisfiedRequirements) {
            sb.append("\n").append(r.getRequirement());
        }
        sb.append("]");
        return sb;
    }

    /**
     * Convert the repositories list to an array.
     * @return Repository[]
     */
    private Repository[] getRepositories() {
        return repositories.toArray(new Repository[repositories.size()]);
    }
}
