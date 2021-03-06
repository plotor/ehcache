/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.search.impl;

import java.util.Collections;
import java.util.List;

import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;

/**
 * Results implementation
 *
 * @author teck
 */
public class ResultsImpl implements Results {

    private final List<Result> results;
    private final boolean hasKeys;
    private final boolean hasAttributes;
    private final boolean hasAggregators;
    private final boolean hasValues;
    private final boolean empty;
    private final Runnable discardHook;

    /**
     * Constructor
     *
     * @param results
     * @param hasKeys
     * @param hasAttributes
     * @param hasAggregators
     */
    public ResultsImpl(List<? extends Result> results, boolean hasKeys, boolean hasValues, boolean hasAttributes, boolean hasAggregators) {
        this(results, hasKeys, hasValues, hasAttributes, hasAggregators, null);
    }

    /**
     * Constructor
     *
     * @param results
     * @param hasKeys
     * @param hasAttributes
     * @param hasAggregators
     */
    public ResultsImpl(List<? extends Result> results, boolean hasKeys, boolean hasValues, boolean hasAttributes, boolean hasAggregators, 
            Runnable discardCallback) {
        this.hasKeys = hasKeys;
        this.hasValues = hasValues;
        this.hasAttributes = hasAttributes;
        this.hasAggregators = hasAggregators;
        this.results = Collections.unmodifiableList(results);
        this.empty = results.isEmpty();
        this.discardHook = discardCallback;
    }

    @Override
    public String toString() {
        return "Results(size=" + size() + ", hasKeys=" + hasKeys() + ", hasValues=" + hasValues()
                + ", hasAttributes=" + hasAttributes() + ", hasAggregators=" + hasAggregators() + ")";
    }

    /**
     * {@inheritDoc}
     */
    public void discard() {
        if (discardHook != null) {
            discardHook.run();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Result> all() throws SearchException {
        return results;
    }

    /**
     * {@inheritDoc}
     */
    public List<Result> range(int start, int length) throws SearchException {
        if (start < 0) {
            throw new IllegalArgumentException("start: " + start);
        }

        if (length < 0) {
            throw new IllegalArgumentException("length: " + length);
        }

        int size = results.size();

        if (start > size - 1 || length == 0) {
            return Collections.emptyList();
        }

        int end = start + length;

        if (end > size) {
            end = size;
        }

        return results.subList(start, end);
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return results.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasKeys() {
        if (empty) {
            return false;
        }
        return hasKeys;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasValues() {
        if (empty) {
            return false;
        }

        return hasValues;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAttributes() {
        if (empty) {
            return false;
        }

        return hasAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAggregators() {
        if (empty) {
            return false;
        }
        return hasAggregators;
    }

}
