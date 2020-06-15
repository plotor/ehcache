/**
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.ehcache.terracotta;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.management.event.ManagementEventSink;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.transaction.id.TransactionIDFactory;
import net.sf.ehcache.transaction.lock.SoftLockManager;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import java.util.concurrent.Callable;

/**
 * A {@link ClusteredInstanceFactory} implementation that delegates all operations to an underlying delegate except for the following
 * operations:
 * <ul>
 * <li>{@link #getTopology()} : Delegates to the {@link TerracottaClient#getCacheCluster()}</li>
 * </ul>
 *
 * @author Abhishek Sanoujam
 */
public class ClusteredInstanceFactoryWrapper implements ClusteredInstanceFactory {

    private final TerracottaClient client;
    private final ClusteredInstanceFactory delegate;

    /**
     * Constructor accepting the TerracottaClient and the actual factory
     *
     * @param client
     * @param delegate
     */
    public ClusteredInstanceFactoryWrapper(TerracottaClient client, ClusteredInstanceFactory delegate) {
        this.client = client;
        this.delegate = delegate;

    }

    /**
     * Returns the actual underlying factory
     *
     * @return the actual underlying factory
     */
    protected ClusteredInstanceFactory getActualFactory() {
        return delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheCluster getTopology() {
        return client.getCacheCluster();
    }

    // all methods below delegate to the real factory

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUUID() {
        return delegate.getUUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableNonStopForCurrentThread(boolean enable) {
        delegate.enableNonStopForCurrentThread(enable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheEventListener createEventReplicator(Ehcache cache) {
        return delegate.createEventReplicator(cache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Store createStore(Ehcache cache) {
        return delegate.createStore(cache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionIDFactory createTransactionIDFactory(String uuid, String cacheManagerName) {
        return delegate.createTransactionIDFactory(uuid, cacheManagerName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WriteBehind createWriteBehind(Ehcache cache) {
        return delegate.createWriteBehind(cache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SoftLockManager getOrCreateSoftLockManager(Ehcache cache) {
        return delegate.getOrCreateSoftLockManager(cache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public TerracottaStore createNonStopStore(Callable<TerracottaStore> store, Ehcache cache) {
        return delegate.createNonStopStore(store, cache);
    }

    @Override
    public boolean destroyCache(final String cacheManagerName, final String cacheName) {
        return delegate.destroyCache(cacheManagerName, cacheName);
    }

    @Override
    public void linkClusteredCacheManager(String cacheManagerName, Configuration configuration) {
        delegate.linkClusteredCacheManager(cacheManagerName, configuration);
    }

    @Override
    public void unlinkCache(String cacheName) {
        delegate.unlinkCache(cacheName);
    }

    @Override
    public ManagementEventSink createEventSink() {
        return delegate.createEventSink();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitForOrchestrator(String cacheManagerName) {
        delegate.waitForOrchestrator(cacheManagerName);
    }
}
