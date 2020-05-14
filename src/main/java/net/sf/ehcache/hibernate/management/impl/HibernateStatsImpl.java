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

import net.sf.ehcache.hibernate.management.api.HibernateStats;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

/**
 * Implementation of {@link HibernateStats}
 *
 * <p>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public class HibernateStatsImpl extends BaseEmitterBean implements HibernateStats {
    private static final double MILLIS_PER_SECOND = 1000;
    private static final MBeanNotificationInfo NOTIFICATION_INFO;

    private final SessionFactory sessionFactory;

    static {
        final String[] notifTypes = new String[] {};
        final String name = Notification.class.getName();
        final String description = "Hibernate Statistics Event";
        NOTIFICATION_INFO = new MBeanNotificationInfo(notifTypes, name, description);
    }

    /**
     * Constructor accepting the backing {@link SessionFactory}
     *
     * @param sessionFactory
     * @throws NotCompliantMBeanException
     */
    public HibernateStatsImpl(SessionFactory sessionFactory) throws NotCompliantMBeanException {
        super(HibernateStats.class);
        this.sessionFactory = sessionFactory;
    }

    /**
     * @return statistics
     */
    private Statistics getStatistics() {
        return sessionFactory.getStatistics();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#clearStats()
     */
    @Override
    public void clearStats() {
        this.getStatistics().clear();
        this.sendNotification(CACHE_STATISTICS_RESET);
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#disableStats()
     */
    @Override
    public void disableStats() {
        this.setStatisticsEnabled(false);
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#enableStats()
     */
    @Override
    public void enableStats() {
        this.setStatisticsEnabled(true);
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getCloseStatementCount()
     */
    @Override
    public long getCloseStatementCount() {
        return this.getStatistics().getCloseStatementCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getConnectCount()
     */
    @Override
    public long getConnectCount() {
        return this.getStatistics().getConnectCount();
    }

    /**
     * Not supported right now
     */
    public long getDBSQLExecutionSample() {
        throw new UnsupportedOperationException("Use getQueryExecutionCount() instead");
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getFlushCount()
     */
    @Override
    public long getFlushCount() {
        return this.getStatistics().getFlushCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getOptimisticFailureCount()
     */
    @Override
    public long getOptimisticFailureCount() {
        return this.getStatistics().getOptimisticFailureCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getPrepareStatementCount()
     */
    @Override
    public long getPrepareStatementCount() {
        return this.getStatistics().getPrepareStatementCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getQueryExecutionCount()
     */
    @Override
    public long getQueryExecutionCount() {
        return this.getStatistics().getQueryExecutionCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getQueryExecutionRate()
     */
    @Override
    public double getQueryExecutionRate() {
        long startTime = this.getStatistics().getStartTime();
        long now = System.currentTimeMillis();
        double deltaSecs = (now - startTime) / MILLIS_PER_SECOND;
        return this.getQueryExecutionCount() / deltaSecs;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getQueryExecutionSample()
     */
    @Override
    public long getQueryExecutionSample() {
        throw new UnsupportedOperationException("TODO: need to impl. rates for query execution");
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getSessionCloseCount()
     */
    @Override
    public long getSessionCloseCount() {
        return this.getStatistics().getSessionCloseCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getSessionOpenCount()
     */
    @Override
    public long getSessionOpenCount() {
        return this.getStatistics().getSessionOpenCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getSuccessfulTransactionCount()
     */
    @Override
    public long getSuccessfulTransactionCount() {
        return this.getStatistics().getSuccessfulTransactionCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getTransactionCount()
     */
    @Override
    public long getTransactionCount() {
        return this.getStatistics().getTransactionCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#isStatisticsEnabled()
     */
    @Override
    public boolean isStatisticsEnabled() {
        return this.getStatistics().isStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#setStatisticsEnabled(boolean)
     */
    @Override
    public void setStatisticsEnabled(boolean flag) {
        this.getStatistics().setStatisticsEnabled(flag);
        this.sendNotification(CACHE_STATISTICS_ENABLED, flag);
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getEntityStats()
     */
    @Override
    public TabularData getEntityStats() {
        List<CompositeData> result = new ArrayList<CompositeData>();
        Statistics statistics = this.getStatistics();
        for (String entity : statistics.getEntityNames()) {
            EntityStats entityStats = new EntityStats(entity, statistics.getEntityStatistics(entity));
            result.add(entityStats.toCompositeData());
        }
        TabularData td = EntityStats.newTabularDataInstance();
        td.putAll(result.toArray(new CompositeData[result.size()]));
        return td;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getCollectionStats()
     */
    @Override
    public TabularData getCollectionStats() {
        List<CompositeData> result = new ArrayList<CompositeData>();
        Statistics statistics = this.getStatistics();
        for (String roleName : statistics.getCollectionRoleNames()) {
            CollectionStats collectionStats = new CollectionStats(roleName, statistics.getCollectionStatistics(roleName));
            result.add(collectionStats.toCompositeData());
        }
        TabularData td = CollectionStats.newTabularDataInstance();
        td.putAll(result.toArray(new CompositeData[result.size()]));
        return td;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getQueryStats()
     */
    @Override
    public TabularData getQueryStats() {
        List<CompositeData> result = new ArrayList<CompositeData>();
        Statistics statistics = this.getStatistics();
        for (String query : statistics.getQueries()) {
            QueryStats queryStats = new QueryStats(query, statistics.getQueryStatistics(query));
            result.add(queryStats.toCompositeData());
        }
        TabularData td = QueryStats.newTabularDataInstance();
        td.putAll(result.toArray(new CompositeData[result.size()]));
        return td;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TabularData getCacheRegionStats() {
        List<CompositeData> list = new ArrayList<CompositeData>();
        Statistics statistics = this.getStatistics();
        for (String region : statistics.getSecondLevelCacheRegionNames()) {
            CacheRegionStats l2CacheStats = new CacheRegionStats(region, statistics.getSecondLevelCacheStatistics(region));
            list.add(l2CacheStats.toCompositeData());
        }
        TabularData td = CacheRegionStats.newTabularDataInstance();
        td.putAll(list.toArray(new CompositeData[list.size()]));
        return td;
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
