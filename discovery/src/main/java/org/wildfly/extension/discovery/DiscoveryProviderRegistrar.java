/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.discovery;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * @author Paul Ferraro
 */
public class DiscoveryProviderRegistrar implements ChildResourceDefinitionRegistrar {

    // TODO Move this to an SPI module, when this capability acquires any consumers
    static final UnaryServiceDescriptor<DiscoveryProvider> DISCOVERY_PROVIDER_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.discovery.provider", DiscoveryProvider.class);
    static final RuntimeCapability<Void> DISCOVERY_PROVIDER_CAPABILITY = RuntimeCapability.Builder.of(DISCOVERY_PROVIDER_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    private final PathElement path;
    private final ResourceDescriptor descriptor;

    DiscoveryProviderRegistrar(PathElement path, ResourceDescriptor.Builder builder) {
        this.path = path;
        this.descriptor = builder.addCapability(DISCOVERY_PROVIDER_CAPABILITY).build();
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDefinition definition = ResourceDefinition.builder(this.path, this.descriptor.getResourceDescriptionResolver()).build();
        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(this.descriptor).register(registration);
        return registration;
    }
}
