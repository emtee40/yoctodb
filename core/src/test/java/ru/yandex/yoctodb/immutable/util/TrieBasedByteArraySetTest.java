package ru.yandex.yoctodb.immutable.util;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.yoctodb.util.UnsignedByteArray;
import ru.yandex.yoctodb.util.UnsignedByteArrays;
import ru.yandex.yoctodb.util.mutable.TrieBasedByteArraySet;
import ru.yandex.yoctodb.util.mutable.impl.SimpleTrieBasedByteArraySet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author svyatoslav
 */
public class TrieBasedByteArraySetTest {

    @Test
    public void simpleTest() throws IOException {
        //unsorted elements
        final List<UnsignedByteArray> elements = new ArrayList<UnsignedByteArray>();
        elements.add(UnsignedByteArrays.raw(new byte[]{8, 9, 10, 11}));
        elements.add(UnsignedByteArrays.raw(new byte[]{16, 17, 18, 19}));
        elements.add(UnsignedByteArrays.raw(new byte[]{12, 13, 14, 15}));
        elements.add(UnsignedByteArrays.raw(new byte[]{4, 5, 6, 7}));
        elements.add(UnsignedByteArrays.raw(new byte[]{0, 1, 2, 3}));
        elements.add(UnsignedByteArrays.raw(new byte[]{0, 1, 3, 3}));
        elements.add(UnsignedByteArrays.raw(new byte[]{0, 1, 4, 3}));

        final ByteBuffer bb =
                prepareDataFromTrieBasedByteArraySet(elements);
        final ru.yandex.yoctodb.util.immutable.TrieBasedByteArraySet ss =
                ru.yandex.yoctodb.util.immutable.impl.SimpleTrieBasedByteArraySet.from(bb);

        Assert.assertEquals(elements.size(), ss.size());

        //sorting to compare
        Collections.sort(elements);
        for (int i = 0; i < elements.size(); i++) {
            Assert.assertEquals(i, ss.indexOf(elements.get(i).toByteBuffer()));
        }
        //not contains
        Assert.assertEquals(-1, ss.indexOf(ByteBuffer.wrap(new byte[]{0})));
        Assert.assertEquals(-1, ss.indexOf(ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4})));
        Assert.assertEquals(-1, ss.indexOf(ByteBuffer.wrap(new byte[]{4, 5})));
        Assert.assertEquals(-1, ss.indexOf(ByteBuffer.wrap(new byte[]{2, 3})));
    }

    private ByteBuffer prepareDataFromTrieBasedByteArraySet(
            final Collection<UnsignedByteArray> elements) throws IOException {
        final TrieBasedByteArraySet trieBasedByteArraySet = new SimpleTrieBasedByteArraySet();
        for (UnsignedByteArray element : elements) {
            trieBasedByteArraySet.add(element);
        }

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        trieBasedByteArraySet.writeTo(os);
        Assert.assertEquals(
                os.size(),
                trieBasedByteArraySet.getSizeInBytes());

        return ByteBuffer.wrap(os.toByteArray());
    }

}