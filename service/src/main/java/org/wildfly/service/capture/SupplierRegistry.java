/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.service.capture;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The consumer interface for a registry of captured values.
 * @author Paul Ferraro
 */
public interface SupplierRegistry<K, V> {
    /**
     * Returns a supplier providing the value for the specified key, if present, or null otherwise.
     * @param key a registry key
     * @return a supplier providing the value for the specified key, if present, or null otherwise.
     */
    Supplier<V> getReference(K key);

    /**
     * Returns a registry that supplies values from this registry via the specified mapping function.
     * @param <R> the mapped value type
     * @param mapper a mapping function
     * @return a registry that supplies values from this registry via the specified mapping function.
     */
    default <R> SupplierRegistry<K, R> map(Function<? super V, ? extends R> mapper) {
        return new SupplierRegistry<>() {
            @Override
            public Supplier<R> getReference(K key) {
                Supplier<V> reference = SupplierRegistry.this.getReference(key);
                return new Supplier<>() {
                    @Override
                    public R get() {
                        V value = reference.get();
                        return (value != null) ? mapper.apply(value) : null;
                    }
                };
            }
        };
    }
}
