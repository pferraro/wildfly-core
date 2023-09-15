/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller.management;

import javax.management.MBeanServer;

import org.wildfly.service.descriptor.NullaryServiceDescriptor;

/**
 * Enumerates management-related service descriptors.
 */
public class ManagementServiceDescriptor {

    public static final NullaryServiceDescriptor<MBeanServer> MBEAN_SERVER = NullaryServiceDescriptor.of("org.wildfly.management.jmx", MBeanServer.class);

    private ManagementServiceDescriptor() {
        // Hide
    }
}
