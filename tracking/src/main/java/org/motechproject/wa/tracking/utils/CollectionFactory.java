package org.motechproject.wa.tracking.utils;

import java.util.*;

public abstract class CollectionFactory {

    public abstract Collection<Object> createCollection();

    public static CollectionFactory of(final Class<? extends Collection> clazz) {
        return new CollectionFactory() {
            @Override
            public Collection<Object> createCollection() {
                if (List.class.equals(clazz) || ArrayList.class.equals(clazz)) {
                    return new ArrayList<>();
                } else if (Set.class.equals(clazz) || HashSet.class.equals(clazz)) {
                    return new HashSet<>();
                } else {
                    throw new IllegalArgumentException("Cannot instantiate collection of type " + clazz.getName());
                }
            }
        };

    }
}
