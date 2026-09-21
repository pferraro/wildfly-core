/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.service.capture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A registry of captured values with {@link ValueRegistry provider-side} and {@link SupplierRegistry consumer-side} interfaces.
 * This is intended as a lightweight alternative to fixed value MSC services.
 * @author Paul Ferraro
 */
public interface SupplierValueRegistry<K, V> extends ValueRegistry<K, V>, SupplierRegistry<K, V> {

    /**
     * Creates a new registry of values.
     * @param <K> the registry key type
     * @param <V> the registry value type
     * @return a new registry instance
     */
    static <K, V> SupplierValueRegistry<K, V> newInstance() {
        Map<K, AtomicReference<V>> references = new ConcurrentHashMap<>();
        Function<K, AtomicReference<V>> factory = new Function<>() {
            @Override
            public AtomicReference<V> apply(K key) {
                return new AtomicReference<>();
            }
        };
        return new SupplierValueRegistry<>() {
            @Override
            public Consumer<V> add(K key) {
                return references.computeIfAbsent(key, factory)::set;
            }

            @Override
            public void remove(K key) {
                AtomicReference<V> reference = references.remove(key);
                if (reference != null) {
                    reference.set(null);
                }
            }

            @Override
            public Supplier<V> getReference(K key) {
                return new Supplier<>() {
                    @Override
                    public V get() {
                        AtomicReference<V> reference = references.get(key);
                        return (reference != null) ? reference.get() : null;
                    }
                };
            }
        };
    }
}
