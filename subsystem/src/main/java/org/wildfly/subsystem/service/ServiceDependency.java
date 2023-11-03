/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.subsystem.service;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.msc.service.ServiceName;
import org.wildfly.service.Dependency;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.TernaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Encapsulates a dependency on a {@link org.jboss.msc.Service} that supplies a value.
 * @author Paul Ferraro
 */
public interface ServiceDependency<V> extends Dependency<RequirementServiceBuilder<?>, V> {

    /**
     * Returns a dependency on the service with the specified name.
     * @param <T> the dependency type
     * @return a dependency supplier
     */
    @SuppressWarnings("unchecked")
    static <T> ServiceDependency<T> of(T value) {
        return (value != null) ? new SimpleServiceDependency<>(value) : (ServiceDependency<T>) SimpleServiceDependency.NULL;
    }

    /**
     * Returns a dependency on the service with the specified name.
     * @param <T> the dependency type
     * @return a dependency supplier
     */
    static <T> ServiceDependency<T> on(ServiceName name) {
        return (name != null) ? new DefaultServiceDependency<>(name) : of(null);
    }

    /**
     * Returns a dependency on the capability with the specified name and type, resolved against the specified references names.
     * @param <T> the dependency type
     * @param capabilityName the name of the referenced capability
     * @param type the service type of the referenced capability
     * @param referenceNames the reference names
     * @return a dependency supplier
     */
    static <T> ServiceDependency<T> on(String capabilityName, Class<T> type, String... referenceNames) {
        return new AbstractServiceDependency<>(Map.entry(capabilityName, referenceNames)) {
            @Override
            public Supplier<T> apply(RequirementServiceBuilder<?> builder) {
                return builder.requiresCapability(capabilityName, type, referenceNames);
            }
        };
    }

    /**
     * Returns a dependency on the specified capability.
     * @param <T> the dependency type
     * @param descriptor the descriptor for the required service
     * @return a dependency supplier
     */
    static <T> ServiceDependency<T> on(NullaryServiceDescriptor<T> descriptor) {
        return new AbstractServiceDependency<>(descriptor.resolve()) {
            @Override
            public Supplier<T> apply(RequirementServiceBuilder<?> builder) {
                return builder.requires(descriptor);
            }
        };
    }

    /**
     * Returns a dependency on the specified unary capability, resolved against the specified reference name.
     * @param <T> the dependency type
     * @param descriptor the descriptor for the required service
     * @param name the reference name
     * @return a dependency supplier
     */
    static <T> ServiceDependency<T> on(UnaryServiceDescriptor<T> descriptor, String name) {
        return new AbstractServiceDependency<>(descriptor.resolve(name)) {
            @Override
            public Supplier<T> apply(RequirementServiceBuilder<?> builder) {
                return builder.requires(descriptor, name);
            }
        };
    }

    /**
     * Returns a dependency on the specified binary capability, resolved against the specified reference names.
     * @param <T> the dependency type
     * @param descriptor the descriptor for the required service
     * @param parentName the parent reference name
     * @param childName the child reference name
     * @return a dependency supplier
     */
    static <T> ServiceDependency<T> on(BinaryServiceDescriptor<T> descriptor, String parentName, String childName) {
        return new AbstractServiceDependency<>(descriptor.resolve(parentName, childName)) {
            @Override
            public Supplier<T> apply(RequirementServiceBuilder<?> builder) {
                return builder.requires(descriptor, parentName, childName);
            }
        };
    }

    /**
     * Returns a dependency on the specified ternary capability, resolved against the specified reference names.
     * @param <T> the dependency type
     * @param descriptor the descriptor for the required service
     * @param ancestorName the ancestor reference name
     * @param parentName the parent reference name
     * @param childName the child reference name
     * @return a dependency supplier
     */
    static <T> ServiceDependency<T> on(TernaryServiceDescriptor<T> descriptor, String ancestorName, String parentName, String childName) {
        return new AbstractServiceDependency<>(descriptor.resolve(ancestorName, parentName, childName)) {
            @Override
            public Supplier<T> apply(RequirementServiceBuilder<?> builder) {
                return builder.requires(descriptor, ancestorName, parentName, childName);
            }
        };
    }

    abstract class AbstractServiceDependency<T> extends AbstractDependency<RequirementServiceBuilder<?>, T> implements ServiceDependency<T> {
        private final Map.Entry<String, String[]> resolved;

        AbstractServiceDependency(Map.Entry<String, String[]> resolved) {
            this.resolved = resolved;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof AbstractServiceDependency)) return false;
            Map.Entry<String, String[]> resolved = ((AbstractServiceDependency<?>) object).resolved;
            return this.resolved.getKey().equals(resolved.getKey()) && Arrays.equals(this.resolved.getValue(), resolved.getValue());
        }

        @Override
        public int hashCode() {
            int result = this.resolved.getKey().hashCode();
            for (String name : this.resolved.getValue()) {
                result = 31 * result + name.hashCode();
            }
            return result;
        }

        @Override
        public String toString() {
            ServiceName name = ServiceNameFactory.parseServiceName(this.resolved.getKey());
            return ((this.resolved.getValue().length > 0) ? name.append(this.resolved.getValue()) : name).getCanonicalName();
        }
    }

    class SimpleServiceDependency<V> extends SimpleDependency<RequirementServiceBuilder<?>, V> implements ServiceDependency<V> {
        static final ServiceDependency<Object> NULL = new SimpleServiceDependency<>(null);

        SimpleServiceDependency(V value) {
            super(value);
        }
    }

    class DefaultServiceDependency<V> extends Dependency.DefaultDependency<RequirementServiceBuilder<?>, V> implements ServiceDependency<V> {

        DefaultServiceDependency(ServiceName name) {
            super(name);
        }
    }
}
