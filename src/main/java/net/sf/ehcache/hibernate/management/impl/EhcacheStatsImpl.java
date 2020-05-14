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

package net.sf.ehcache.hibernate.management.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.management.api.EhcacheStats;
import net.sf.ehcache.management.sampled.SampledCacheManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

/**
 * Implementation of {@link EhcacheStats}
 *
 * <p>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public class EhcacheStatsImpl extends BaseEmitterBean implements EhcacheStats {
    private static final long MILLIS_PER_SECOND = 1000;
    private static final MBeanNotificationInfo NOTIFICATION_INFO;

    private final SampledCacheManager sampledCacheManager;
    private final CacheManager cacheManager;
    private long statsSince = System.currentTimeMillis();

    static {
        final String[] notifTypes = new String[] {CACHE_ENABLED, CACHE_REGION_CHANGED, CACHE_FLUSHED, CACHE_REGION_FLUSHED,
                CACHE_STATISTICS_ENABLED, CACHE_STATISTICS_RESET,};
        final String name = Notification.class.getName();
        final String description = "Ehcache Hibernate Statistics Event";
        NOTIFICATION_INFO = new MBeanNotificationInfo(notifTypes, name, description);
    }

    /**
     * Constructor accepting the backing {@link CacheManager}
     *
     * @throws NotCompliantMBeanException
     */
    public EhcacheStatsImpl(CacheManager manager) throws NotCompliantMBeanException {
        super(EhcacheStats.class);
        this.sampledCacheManager = new SampledCacheManager(manager);
        this.cacheManager = manager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushRegionCache(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushRegionCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = this.cacheManager.getCache(name);
            if (cache != null) {
                cache.flush();
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateActiveConfigDeclaration() {
        return this.cacheManager.getActiveConfigurationText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateActiveConfigDeclaration(String region) {
        return this.cacheManager.getActiveConfigurationText(region);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitCount() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getStatistics().cacheHitCount();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCacheHitRate() {
        long now = System.currentTimeMillis();
        double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
        return this.getCacheHitCount() / deltaSecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitSample() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getStatistics().cacheHitOperation().rate().value().longValue();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissCount() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getStatistics().cacheMissCount();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCacheMissRate() {
        long now = System.currentTimeMillis();
        double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
        return this.getCacheMissCount() / deltaSecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissSample() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getStatistics().cacheMissOperation().rate().value().longValue();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCachePutCount() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getStatistics().cachePutCount();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCachePutRate() {
        long now = System.currentTimeMillis();
        double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
        return this.getCachePutCount() / deltaSecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCachePutSample() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getStatistics().cachePutOperation().rate().value().longValue();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOriginalConfigDeclaration() {
        return this.cacheManager.getOriginalConfigurationText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOriginalConfigDeclaration(String region) {
        return this.cacheManager.getOriginalConfigurationText(region);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<String, Object>> getRegionCacheAttributes() {
        Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
        for (String regionName : this.cacheManager.getCacheNames()) {
            result.put(regionName, this.getRegionCacheAttributes(regionName));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getRegionCacheAttributes(String regionName) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("Enabled", this.isRegionCacheEnabled(regionName));
        result.put("LoggingEnabled", this.isRegionCacheLoggingEnabled(regionName));
        result.put("MaxTTISeconds", this.getRegionCacheMaxTTISeconds(regionName));
        result.put("MaxTTLSeconds", this.getRegionCacheMaxTTLSeconds(regionName));
        result.put("TargetMaxInMemoryCount", this.getRegionCacheTargetMaxInMemoryCount(regionName));
        result.put("TargetMaxTotalCount", this.getRegionCacheTargetMaxTotalCount(regionName));
        result.put("OrphanEvictionEnabled", this.isRegionCacheOrphanEvictionEnabled(regionName));
        result.put("OrphanEvictionPeriod", this.getRegionCacheOrphanEvictionPeriod(regionName));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRegionCacheMaxTTISeconds(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return (int) cache.getCacheConfiguration().getTimeToIdleSeconds();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRegionCacheMaxTTLSeconds(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return (int) cache.getCacheConfiguration().getTimeToLiveSeconds();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRegionCacheOrphanEvictionPeriod(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null && cache.isTerracottaClustered()) {
            return cache.getCacheConfiguration().getTerracottaConfiguration().getOrphanEvictionPeriod();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, int[]> getRegionCacheSamples() {
        Map<String, int[]> rv = new HashMap<String, int[]>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                rv.put(name, new int[] {cache.getStatistics().cacheHitOperation().rate().value().intValue(),
                        cache.getStatistics().cacheMissNotFoundOperation().rate().value().intValue(),
                        cache.getStatistics().cacheMissExpiredOperation().rate().value().intValue(),
                        cache.getStatistics().cachePutOperation().rate().value().intValue()});
            }
        }
        return rv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRegionCacheTargetMaxInMemoryCount(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return cache.getCacheConfiguration().getMaxElementsInMemory();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRegionCacheTargetMaxTotalCount(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return cache.getCacheConfiguration().getMaxElementsOnDisk();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getTerracottaHibernateCacheRegionNames() {
        ArrayList<String> rv = new ArrayList<String>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                if (cache.getCacheConfiguration().isTerracottaClustered()) {
                    rv.add(name);
                }
            }
        }
        return rv.toArray(new String[] {});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegionCacheEnabled(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            return !cache.isDisabled();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegionCacheEnabled(String region, boolean enabled) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.setDisabled(!enabled);
        }
        this.sendNotification(CACHE_REGION_CHANGED, this.getRegionCacheAttributes(region), region);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegionCachesEnabled() {
        for (String name : this.cacheManager.getCacheNames()) {
            Cache cache = this.cacheManager.getCache(name);
            if (cache != null) {
                if (cache.isDisabled()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @see EhcacheStats#setRegionCachesEnabled(boolean)
     */
    @Override
    public void setRegionCachesEnabled(final boolean flag) {
        for (String name : this.cacheManager.getCacheNames()) {
            Cache cache = this.cacheManager.getCache(name);
            if (cache != null) {
                cache.setDisabled(!flag);
            }
        }
        this.sendNotification(CACHE_ENABLED, flag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegionCacheLoggingEnabled(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            return cache.getCacheConfiguration().getLogging();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegionCacheOrphanEvictionEnabled(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null && cache.isTerracottaClustered()) {
            return cache.getCacheConfiguration().getTerracottaConfiguration().getOrphanEviction();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTerracottaHibernateCache(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return cache.getCacheConfiguration().isTerracottaClustered();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegionCacheLoggingEnabled(String region, boolean loggingEnabled) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setLogging(loggingEnabled);
            this.sendNotification(CACHE_REGION_CHANGED, this.getRegionCacheAttributes(region), region);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegionCacheMaxTTISeconds(String region, int maxTTISeconds) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setTimeToIdleSeconds(maxTTISeconds);
            this.sendNotification(CACHE_REGION_CHANGED, this.getRegionCacheAttributes(region), region);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegionCacheMaxTTLSeconds(String region, int maxTTLSeconds) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setTimeToLiveSeconds(maxTTLSeconds);
            this.sendNotification(CACHE_REGION_CHANGED, this.getRegionCacheAttributes(region), region);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegionCacheTargetMaxInMemoryCount(String region, int targetMaxInMemoryCount) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setMaxElementsInMemory(targetMaxInMemoryCount);
            this.sendNotification(CACHE_REGION_CHANGED, this.getRegionCacheAttributes(region), region);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRegionCacheTargetMaxTotalCount(String region, int targetMaxTotalCount) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setMaxElementsOnDisk(targetMaxTotalCount);
            this.sendNotification(CACHE_REGION_CHANGED, this.getRegionCacheAttributes(region), region);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see EhcacheStats#getNumberOfElementsInMemory(String)
     */
    @Override
    public int getNumberOfElementsInMemory(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            return (int) cache.getStatistics().getLocalHeapSize();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see EhcacheStats#getNumberOfElementsOffHeap(String)
     */
    @Override
    public int getNumberOfElementsOffHeap(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            return (int) cache.getStatistics().getLocalOffHeapSize();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see EhcacheStats#getNumberOfElementsOnDisk(String)
     */
    @Override
    public int getNumberOfElementsOnDisk(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            return (int) cache.getStatistics().getLocalDiskSize();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaxGetTimeMillis() {
        long rv = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                rv = Math.max(rv, TimeUnit.MILLISECONDS.convert(cache.getStatistics().cacheSearchOperation().latency().maximum().value().longValue(),
                        TimeUnit.NANOSECONDS));
            }
        }
        return rv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMinGetTimeMillis() {
        long rv = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                rv = Math.max(rv, TimeUnit.MILLISECONDS.convert(cache.getStatistics().cacheSearchOperation().latency().minimum().value().longValue(),
                        TimeUnit.NANOSECONDS));
                // TODO CRSS why max?
            }
        }
        return rv;
    }

    /**
     * {@inheritDoc}
     *
     * @see EhcacheStats#getMaxGetTimeMillis(String)
     */
    @Override
    public long getMaxGetTimeMillis(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            return TimeUnit.MILLISECONDS.convert(cache.getStatistics().cacheGetOperation().latency().maximum().value().longValue(),
                    TimeUnit.NANOSECONDS);
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see EhcacheStats#getMinGetTimeMillis(String)
     */
    @Override
    public long getMinGetTimeMillis(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            return TimeUnit.MILLISECONDS.convert(cache.getStatistics().cacheGetOperation().latency().minimum().value().longValue(),
                    TimeUnit.NANOSECONDS);
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see EhcacheStats#getAverageGetTimeMillis(String)
     */
    @Override
    public float getAverageGetTimeMillis(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            return TimeUnit.MILLISECONDS.convert(cache.getStatistics().cacheGetOperation().latency().average().value().longValue(),
                    TimeUnit.NANOSECONDS);
        } else {
            return -1f;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDispose() {
        // no-op
    }

    /**
     * @see BaseEmitterBean#getNotificationInfo()
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[] {NOTIFICATION_INFO};
    }
}
