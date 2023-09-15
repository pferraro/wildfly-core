/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.remoting;

/**
 * Class to hold capabilities provided by and required by resources within this package.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
final class Capabilities {
    static final String HTTP_LISTENER_REGISTRY_CAPABILITY_NAME = "org.wildfly.remoting.http-listener-registry";

    static final String REMOTING_ENDPOINT_CAPABILITY_NAME = "org.wildfly.remoting.endpoint";

    static final String IO_WORKER_CAPABILITY_NAME = "org.wildfly.io.worker";
}
