/*
 * Copyright (c) 2014 Yandex
 */

package ru.yandex.yoctodb.query.simple;

import net.jcip.annotations.NotThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.yandex.yoctodb.query.*;
import ru.yandex.yoctodb.util.mutable.BitSet;

import java.util.Iterator;
import java.util.List;

/**
 * Where clause
 *
 * @author incubos
 */
@NotThreadSafe
public final class SimpleOrderClause implements OrderBy {
    @NotNull
    private final SimpleSelect select;
    @NotNull
    private final List<Order> sorts;

    public SimpleOrderClause(
            @NotNull
            final SimpleSelect delegate,
            @NotNull
            final List<Order> sorts) {
        this.select = delegate;
        this.sorts = sorts;
    }

    @NotNull
    @Override
    public OrderBy and(
            @NotNull
            final
            Order order) {
        sorts.add(order);

        return this;
    }

    // Delegated

    @Override
    public int getSkip() {
        return select.getSkip();
    }

    @Override
    public int getLimit() {
        return select.getLimit();
    }

    @Override
    public boolean hasSorting() {
        return select.hasSorting();
    }

    @NotNull
    @Override
    public OrderBy orderBy(
            @NotNull
            final Order order) {
        return select.orderBy(order);
    }

    @NotNull
    @Override
    public Where where(
            @NotNull
            final Condition condition) {
        return select.where(condition);
    }

    @NotNull
    @Override
    public Select skip(final int skip) {
        return select.skip(skip);
    }

    @NotNull
    @Override
    public Select limit(final int limit) {
        return select.limit(limit);
    }

    @Override
    @Nullable
    public BitSet filteredUnlimited(
            @NotNull
            final QueryContext ctx) {
        return select.filteredUnlimited(ctx);
    }

    @Override
    @NotNull
    public Iterator<? extends ScoredDocument<?>> sortedUnlimited(
            @NotNull
            final BitSet docs,
            @NotNull
            final QueryContext ctx) {
        return select.sortedUnlimited(docs, ctx);
    }

    @Override
    public String toString() {
        return "SimpleOrderClause{" +
               "select=" + select +
               ", sorts=" + sorts +
               '}';
    }
}