/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.management;

import org.jboss.as.controller.security.SecurityServiceDescriptor;

/**
 * Class to hold capabilities provided by and required by resources within this package.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @deprecated Replaced by {@link SecurityServiceDescriptor}.
 */
@Deprecated(forRemoval = true)
public final class Capabilities {

    public static final String HTTP_MANAGEMENT_CAPABILITY = BaseHttpInterfaceResourceDefinition.HTTP_MANAGEMENT_RUNTIME_CAPABILITY.getName();

    public static final String HTTP_AUTHENTICATION_FACTORY_CAPABILITY = SecurityServiceDescriptor.HTTP_AUTHENTICATION_FACTORY.getName();

    public static final String NATIVE_MANAGEMENT_CAPABILITY = BaseNativeInterfaceResourceDefinition.NATIVE_MANAGEMENT_RUNTIME_CAPABILITY.getName();

    public static final String SASL_AUTHENTICATION_FACTORY_CAPABILITY = SecurityServiceDescriptor.SASL_AUTHENTICATION_FACTORY.getName();

    public static final String SSL_CONTEXT_CAPABILITY = SecurityServiceDescriptor.SSL_CONTEXT.getName();

    public static final String MANAGEMENT_SECURITY_REALM_CAPABILITY = "org.wildfly.core.management.security.realm";
}
