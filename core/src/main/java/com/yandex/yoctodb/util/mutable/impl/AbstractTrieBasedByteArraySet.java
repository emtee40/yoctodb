/*
 * Copyright © 2014 Yandex
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file or
 * http://www.wtfpl.net/ for more details.
 */

package com.yandex.yoctodb.util.mutable.impl;

import net.jcip.annotations.NotThreadSafe;
import org.jetbrains.annotations.NotNull;
import com.yandex.yoctodb.util.UnsignedByteArray;
import com.yandex.yoctodb.util.mutable.TrieBasedByteArraySet;

import java.util.*;

/**
 * @author svyatoslav
 */
@NotThreadSafe
abstract class AbstractTrieBasedByteArraySet implements TrieBasedByteArraySet {
    public static final int COMPRESSION_TRESHOLD = 255;
    protected Map<UnsignedByteArray, UnsignedByteArray> elements =
            new HashMap<UnsignedByteArray, UnsignedByteArray>();
    protected boolean frozen = false;
    protected TrieNode root = new TrieNode();
    protected Map<UnsignedByteArray, Integer> sortedElements = null;
    protected int trieSizeInBytes = 0;

    protected void build() {
        assert !frozen;

        // Sorting
        final UnsignedByteArray[] sorted =
                elements.keySet().toArray(new UnsignedByteArray[elements.size()]);
        Arrays.sort(sorted);

        // Releasing resources
        elements = null;

        // Copying
        sortedElements = new LinkedHashMap<UnsignedByteArray, Integer>(sorted.length);
        int i = 0;
        for (UnsignedByteArray e : sorted) {
            sortedElements.put(e, i++);
        }

        // Freezing
        frozen = true;

        for (Map.Entry<UnsignedByteArray, Integer> entry : sortedElements.entrySet()) {
            insertByteArray(entry.getKey(), entry.getValue());
        }
        updateOffsets();
    }

    protected void insertByteArray(UnsignedByteArray byteArray, int position) {
        TrieNode currentNode = root;
        for (byte currentByte : byteArray) {
            if (!currentNode.children.containsKey(currentByte)) {
                currentNode.children.put(currentByte, new TrieNode());
                if (currentByte > currentNode.getMaxByteJmpValue()) {
                    currentNode.setMaxByteJmpValue(currentByte);
                }
                if (currentByte < currentNode.getMinByteJmpValue()) {
                    currentNode.setMinByteJmpValue(currentByte);
                }
            }
            currentNode = currentNode.children.get(currentByte);
        }
        currentNode.setPosition(position);
    }


    protected void updateOffsets() {
        Queue<TrieNode> queue = new LinkedList<TrieNode>();
        queue.add(root);
        int offset = 0;
        while (queue.size() > 0) {
            TrieNode node = queue.poll();
            if (node.getChildren().size() == 0 || node.getChildren().size() < COMPRESSION_TRESHOLD) {
                node.setCompressed(true);
            }

            node.setOffset(offset);
            if (node.isCompressed()) {
                node.setEnds(offset
                        + 1  //is terminal node
                        + (node.position >= 0 ? 4 : 0) // position of key in sorted set
                        + 1 //compression
                        + 4 //count of child
                        + node.getChildren().size() * (1 + 4) //children cnt * (jmp value + jmp destination)
                );

            } else {
                int countOfJmpsInRange = node.getChildren().size() == 0 ? 0
                        : ((int) node.getMaxByteJmpValue()) - ((int) node.getMinByteJmpValue()) + 1;
                node.setEnds(offset
                        + 1     //is terminal node
                        + (node.position >= 0 ? 4 : 0) // position of key in sorted set
                        + 2     //two bytes for min and max jmp value
                        + 1    //compression
                        + 4 * (countOfJmpsInRange)  //jmps destination
                );
            }

            offset = node.getEnds();
            for (TrieNode child : node.getChildren().values()) {
                queue.add(child);
            }
        }
        trieSizeInBytes = offset;
    }


    @NotNull
    @Override
    public UnsignedByteArray add(
            @NotNull
            final UnsignedByteArray e) {
        assert e.length() > 0;

        if (frozen)
            throw new IllegalStateException("The collection is frozen");

        final UnsignedByteArray previous = elements.get(e);
        if (previous == null) {
            elements.put(e, e);
            return e;
        } else {
            return previous;
        }
    }

    protected class TrieNode {
        private Map<Byte, TrieNode> children = new TreeMap<Byte, TrieNode>();
        //if position == -1 - this node not terminal;
        // else - position equals to position of key in sorted list;
        private int position = -1;
        private int offset = -1;
        private int ends = -1;
        private byte minByteJmpValue = Byte.MAX_VALUE;
        private byte maxByteJmpValue = Byte.MIN_VALUE;
        private boolean isCompressed = false;

        public Map<Byte, TrieNode> getChildren() {
            return children;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public boolean isCompressed() {
            return isCompressed;
        }

        public int getEnds() {
            return ends;
        }

        public void setEnds(int ends) {
            this.ends = ends;
        }

        public void setCompressed(boolean isCompressed) {
            this.isCompressed = isCompressed;
        }

        public byte getMinByteJmpValue() {
            return minByteJmpValue;
        }

        public void setMinByteJmpValue(byte minByteJmpValue) {
            this.minByteJmpValue = minByteJmpValue;
        }

        public byte getMaxByteJmpValue() {
            return maxByteJmpValue;
        }

        public void setMaxByteJmpValue(byte maxByteJmpValue) {
            this.maxByteJmpValue = maxByteJmpValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TrieNode trieNode = (TrieNode) o;

            if (ends != trieNode.ends) return false;
            if (isCompressed != trieNode.isCompressed) return false;
            if (offset != trieNode.offset) return false;
            if (position != trieNode.position) return false;
            if (children != null ? !children.equals(trieNode.children) : trieNode.children != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = children != null ? children.hashCode() : 0;
            result = 31 * result + position;
            result = 31 * result + offset;
            result = 31 * result + ends;
            result = 31 * result + (isCompressed ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TrieNode{" +
                    "children=" + children +
                    ", position=" + position +
                    ", offset=" + offset +
                    ", ends=" + ends +
                    ", isCompressed=" + isCompressed +
                    '}';
        }
    }

}