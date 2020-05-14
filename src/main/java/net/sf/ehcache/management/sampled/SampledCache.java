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

package net.sf.ehcache.management.sampled;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.hibernate.management.impl.BaseEmitterBean;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

/**
 * An implementation of {@link SampledCacheMBean}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public class SampledCache extends BaseEmitterBean implements SampledCacheMBean, PropertyChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(SampledCache.class);

    private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

    private final CacheSamplerImpl sampledCacheDelegate;

    private final String immutableCacheName;

    static {
        final String[] notifTypes = new String[] {CACHE_ENABLED, CACHE_CHANGED, CACHE_FLUSHED,};
        final String name = Notification.class.getName();
        final String description = "Ehcache SampledCache Event";
        NOTIFICATION_INFO = new MBeanNotificationInfo[] {new MBeanNotificationInfo(notifTypes, name, description)};
    }

    /**
     * Constructor accepting the backing {@link Ehcache}
     *
     * @param cache the cache object to use in initializing this sampled jmx mbean
     * @throws NotCompliantMBeanException if object doesn't comply with mbean spec
     */
    public SampledCache(Ehcache cache) throws NotCompliantMBeanException {
        super(SampledCacheMBean.class);
        immutableCacheName = cache.getName();

        cache.addPropertyChangeListener(this);
        this.sampledCacheDelegate = new CacheSamplerImpl(cache);
    }

    /**
     * Method which returns the name of the cache at construction time.
     * Package protected method.
     *
     * @return The name of the cache
     */
    String getImmutableCacheName() {
        return immutableCacheName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return sampledCacheDelegate.isEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(boolean enabled) {
        sampledCacheDelegate.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClusterBulkLoadEnabled() {
        return sampledCacheDelegate.isClusterBulkLoadEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNodeBulkLoadEnabled() {
        return sampledCacheDelegate.isNodeBulkLoadEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNodeBulkLoadEnabled(boolean bulkLoadEnabled) {
        sampledCacheDelegate.setNodeBulkLoadEnabled(bulkLoadEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        sampledCacheDelegate.flush();
        this.sendNotification(CACHE_FLUSHED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return sampledCacheDelegate.getCacheName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatus() {
        return sampledCacheDelegate.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() {
        sampledCacheDelegate.removeAll();
        this.sendNotification(CACHE_CLEARED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    @Override
    public long getAverageGetTimeNanosMostRecentSample() {
        return sampledCacheDelegate.getAverageGetTimeNanosMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheEvictionRate() {
        return sampledCacheDelegate.getCacheEvictionRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementEvictedMostRecentSample() {
        return sampledCacheDelegate.getCacheElementEvictedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheExpirationRate() {
        return sampledCacheDelegate.getCacheExpirationRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementExpiredMostRecentSample() {
        return sampledCacheDelegate.getCacheElementExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCachePutRate() {
        return sampledCacheDelegate.getCachePutRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementPutMostRecentSample() {
        return sampledCacheDelegate.getCacheElementPutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheRemoveRate() {
        return sampledCacheDelegate.getCacheRemoveRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementRemovedMostRecentSample() {
        return sampledCacheDelegate.getCacheElementRemovedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheUpdateRate() {
        return this.getCacheElementUpdatedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return sampledCacheDelegate.getCacheElementUpdatedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheInMemoryHitRate() {
        return this.getCacheHitInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return sampledCacheDelegate.getCacheHitInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheOffHeapHitRate() {
        return sampledCacheDelegate.getCacheOffHeapHitRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOffHeapMostRecentSample() {
        return sampledCacheDelegate.getCacheHitOffHeapMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitRate() {
        return sampledCacheDelegate.getCacheHitRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitMostRecentSample() {
        return sampledCacheDelegate.getCacheHitMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheOnDiskHitRate() {
        return sampledCacheDelegate.getCacheOnDiskHitRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return sampledCacheDelegate.getCacheHitOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissExpiredMostRecentSample() {
        return sampledCacheDelegate.getCacheMissExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissRate() {
        return sampledCacheDelegate.getCacheMissRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissMostRecentSample() {
        return sampledCacheDelegate.getCacheMissMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheInMemoryMissRate() {
        return sampledCacheDelegate.getCacheInMemoryMissRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissInMemoryMostRecentSample() {
        return sampledCacheDelegate.getCacheMissInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheOffHeapMissRate() {
        return sampledCacheDelegate.getCacheOffHeapMissRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissOffHeapMostRecentSample() {
        return sampledCacheDelegate.getCacheMissOffHeapMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheOnDiskMissRate() {
        return sampledCacheDelegate.getCacheOnDiskMissRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissOnDiskMostRecentSample() {
        return sampledCacheDelegate.getCacheMissOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return sampledCacheDelegate.getCacheMissNotFoundMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTerracottaClustered() {
        return sampledCacheDelegate.isTerracottaClustered();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTerracottaConsistency() {
        return sampledCacheDelegate.getTerracottaConsistency();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #setNodeBulkLoadEnabled(boolean)} instead
     */
    @Override
    @Deprecated
    public void setNodeCoherent(boolean coherent) {
        boolean isNodeCoherent = this.isNodeCoherent();
        if (coherent != isNodeCoherent) {
            if (!coherent && this.getTransactional()) {
                LOG.warn("a transactional cache cannot be incoherent");
                return;
            }
            try {
                sampledCacheDelegate.getCache().setNodeCoherent(coherent);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public boolean isClusterCoherent() {
        try {
            return sampledCacheDelegate.getCache().isClusterCoherent();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public boolean isNodeCoherent() {
        try {
            return sampledCacheDelegate.getCache().isNodeCoherent();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getAverageGetTimeNanos()
     */
    public long getAverageGetTimeNanos() {
        return sampledCacheDelegate.getAverageGetTimeNanos();
    }

    /**
     * {@inheritDoc}
     *
     * @see LegacyCacheStatistics#getMaxGetTimeNanos()
     */
    public Long getMaxGetTimeNanos() {
        return sampledCacheDelegate.getMaxGetTimeNanos();
    }

    /**
     * {@inheritDoc}
     *
     * @see LegacyCacheStatistics#getMinGetTimeNanos()
     */
    public Long getMinGetTimeNanos() {
        return sampledCacheDelegate.getMinGetTimeNanos();
    }

    /**
     * {@inheritDoc}
     *
     * @see LegacyCacheStatistics#getXaCommitCount()
     */
    public long getXaCommitCount() {
        return sampledCacheDelegate.getXaCommitCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see LegacyCacheStatistics#getXaRollbackCount()
     */
    public long getXaRollbackCount() {
        return sampledCacheDelegate.getXaRollbackCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see LegacyCacheStatistics#getXaRecoveredCount()
     */
    public long getXaRecoveredCount() {
        return sampledCacheDelegate.getXaRecoveredCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getHasWriteBehindWriter() {
        return sampledCacheDelegate.getHasWriteBehindWriter();
    }

    /**
     * {@inheritDoc}
     *
     * @see LegacyCacheStatistics#getWriterQueueLength()
     */
    @Override
    public long getWriterQueueLength() {
        return sampledCacheDelegate.getWriterQueueLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWriterMaxQueueSize() {
        return sampledCacheDelegate.getWriterMaxQueueSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWriterConcurrency() {
        return sampledCacheDelegate.getWriterConcurrency();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheHitCount()
     */
    public long getCacheHitCount() {
        return sampledCacheDelegate.getCacheHitCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheMissCount()
     */
    public long getCacheMissCount() {
        return sampledCacheDelegate.getCacheMissCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemoryMissCount()
     */
    public long getInMemoryMissCount() {
        return sampledCacheDelegate.getInMemoryMissCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOffHeapMissCount()
     */
    public long getOffHeapMissCount() {
        return sampledCacheDelegate.getOffHeapMissCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskMissCount()
     */
    public long getOnDiskMissCount() {
        return sampledCacheDelegate.getOnDiskMissCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheMissCountExpired()
     */
    public long getCacheMissCountExpired() {
        return sampledCacheDelegate.getCacheMissCountExpired();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getDiskExpiryThreadIntervalSeconds()
     */
    @Override
    public long getDiskExpiryThreadIntervalSeconds() {
        return sampledCacheDelegate.getDiskExpiryThreadIntervalSeconds();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setDiskExpiryThreadIntervalSeconds(long)
     */
    @Override
    public void setDiskExpiryThreadIntervalSeconds(long seconds) {
        sampledCacheDelegate.setDiskExpiryThreadIntervalSeconds(seconds);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxEntriesLocalHeap()
     */
    @Override
    public long getMaxEntriesLocalHeap() {
        return sampledCacheDelegate.getMaxEntriesLocalHeap();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setMaxEntriesLocalHeap(long)
     */
    @Override
    public void setMaxEntriesLocalHeap(long maxEntries) {
        sampledCacheDelegate.setMaxEntriesLocalHeap(maxEntries);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxBytesLocalHeap()
     */
    @Override
    public long getMaxBytesLocalHeap() {
        return sampledCacheDelegate.getMaxBytesLocalHeap();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setMaxBytesLocalHeap(long)
     */
    @Override
    public void setMaxBytesLocalHeap(long maxBytes) {
        sampledCacheDelegate.setMaxBytesLocalHeap(maxBytes);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setMaxBytesLocalHeapAsString(String)
     */
    @Override
    public void setMaxBytesLocalHeapAsString(String maxBytes) {
        sampledCacheDelegate.setMaxBytesLocalHeapAsString(maxBytes);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxBytesLocalHeapAsString()
     */
    @Override
    public String getMaxBytesLocalHeapAsString() {
        return sampledCacheDelegate.getMaxBytesLocalHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxElementsInMemory() {
        return sampledCacheDelegate.getCache().getCacheConfiguration().getMaxElementsInMemory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxElementsInMemory(int maxElements) {
        if (this.getMaxElementsInMemory() != maxElements) {
            try {
                sampledCacheDelegate.getCache().getCacheConfiguration().setMaxElementsInMemory(maxElements);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxEntriesLocalDisk()
     */
    @Override
    public long getMaxEntriesLocalDisk() {
        return sampledCacheDelegate.getMaxEntriesLocalDisk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaxEntriesInCache() {
        return sampledCacheDelegate.getMaxEntriesInCache();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setMaxEntriesLocalDisk(long)
     */
    @Override
    public void setMaxEntriesLocalDisk(long maxEntries) {
        sampledCacheDelegate.setMaxEntriesLocalDisk(maxEntries);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setMaxBytesLocalDisk(long)
     */
    @Override
    public void setMaxBytesLocalDisk(long maxBytes) {
        sampledCacheDelegate.setMaxBytesLocalDisk(maxBytes);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setMaxBytesLocalDiskAsString(String)
     */
    @Override
    public void setMaxBytesLocalDiskAsString(String maxBytes) {
        sampledCacheDelegate.setMaxBytesLocalDiskAsString(maxBytes);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxBytesLocalDiskAsString()
     */
    @Override
    public String getMaxBytesLocalDiskAsString() {
        return sampledCacheDelegate.getMaxBytesLocalDiskAsString();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxElementsOnDisk()
     */
    @Override
    public int getMaxElementsOnDisk() {
        return sampledCacheDelegate.getMaxElementsOnDisk();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setMaxElementsOnDisk(int)
     */
    @Override
    public void setMaxElementsOnDisk(int maxElements) {
        sampledCacheDelegate.setMaxElementsOnDisk(maxElements);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxEntriesInCache(long maxEntries) {
        sampledCacheDelegate.setMaxEntriesInCache(maxEntries);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxBytesLocalDisk()
     */
    @Override
    public long getMaxBytesLocalDisk() {
        return sampledCacheDelegate.getMaxBytesLocalDisk();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxBytesLocalOffHeap()
     */
    @Override
    public long getMaxBytesLocalOffHeap() {
        return sampledCacheDelegate.getMaxBytesLocalOffHeap();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMaxBytesLocalOffHeapAsString()
     */
    @Override
    public String getMaxBytesLocalOffHeapAsString() {
        return sampledCacheDelegate.getMaxBytesLocalOffHeapAsString();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getMemoryStoreEvictionPolicy()
     */
    @Override
    public String getMemoryStoreEvictionPolicy() {
        return sampledCacheDelegate.getMemoryStoreEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setMemoryStoreEvictionPolicy(String)
     */
    @Override
    public void setMemoryStoreEvictionPolicy(String evictionPolicy) {
        sampledCacheDelegate.setMemoryStoreEvictionPolicy(evictionPolicy);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getTimeToIdleSeconds()
     */
    @Override
    public long getTimeToIdleSeconds() {
        return sampledCacheDelegate.getTimeToIdleSeconds();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setTimeToIdleSeconds(long)
     */
    @Override
    public void setTimeToIdleSeconds(long tti) {
        sampledCacheDelegate.setTimeToIdleSeconds(tti);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getTimeToLiveSeconds()
     */
    @Override
    public long getTimeToLiveSeconds() {
        return sampledCacheDelegate.getTimeToLiveSeconds();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setTimeToLiveSeconds(long)
     */
    @Override
    public void setTimeToLiveSeconds(long ttl) {
        sampledCacheDelegate.setTimeToLiveSeconds(ttl);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isOverflowToOffHeap()
     */
    @Override
    public boolean isOverflowToOffHeap() {
        return sampledCacheDelegate.isOverflowToOffHeap();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isDiskPersistent()
     */
    @Override
    public boolean isDiskPersistent() {
        return sampledCacheDelegate.isDiskPersistent();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setDiskPersistent(boolean)
     */
    @Override
    public void setDiskPersistent(boolean diskPersistent) {
        sampledCacheDelegate.setDiskPersistent(diskPersistent);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getPersistenceStrategy
     */
    @Override
    public String getPersistenceStrategy() {
        return sampledCacheDelegate.getPersistenceStrategy();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isEternal()
     */
    @Override
    public boolean isEternal() {
        return sampledCacheDelegate.isEternal();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setEternal(boolean)
     */
    @Override
    public void setEternal(boolean eternal) {
        sampledCacheDelegate.setEternal(eternal);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isOverflowToDisk()
     */
    @Override
    public boolean isOverflowToDisk() {
        return sampledCacheDelegate.isOverflowToDisk();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setOverflowToDisk(boolean)
     */
    @Override
    public void setOverflowToDisk(boolean overflowToDisk) {
        sampledCacheDelegate.setOverflowToDisk(overflowToDisk);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isLoggingEnabled()
     */
    @Override
    public boolean isLoggingEnabled() {
        return sampledCacheDelegate.isLoggingEnabled();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setLoggingEnabled(boolean)
     */
    @Override
    public void setLoggingEnabled(boolean enabled) {
        sampledCacheDelegate.setLoggingEnabled(enabled);
        this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isPinned()
     */
    @Override
    public boolean isPinned() {
        return sampledCacheDelegate.isPinned();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getPinnedToStore()
     */
    @Override
    public String getPinnedToStore() {
        return sampledCacheDelegate.getPinnedToStore();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getEvictedCount()
     */
    public long getEvictedCount() {
        return sampledCacheDelegate.getEvictedCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getExpiredCount()
     */
    public long getExpiredCount() {
        return sampledCacheDelegate.getExpiredCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemoryHitCount()
     */
    public long getInMemoryHitCount() {
        return sampledCacheDelegate.getInMemoryHitCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see SampledCache#getInMemorySize()
     * @deprecated use {@link #getLocalHeapSize()}
     */
    @Deprecated
    public long getInMemorySize() {
        return this.getLocalHeapSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOffHeapHitCount()
     */
    public long getOffHeapHitCount() {
        return sampledCacheDelegate.getOffHeapHitCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOffHeapSize()
     * @deprecated use {@link #getLocalOffHeapSize()}
     */
    @Deprecated
    public long getOffHeapSize() {
        return sampledCacheDelegate.getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskHitCount()
     */
    public long getOnDiskHitCount() {
        return sampledCacheDelegate.getOnDiskHitCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskSize()
     * @deprecated use {@link #getLocalDiskSize()}
     */
    @Deprecated
    public long getOnDiskSize() {
        return sampledCacheDelegate.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalDiskSize()
     */
    public long getLocalDiskSize() {
        return sampledCacheDelegate.getLocalDiskSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalHeapSize()
     */
    public long getLocalHeapSize() {
        return sampledCacheDelegate.getLocalHeapSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalOffHeapSize()
     */
    public long getLocalOffHeapSize() {
        return sampledCacheDelegate.getLocalOffHeapSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalDiskSizeInBytes()
     */
    public long getLocalDiskSizeInBytes() {
        return sampledCacheDelegate.getLocalDiskSizeInBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalHeapSizeInBytes()
     */
    public long getLocalHeapSizeInBytes() {
        return sampledCacheDelegate.getLocalHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalOffHeapSizeInBytes()
     */
    public long getLocalOffHeapSizeInBytes() {
        return sampledCacheDelegate.getLocalOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getPutCount()
     */
    public long getPutCount() {
        return sampledCacheDelegate.getPutCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getRemovedCount()
     */
    public long getRemovedCount() {
        return sampledCacheDelegate.getRemovedCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getSize()
     */
    public long getSize() {
        return sampledCacheDelegate.getSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getUpdateCount()
     */
    public long getUpdateCount() {
        return sampledCacheDelegate.getUpdateCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getReplaceOneArgSuccessCount() {
        return sampledCacheDelegate.getReplaceOneArgSuccessCount();
    }

    @Override
    public long getReplaceOneArgSuccessRate() {
        return sampledCacheDelegate.getReplaceOneArgSuccessRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getReplaceOneArgMissCount() {
        return sampledCacheDelegate.getReplaceOneArgMissCount();
    }

    @Override
    public long getReplaceOneArgMissRate() {
        return sampledCacheDelegate.getReplaceOneArgMissRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getReplaceTwoArgSuccessCount() {
        return sampledCacheDelegate.getReplaceTwoArgSuccessCount();
    }

    @Override
    public long getReplaceTwoArgSuccessRate() {
        return sampledCacheDelegate.getReplaceTwoArgSuccessRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getReplaceTwoArgMissCount() {
        return sampledCacheDelegate.getReplaceTwoArgMissCount();
    }

    @Override
    public long getReplaceTwoArgMissRate() {
        return sampledCacheDelegate.getReplaceTwoArgMissRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPutIfAbsentSuccessCount() {
        return sampledCacheDelegate.getPutIfAbsentSuccessCount();
    }

    @Override
    public long getPutIfAbsentSuccessRate() {
        return sampledCacheDelegate.getPutIfAbsentSuccessRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPutIfAbsentMissCount() {
        return sampledCacheDelegate.getPutIfAbsentMissCount();
    }

    @Override
    public long getPutIfAbsentMissRate() {
        return sampledCacheDelegate.getPutIfAbsentMissRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRemoveElementSuccessCount() {
        return sampledCacheDelegate.getRemoveElementSuccessCount();
    }

    @Override
    public long getRemoveElementSuccessRate() {
        return sampledCacheDelegate.getRemoveElementSuccessRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRemoveElementMissCount() {
        return sampledCacheDelegate.getRemoveElementMissCount();
    }

    @Override
    public long getRemoveElementMissRate() {
        return sampledCacheDelegate.getRemoveElementMissRate();
    }

    /**
     * getCacheAttributes
     *
     * @return map of attribute name {@code ->} value
     */
    public Map<String, Object> getCacheAttributes() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("Enabled", this.isEnabled());
        result.put("TerracottaClustered", this.isTerracottaClustered());
        result.put("LoggingEnabled", this.isLoggingEnabled());
        result.put("TimeToIdleSeconds", this.getTimeToIdleSeconds());
        result.put("TimeToLiveSeconds", this.getTimeToLiveSeconds());
        result.put("MaxEntriesLocalHeap", this.getMaxEntriesLocalHeap());
        result.put("MaxEntriesLocalDisk", this.getMaxEntriesLocalDisk());
        result.put("MaxBytesLocalHeapAsString", this.getMaxBytesLocalHeapAsString());
        result.put("MaxBytesLocalOffHeapAsString", this.getMaxBytesLocalOffHeapAsString());
        result.put("MaxBytesLocalDiskAsString", this.getMaxBytesLocalDiskAsString());
        result.put("MaxBytesLocalHeap", this.getMaxBytesLocalHeap());
        result.put("MaxBytesLocalOffHeap", this.getMaxBytesLocalOffHeap());
        result.put("MaxBytesLocalDisk", this.getMaxBytesLocalDisk());
        result.put("MaxEntriesInCache", this.getMaxEntriesInCache());
        result.put("DiskPersistent", this.isDiskPersistent());
        result.put("PersistenceStrategy", this.getPersistenceStrategy());
        result.put("Eternal", this.isEternal());
        result.put("OverflowToDisk", this.isOverflowToDisk());
        result.put("OverflowToOffHeap", this.isOverflowToOffHeap());
        result.put("DiskExpiryThreadIntervalSeconds", this.getDiskExpiryThreadIntervalSeconds());
        result.put("MemoryStoreEvictionPolicy", this.getMemoryStoreEvictionPolicy());
        result.put("TerracottaConsistency", this.getTerracottaConsistency());
        if (this.isTerracottaClustered()) {
            result.put("NodeBulkLoadEnabled", this.isNodeBulkLoadEnabled());
            result.put("NodeCoherent", this.isNodeCoherent());
            result.put("ClusterBulkLoadEnabled", this.isClusterBulkLoadEnabled());
            result.put("ClusterCoherent", this.isClusterCoherent());
        }
        result.put("WriterConcurrency", this.getWriterConcurrency());
        result.put("Transactional", this.getTransactional());
        result.put("PinnedToStore", this.getPinnedToStore());
        return result;
    }

    /**
     * @see BaseEmitterBean#getNotificationInfo()
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return NOTIFICATION_INFO;
    }

    /**
     * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        try {
            this.sendNotification(CACHE_CHANGED, this.getCacheAttributes(), this.getImmutableCacheName());
        } catch (RuntimeException e) {
            LOG.warn("Failed to send JMX notification for {} {} -> {} due to {}",
                    new Object[] {evt.getPropertyName(), evt.getOldValue(), evt.getNewValue(), e.getMessage()});
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDispose() {
        sampledCacheDelegate.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public long getSearchesPerSecond() {
        return sampledCacheDelegate.getSearchesPerSecond();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getTransactional() {
        return sampledCacheDelegate.getTransactional();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getSearchable() {
        return sampledCacheDelegate.getSearchable();
    }

    @Override
    public Map<String, String> getSearchAttributes() {
        return sampledCacheDelegate.getSearchAttributes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheSearchRate() {
        return sampledCacheDelegate.getCacheSearchRate();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheAverageSearchTimeNanos() {
        return sampledCacheDelegate.getAverageSearchTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTransactionCommitRate() {
        return this.getCacheXaCommitsMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaCommitsMostRecentSample() {
        return sampledCacheDelegate.getCacheXaCommitsMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTransactionRollbackRate() {
        return this.getCacheXaRollbacksMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaRollbacksMostRecentSample() {
        return sampledCacheDelegate.getCacheXaRollbacksMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocalHeapCountBased() {
        return sampledCacheDelegate.isLocalHeapCountBased();
    }

    /**
     * {@inheritDoc}
     */
    public int getCacheHitRatio() {
        return sampledCacheDelegate.getCacheHitRatio();
    }

    /**
     * {@inheritDoc}
     */
    public int getCacheHitRatioMostRecentSample() {
        return sampledCacheDelegate.getCacheHitRatioMostRecentSample();
    }

    @Override
    public long getAverageSearchTimeNanos() {
        return sampledCacheDelegate.getAverageSearchTimeNanos();
    }

    @Override
    public SampledCounter getCacheHitSample() {
        return sampledCacheDelegate.getCacheHitSample();
    }

    @Override
    public SampledCounter getCacheHitRatioSample() {
        return sampledCacheDelegate.getCacheHitRatioSample();
    }

    @Override
    public SampledCounter getCacheHitInMemorySample() {
        return sampledCacheDelegate.getCacheHitInMemorySample();
    }

    @Override
    public SampledCounter getCacheHitOffHeapSample() {
        return sampledCacheDelegate.getCacheHitOffHeapSample();
    }

    @Override
    public SampledCounter getCacheHitOnDiskSample() {
        return sampledCacheDelegate.getCacheHitOnDiskSample();
    }

    @Override
    public SampledCounter getCacheMissSample() {
        return sampledCacheDelegate.getCacheMissSample();
    }

    @Override
    public SampledCounter getCacheMissInMemorySample() {
        return sampledCacheDelegate.getCacheMissInMemorySample();
    }

    @Override
    public SampledCounter getCacheMissOffHeapSample() {
        return sampledCacheDelegate.getCacheMissOffHeapSample();
    }

    @Override
    public SampledCounter getCacheMissOnDiskSample() {
        return sampledCacheDelegate.getCacheMissOnDiskSample();
    }

    @Override
    public SampledCounter getCacheMissExpiredSample() {
        return sampledCacheDelegate.getCacheMissExpiredSample();
    }

    @Override
    public SampledCounter getCacheMissNotFoundSample() {
        return sampledCacheDelegate.getCacheMissNotFoundSample();
    }

    @Override
    public SampledCounter getCacheElementEvictedSample() {
        return sampledCacheDelegate.getCacheElementEvictedSample();
    }

    @Override
    public SampledCounter getCacheElementRemovedSample() {
        return sampledCacheDelegate.getCacheElementRemovedSample();
    }

    @Override
    public SampledCounter getCacheElementExpiredSample() {
        return sampledCacheDelegate.getCacheElementExpiredSample();
    }

    @Override
    public SampledCounter getCacheElementPutSample() {
        return sampledCacheDelegate.getCacheElementPutSample();
    }

    @Override
    public SampledCounter getCacheElementUpdatedSample() {
        return sampledCacheDelegate.getCacheElementUpdatedSample();
    }

    @Override
    public SampledRateCounter getAverageGetTimeSample() {
        return sampledCacheDelegate.getAverageGetTimeSample();
    }

    @Override
    public SampledRateCounter getAverageSearchTimeSample() {
        return sampledCacheDelegate.getAverageSearchTimeSample();
    }

    @Override
    public SampledCounter getSearchesPerSecondSample() {
        return sampledCacheDelegate.getSearchesPerSecondSample();
    }

    @Override
    public SampledCounter getCacheXaCommitsSample() {
        return sampledCacheDelegate.getCacheXaCommitsSample();
    }

    @Override
    public SampledCounter getCacheXaRollbacksSample() {
        return sampledCacheDelegate.getCacheXaRollbacksSample();
    }

    @Override
    public long getAverageSearchTime() {
        return sampledCacheDelegate.getAverageSearchTime();
    }

    @Override
    public long getAverageGetTime() {
        return sampledCacheDelegate.getAverageGetTime();
    }

    @Override
    public SampledCounter getSizeSample() {
        return sampledCacheDelegate.getSizeSample();
    }

    @Override
    public SampledCounter getLocalHeapSizeSample() {
        return sampledCacheDelegate.getLocalHeapSizeSample();
    }

    @Override
    public SampledCounter getLocalHeapSizeInBytesSample() {
        return sampledCacheDelegate.getLocalHeapSizeInBytesSample();
    }

    @Override
    public SampledCounter getLocalOffHeapSizeSample() {
        return sampledCacheDelegate.getLocalOffHeapSizeSample();
    }

    @Override
    public SampledCounter getLocalOffHeapSizeInBytesSample() {
        return sampledCacheDelegate.getLocalOffHeapSizeInBytesSample();
    }

    @Override
    public SampledCounter getLocalDiskSizeSample() {
        return sampledCacheDelegate.getLocalDiskSizeSample();
    }

    @Override
    public SampledCounter getLocalDiskSizeInBytesSample() {
        return sampledCacheDelegate.getLocalDiskSizeInBytesSample();
    }

    @Override
    public SampledCounter getRemoteSizeSample() {
        return sampledCacheDelegate.getRemoteSizeSample();
    }

    @Override
    public SampledCounter getWriterQueueLengthSample() {
        return sampledCacheDelegate.getWriterQueueLengthSample();
    }

    @Override
    public SampledCounter getCacheClusterOfflineSample() {
        return sampledCacheDelegate.getCacheClusterOfflineSample();
    }

    @Override
    public SampledCounter getCacheClusterOnlineSample() {
        return sampledCacheDelegate.getCacheClusterOnlineSample();
    }

    @Override
    public SampledCounter getCacheClusterRejoinSample() {
        return sampledCacheDelegate.getCacheClusterRejoinSample();
    }

    @Override
    public long getCacheClusterOfflineCount() {
        return sampledCacheDelegate.getCacheClusterOfflineCount();
    }

    @Override
    public long getCacheClusterRejoinCount() {
        return sampledCacheDelegate.getCacheClusterRejoinCount();
    }

    @Override
    public long getCacheClusterOnlineCount() {
        return sampledCacheDelegate.getCacheClusterOnlineCount();
    }

    @Override
    public long getCacheClusterOfflineMostRecentSample() {
        return sampledCacheDelegate.getCacheClusterOfflineMostRecentSample();
    }

    @Override
    public long getCacheClusterRejoinMostRecentSample() {
        return sampledCacheDelegate.getCacheClusterRejoinMostRecentSample();
    }

    @Override
    public long getCacheClusterOnlineMostRecentSample() {
        return sampledCacheDelegate.getCacheClusterOnlineMostRecentSample();
    }

    @Override
    public long getMostRecentRejoinTimeStampMillis() {
        return sampledCacheDelegate.getMostRecentRejoinTimeStampMillis();
    }

    @Override
    public SampledCounter getMostRecentRejoinTimestampMillisSample() {
        return sampledCacheDelegate.getMostRecentRejoinTimestampMillisSample();
    }

    @Override
    public SampledCounter getNonStopSuccessSample() {
        return sampledCacheDelegate.getNonStopSuccessSample();
    }

    @Override
    public SampledCounter getNonStopFailureSample() {
        return sampledCacheDelegate.getNonStopFailureSample();
    }

    @Override
    public SampledCounter getNonStopRejoinTimeoutSample() {
        return sampledCacheDelegate.getNonStopRejoinTimeoutSample();
    }

    @Override
    public SampledCounter getNonStopTimeoutSample() {
        return sampledCacheDelegate.getNonStopTimeoutSample();
    }

    @Override
    public long getNonStopSuccessCount() {
        return sampledCacheDelegate.getNonStopSuccessCount();
    }

    @Override
    public long getNonStopFailureCount() {
        return sampledCacheDelegate.getNonStopFailureCount();
    }

    @Override
    public long getNonStopRejoinTimeoutCount() {
        return sampledCacheDelegate.getNonStopRejoinTimeoutCount();
    }

    @Override
    public long getNonStopTimeoutCount() {
        return sampledCacheDelegate.getNonStopTimeoutCount();
    }

    @Override
    public long getNonStopSuccessMostRecentSample() {
        return sampledCacheDelegate.getNonStopSuccessMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNonStopFailureMostRecentSample() {
        return sampledCacheDelegate.getNonStopFailureMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNonStopRejoinTimeoutMostRecentSample() {
        return sampledCacheDelegate.getNonStopRejoinTimeoutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNonStopTimeoutMostRecentSample() {
        return sampledCacheDelegate.getNonStopTimeoutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNonStopSuccessRate() {
        return this.getNonStopSuccessMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNonStopFailureRate() {
        return this.getNonStopFailureMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNonStopRejoinTimeoutRate() {
        return this.getNonStopRejoinTimeoutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNonStopTimeoutRate() {
        return this.getNonStopTimeoutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getReplaceOneArgSuccessSample() {
        return sampledCacheDelegate.getReplaceOneArgSuccessSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getReplaceOneArgMissSample() {
        return sampledCacheDelegate.getReplaceOneArgMissSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getReplaceTwoArgSuccessSample() {
        return sampledCacheDelegate.getReplaceTwoArgSuccessSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getReplaceTwoArgMissSample() {
        return sampledCacheDelegate.getReplaceTwoArgMissSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getPutIfAbsentSuccessSample() {
        return sampledCacheDelegate.getPutIfAbsentSuccessSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getPutIfAbsentMissSample() {
        return sampledCacheDelegate.getPutIfAbsentMissSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getRemoveElementSuccessSample() {
        return sampledCacheDelegate.getRemoveElementSuccessSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getRemoveElementMissSample() {
        return sampledCacheDelegate.getRemoveElementMissSample();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNonstopTimeoutRatio() {
        return sampledCacheDelegate.getNonstopTimeoutRatio();
    }
}