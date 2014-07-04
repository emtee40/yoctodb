/*
 * Copyright (c) 2014 Yandex
 */

package ru.yandex.yoctodb.v1.immutable.segment;

import net.jcip.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import ru.yandex.yoctodb.immutable.FilterableIndex;
import ru.yandex.yoctodb.util.immutable.IntToIntArray;
import ru.yandex.yoctodb.immutable.SortableIndex;
import ru.yandex.yoctodb.util.immutable.ByteArraySortedSet;
import ru.yandex.yoctodb.util.immutable.IndexToIndexMap;
import ru.yandex.yoctodb.util.immutable.IndexToIndexMultiMap;
import ru.yandex.yoctodb.util.immutable.impl.*;
import ru.yandex.yoctodb.util.mutable.BitSet;
import ru.yandex.yoctodb.v1.V1DatabaseFormat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Immutable implementation of {@link FilterableIndex} and {@link
 * SortableIndex}
 *
 * @author incubos
 */
@Immutable
public final class V1FullIndex
        implements FilterableIndex, SortableIndex, Segment {
    @NotNull
    private final String fieldName;
    @NotNull
    private final V1FilterableIndex filterableDelegate;
    @NotNull
    private final ByteArraySortedSet values;
    @NotNull
    private final IndexToIndexMultiMap valueToDocuments;
    @NotNull
    private final IndexToIndexMap documentToValues;

    private V1FullIndex(
            @NotNull
            final String fieldName,
            @NotNull
            final ByteArraySortedSet values,
            @NotNull
            final IndexToIndexMultiMap valueToDocuments,
            @NotNull
            final IndexToIndexMap documentToValues) {
        assert !fieldName.isEmpty();

        // May be constructed only from SegmentReader
        this.fieldName = fieldName;
        this.filterableDelegate =
                new V1FilterableIndex(fieldName, values, valueToDocuments);
        this.values = values;
        this.valueToDocuments = valueToDocuments;
        this.documentToValues = documentToValues;
    }

    @NotNull
    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean eq(
            @NotNull
            final BitSet dest,
            @NotNull
            final ByteBuffer value) {
        return filterableDelegate.eq(dest, value);
    }

    @Override
    public boolean in(
            @NotNull
            final BitSet dest,
            @NotNull
            final ByteBuffer... value) {
        return filterableDelegate.in(dest, value);
    }

    @Override
    public boolean lessThan(
            @NotNull
            final BitSet dest,
            @NotNull
            final ByteBuffer value,
            final boolean orEquals) {
        return filterableDelegate.lessThan(dest, value, orEquals);
    }

    @Override
    public boolean greaterThan(
            @NotNull
            final BitSet dest,
            @NotNull
            final ByteBuffer value,
            final boolean orEquals) {
        return filterableDelegate.greaterThan(dest, value, orEquals);
    }

    @Override
    public boolean between(
            @NotNull
            final BitSet dest,
            @NotNull
            final ByteBuffer from,
            final boolean fromInclusive,
            @NotNull
            final ByteBuffer to,
            final boolean toInclusive) {
        return filterableDelegate.between(
                dest,
                from,
                fromInclusive,
                to,
                toInclusive);
    }

    @Override
    public int getSortValueIndex(final int document) {
        return documentToValues.get(document);
    }

    @NotNull
    @Override
    public ByteBuffer getSortValue(final int index) {
        return values.get(index);
    }

    @NotNull
    @Override
    public Iterator<IntToIntArray> ascending(
            @NotNull
            final BitSet docs) {
        return valueToDocuments.ascending(docs);
    }

    @NotNull
    @Override
    public Iterator<IntToIntArray> descending(
            @NotNull
            final BitSet docs) {
        return valueToDocuments.descending(docs);
    }

    static void registerReader() {
        SegmentRegistry.register(
                V1DatabaseFormat.SegmentType.FIXED_LENGTH_FULL_INDEX.getCode(),
                new SegmentReader() {
                    @NotNull
                    @Override
                    public Segment read(
                            @NotNull
                            final ByteBuffer buffer) throws IOException {

                        final byte[] digest = Segments.calculateDigest(buffer, V1DatabaseFormat.MESSAGE_DIGEST_ALGORITHM);

                        final String fieldName = Segments.extractString(buffer);

                        final ByteArraySortedSet values =
                                FixedLengthByteArraySortedSet.from(
                                        Segments.extract(buffer));

                        final IndexToIndexMultiMap valueToDocuments =
                                IndexToIndexMultiMapReader.from(
                                        Segments.extract(buffer));

                        final IndexToIndexMap documentToValues =
                                IntIndexToIndexMap.from(
                                        Segments.extract(buffer));

                        final ByteBuffer digestActual = Segments.extract(buffer);
                        if (!digestActual.equals(ByteBuffer.wrap(digest))) {
                            throw new CorruptSegmentException("checksum error");
                        }

                        return new V1FullIndex(
                                fieldName,
                                values,
                                valueToDocuments,
                                documentToValues);
                    }


                });

        SegmentRegistry.register(
                V1DatabaseFormat.SegmentType
                        .VARIABLE_LENGTH_FULL_INDEX
                        .getCode(),
                new SegmentReader() {
                    @NotNull
                    @Override
                    public Segment read(
                            @NotNull
                            final ByteBuffer buffer) throws IOException {

                        final byte[] digest = Segments.calculateDigest(buffer, V1DatabaseFormat.MESSAGE_DIGEST_ALGORITHM);

                        final String fieldName = Segments.extractString(buffer);

                        final ByteArraySortedSet values =
                                VariableLengthByteArraySortedSet.from(
                                        Segments.extract(buffer));

                        final IndexToIndexMultiMap valueToDocuments =
                                IndexToIndexMultiMapReader.from(
                                        Segments.extract(buffer));

                        final IndexToIndexMap documentToValues =
                                IntIndexToIndexMap.from(
                                        Segments.extract(buffer));

                        final ByteBuffer digestActual = Segments.extract(buffer);
                        if (!digestActual.equals(ByteBuffer.wrap(digest))) {
                            throw new CorruptSegmentException("checksum error");
                        }

                        return new V1FullIndex(
                                fieldName,
                                values,
                                valueToDocuments,
                                documentToValues);
                    }
                });
    }
}