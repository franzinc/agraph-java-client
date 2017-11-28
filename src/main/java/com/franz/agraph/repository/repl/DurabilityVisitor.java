package com.franz.agraph.repository.repl;

/**
 * Interface of objects used to process durability levels.
 * <p>
 * A durability level can be either an integer or a symbolic name,
 * so a visitor interface is used handle both cases in a type-safe way.
 *
 * @param <T> Type of values returned by the visitor.
 */
public interface DurabilityVisitor<T> {
    /**
     * Visit an explicit durability setting.
     *
     * @param numberOfInstances Number of instances that must ingest a commit for it
     *                          to be considered durable.
     * @return Result of the visit.
     */
    T visitInteger(int numberOfInstances);

    /**
     * Visit a symbolic durability level.
     *
     * @param level Durability level name.
     * @return Result of the visit.
     */
    T visitDurabilityLevel(DurabilityLevel level);
}
