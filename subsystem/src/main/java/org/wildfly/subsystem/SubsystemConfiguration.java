/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.subsystem;

import org.jboss.as.controller.SubsystemModel;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;

/**
 * Encapsulates the configuration of a subsystem.
 * @author Paul Ferraro
 */
public interface SubsystemConfiguration {

    String getName();

    SubsystemModel getModel();

    SubsystemResourceDefinitionRegistrar getRegistrar();
}
