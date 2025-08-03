package org.valle.persist;

public interface PersistResult<T> {
    void persist(T toPersist);
}
