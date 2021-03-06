/*
 * (C) YANDEX LLC, 2014-2016
 *
 * The Source Code called "YoctoDB" available at
 * https://github.com/yandex/yoctodb is subject to the terms of the
 * Mozilla Public License, v. 2.0 (hereinafter referred to as the "License").
 *
 * A copy of the License is also available at http://mozilla.org/MPL/2.0/.
 */

package com.yandex.yoctodb.util.immutable.impl;

import com.yandex.yoctodb.util.buf.Buffer;
import net.jcip.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import com.yandex.yoctodb.util.immutable.ByteArrayIndexedList;

/**
 * Fixed length immutable implementation of {@link ByteArrayIndexedList}
 *
 * @author incubos
 */
@Immutable
public class FixedLengthByteArrayIndexedList
        implements ByteArrayIndexedList {
    private final int elementSize;
    private final int elementCount;
    private final Buffer elements;

    @NotNull
    public static ByteArrayIndexedList from(
            @NotNull
            final Buffer buf) {
        final int elementSize = buf.getInt();
        final int elementCount = buf.getInt();

        return new FixedLengthByteArrayIndexedList(
                elementSize,
                elementCount,
                buf.slice());
    }

    private FixedLengthByteArrayIndexedList(
            final int elementSize,
            final int elementCount,
            final Buffer elements) {
        assert elementSize >= 0 : "Negative element size";
        assert elementCount >= 0 : "Negative element count";

        this.elementSize = elementSize;
        this.elementCount = elementCount;
        this.elements = elements;
    }

    @NotNull
    @Override
    public Buffer get(final int i) {
        assert 0 <= i && i < elementCount;

        return elements.slice(((long) i) * elementSize, elementSize);
    }

    @Override
    public int size() {
        return elementCount;
    }

    @Override
    public String toString() {
        return "FixedLengthByteArrayIndexedList{" +
                "elementSize=" + elementSize +
                ", elementCount=" + elementCount +
                '}';
    }
}
