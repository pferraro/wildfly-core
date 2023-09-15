/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.controller.security;

import java.security.KeyStore;
import java.security.Policy;

import javax.net.ssl.SSLContext;

import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.server.HttpAuthenticationFactory;
import org.wildfly.security.auth.server.SaslAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Enumerates security related service descriptors.
 */
public class SecurityServiceDescriptor {

    public static final UnaryServiceDescriptor<KeyStore> KEY_STORE = UnaryServiceDescriptor.of("org.wildfly.security.key-store", KeyStore.class);

    public static final UnaryServiceDescriptor<Policy> JACC_POLICY = UnaryServiceDescriptor.of("org.wildfly.security.jacc-policy", Policy.class);

    public static final UnaryServiceDescriptor<SSLContext> SSL_CONTEXT = UnaryServiceDescriptor.of("org.wildfly.security.ssl-context", SSLContext.class);

    public static final UnaryServiceDescriptor<CredentialStore> CREDENTIAL_STORE = UnaryServiceDescriptor.of("org.wildfly.security.credential-store", CredentialStore.class);

    public static final UnaryServiceDescriptor<AuthenticationContext> AUTHENTICATION_CONTEXT = UnaryServiceDescriptor.of("org.wildfly.security.authentication-context", AuthenticationContext.class);

    public static final UnaryServiceDescriptor<SecurityDomain> SECURITY_DOMAIN = UnaryServiceDescriptor.of("org.wildfly.security.security-domain", SecurityDomain.class);

    public static final UnaryServiceDescriptor<HttpAuthenticationFactory> HTTP_AUTHENTICATION_FACTORY = UnaryServiceDescriptor.of("org.wildfly.security.http-authentication-factory", HttpAuthenticationFactory.class);

    public static final UnaryServiceDescriptor<SaslAuthenticationFactory> SASL_AUTHENTICATION_FACTORY = UnaryServiceDescriptor.of("org.wildfly.security.sasl-authentication-factory", SaslAuthenticationFactory.class);

    private SecurityServiceDescriptor() {
        // Hide
    }
}
