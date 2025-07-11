/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.server.moduleservice;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.server.deployment.module.ExtensionInfo;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFSUtils;

/**
 * An MSC Service that provides an ExtensionIndex.
 * See <a href="https://jakarta.ee/specifications/platform/10/jakarta-platform-spec-10.0#a2945">Jakarta EE Platform Specification, Library Support</a>
 * <p>
 * This service is responsible for scanning a set of directories for JAR files that contain extensions.
 * By default, lib/ext under the server base directory is scanned. Users can specify additional directories
 * using java.ext.dirs system property, {@link org.jboss.as.server.ServerEnvironment#JAVA_EXT_DIRS}. For each
 * discovered extension, a module spec service is registered with the MSC container.
 * <p>
 * In addition to scanning directories, this service also allows the addition and removal of extensions
 * provided by deployed applications. Those extensions are discovered by {@link org.jboss.as.server.deployment.module.ManifestExtensionNameProcessor}, which
 * inspects the manifest file of the deployment. Those discovered extensions are added to
 * this extension index by ModuleExtensionNameProcessor deployment processor.
 * <p>
 * Using this mechanism a Jakarta EE application can reference utility classes or other shared classes or
 * resources packaged in a separate .jar file or directory that is included in the same Jakarta EE application
 * package, or that has been previously installed in the Jakarta EE containers.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Stuart Douglas
 */
public final class ExtensionIndexService implements Service<ExtensionIndex>, ExtensionIndex {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment.module.extension-index");

    public static final String MODULE_PREFIX = ServiceModuleLoader.MODULE_PREFIX + "extension.";

    private final File[] extensionRoots;
    private final Map<String, Set<ExtensionJar>> extensions = new HashMap<String, Set<ExtensionJar>>();

    /**
     * Construct a new service instance.
     * @param roots The directories that will be scanned for JAR files that will be treated as Jakarta EE extensions.
     */
    public ExtensionIndexService(final File... roots) {
        extensionRoots = roots;
    }

    public synchronized void start(final StartContext context) throws StartException {
        // No point in throwing away the index once it is created.
        context.getController().compareAndSetMode(ServiceController.Mode.ON_DEMAND, ServiceController.Mode.ACTIVE);
        extensions.clear();
        for (File root : extensionRoots) {
            final File[] jars = root.listFiles(new FileFilter() {
                public boolean accept(final File file) {
                    return file.getName().endsWith(".jar") && !file.isDirectory();
                }
            });
            if (jars != null)
                for (File jar : jars)
                    try {
                        final JarFile jarFile = new JarFile(jar);
                        try {
                            final Manifest manifest = jarFile.getManifest();
                            if (manifest == null) {
                                // jar lacks manifest file
                                continue;
                            }
                            final Attributes mainAttributes = manifest.getMainAttributes();
                            final String extensionName = mainAttributes.getValue(Attributes.Name.EXTENSION_NAME);
                            if (extensionName == null) {
                                // not an extension
                                continue;
                            }
                            final String implVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                            final String specVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
                            final String implVendorId = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR_ID);
                            jarFile.close();
                            Set<ExtensionJar> extensionJarSet = extensions.get(extensionName);
                            if (extensionJarSet == null)
                                extensions.put(extensionName, extensionJarSet = new LinkedHashSet<>());

                            String moduleIdentifier = moduleIdentifier(extensionName, specVersion, implVersion,
                                    implVendorId);

                            ExtensionJar extensionJar = new ExtensionJar(moduleIdentifier, implVersion, implVendorId,
                                    specVersion, jar.getAbsolutePath());
                            if (extensionJarSet.contains(extensionJar)) // if the same extension is installed in two
                                // different places
                                continue;
                            // now register a module spec service for this extension
                            // this makes it available for loading
                            ExternalModuleSpecService service = new ExternalModuleSpecService(moduleIdentifier, jar);
                            ServiceName serviceName = ServiceModuleLoader.moduleSpecServiceName(moduleIdentifier);
                            ServiceBuilder sb = context.getChildTarget().addService(serviceName, service);
                            sb.requires(org.jboss.as.server.deployment.Services.JBOSS_DEPLOYMENT_EXTENSION_INDEX);
                            sb.setInitialMode(Mode.ON_DEMAND);
                            sb.install();

                            ModuleLoadService.install(context.getChildTarget(), moduleIdentifier);

                            extensionJarSet.add(extensionJar);

                        } finally {
                            VFSUtils.safeClose(jarFile);
                        }
                    } catch (IOException e) {
                        log.debugf("Failed to process JAR manifest for %s: %s", jar, e);
                    }
        }
    }

    public synchronized void stop(final StopContext context) {
        extensions.clear();
    }

    @Override
    public synchronized void addDeployedExtension(String identifier, ExtensionInfo extensionInfo) {
        final ExtensionJar extensionJar = new ExtensionJar(identifier, extensionInfo);
        Set<ExtensionJar> jars = this.extensions.get(extensionInfo.getName());
        if (jars == null) {
            this.extensions.put(extensionInfo.getName(), jars = new HashSet<>());
        }
        jars.add(extensionJar);
    }

    @Override
    public synchronized boolean removeDeployedExtension(String name, String identifier) {
        final Set<ExtensionJar> jars = this.extensions.get(name);
        if (jars != null) {
            final Iterator<ExtensionJar> it = jars.iterator();
            while (it.hasNext()) {
                final ExtensionJar jar = it.next();
                if (jar.moduleIdentifier.equals(identifier)) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String findExtensionAsString(String name, String minSpecVersion, String minImplVersion, String requiredVendorId) {
        final Set<ExtensionJar> jars = extensions.get(name.trim());
        if (jars != null)
            for (ExtensionJar extensionJar : jars) {

                // Check the parameters
                final String implVendorId = extensionJar.implVendorId;
                if (requiredVendorId != null && !requiredVendorId.equals(implVendorId.trim())) {
                    log.debugf("Skipping extension JAR %s because vendor ID %s does not match required vendor ID %s",
                            extensionJar.path, requiredVendorId, implVendorId);
                    continue;
                }
                if (minSpecVersion != null) {
                    final String specVersion = extensionJar.specVersion;
                    if (specVersion == null) {
                        log.debugf("Skipping extension JAR %s because spec version is missing but %s is required",
                                extensionJar.path, minSpecVersion);
                        continue;
                    }
                    try {
                        if (compareVersion(minSpecVersion.trim(), specVersion) > 0) {
                            log.debugf("Skipping extension JAR %s because spec version %s is less than required version %s",
                                    extensionJar.path, specVersion, minSpecVersion);
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        log.debugf("Skipping extension JAR %s because version compare of spec version failed");
                        continue;
                    }
                }
                if (minImplVersion != null) {
                    final String implVersion = extensionJar.implVersion;
                    if (implVersion == null) {
                        log.debugf("Skipping extension JAR %s because impl version is missing but %s is required",
                                extensionJar.path, minImplVersion);
                        continue;
                    }
                    try {
                        if (compareVersion(minImplVersion.trim(), implVersion) > 0) {
                            log.debugf("Skipping extension JAR %s because impl version %s is less than required version %s",
                                    extensionJar.path, implVersion, minImplVersion);
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        log.debugf("Skipping extension JAR %s because version compare of impl version failed");
                        continue;
                    }
                }

                // Extension matches!
                log.debugf("Matched extension JAR %s", extensionJar.path);
                return extensionJar.moduleIdentifier;
            }
        return null;
    }

    private static String moduleIdentifier(final String name, final String minSpecVersion,
                                                    final String minImplVersion, final String requiredVendorId) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(MODULE_PREFIX);
        nameBuilder.append(name);
        if (minSpecVersion != null) {
            nameBuilder.append(".spec-");
            nameBuilder.append(minSpecVersion);
        }
        if (minImplVersion != null) {
            nameBuilder.append(".impl-");
            nameBuilder.append(minImplVersion);
        }
        if (requiredVendorId != null) {
            nameBuilder.append(".vendor-");
            nameBuilder.append(requiredVendorId);
        }
        return nameBuilder.toString();
    }

    public ExtensionIndex getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private static int compareVersion(String v1, String v2) {
        if (v1.isEmpty() && v2.isEmpty()) {
            return 0;
        } else if (v1.isEmpty()) {
            return -1;
        } else if (v2.isEmpty()) {
            return 1;
        }
        int s1 = 0, e1;
        int s2 = 0, e2;
        for (;;) {
            e1 = v1.indexOf('.', s1);
            e2 = v2.indexOf('.', s2);
            String seg1 = e1 == -1 ? v1.substring(s1) : v1.substring(s1, e1);
            String seg2 = e2 == -1 ? v2.substring(s2) : v2.substring(s2, e2);
            int i1 = Integer.parseInt(seg1);
            int i2 = Integer.parseInt(seg2);
            if (i1 > i2)
                return 1;
            if (i1 < i2)
                return -1;
            if (e1 == -1 && e2 == -1)
                return 0;
            if (e1 == -1)
                return 1;
            if (e2 == -1)
                return -1;
            s1 = e1 + 1;
            s2 = e2 + 1;
        }
    }

    static class ExtensionJar {

        private final String implVersion;
        private final String implVendorId;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((implVendorId == null) ? 0 : implVendorId.hashCode());
            result = prime * result + ((implVersion == null) ? 0 : implVersion.hashCode());
            result = prime * result + ((specVersion == null) ? 0 : specVersion.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExtensionJar other = (ExtensionJar) obj;
            if (implVendorId == null) {
                if (other.implVendorId != null)
                    return false;
            } else if (!implVendorId.equals(other.implVendorId))
                return false;
            if (implVersion == null) {
                if (other.implVersion != null)
                    return false;
            } else if (!implVersion.equals(other.implVersion))
                return false;
            if (specVersion == null) {
                if (other.specVersion != null)
                    return false;
            } else if (!specVersion.equals(other.specVersion))
                return false;
            return true;
        }

        private final String specVersion;
        private final String path;
        private final String moduleIdentifier;

        ExtensionJar(final String moduleIdentifier, final String implVersion, final String implVendorId,
                final String specVersion, final String path) {
            this.implVersion = implVersion;
            this.implVendorId = implVendorId;
            this.specVersion = specVersion;
            this.path = path;
            this.moduleIdentifier = moduleIdentifier;
        }

        ExtensionJar(final String moduleIdentifier, final ExtensionInfo info) {
            this.implVersion = info.getImplVersion();
            this.implVendorId = info.getImplVendorId();
            this.specVersion = info.getSpecVersion();
            this.path = null;
            this.moduleIdentifier = moduleIdentifier;
        }
    }
}
