/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.service.descriptor;

import java.util.Map;

import org.wildfly.common.Assert;

/**
 * Describes a service by its name, provided value type, and dynamic name resolution mechanism.
 * @author Paul Ferraro
 * @param <T> the type of the value provided by the described service
 */
public interface BinaryServiceDescriptor<T> extends ServiceDescriptor<T> {

    /**
     * Creates a binary service descriptor with the specified name and type
     * @param <T> the service type
     * @param name the service name
     * @param type the service type
     * @return a service descriptor
     */
    static <T> BinaryServiceDescriptor<T> of(String name, Class<T> type) {
        return new BinaryServiceDescriptor<>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<T> getType() {
                return type;
            }
        };
    }

    /**
     * Resolves the dynamic name of the service using the specified segments.
     * @param parent the first dynamic segment
     * @param child the second dynamic segment
     * @return a tuple containing the resolved name and dynamic segments
     */
    default Map.Entry<String, String[]> resolve(String parent, String child) {
        return Map.entry(this.getName(), new String[] {
                Assert.checkNotNullParamWithNullPointerException("parent", parent),
                Assert.checkNotNullParamWithNullPointerException("child", child)
        });
    }
}
