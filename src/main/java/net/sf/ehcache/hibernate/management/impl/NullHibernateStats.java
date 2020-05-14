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

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.openmbean.TabularData;

/**
 * Implementation of {@link HibernateStats} that does nothing
 *
 * <p>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public final class NullHibernateStats implements HibernateStats {

    /**
     * Singleton instance.
     */
    public static final HibernateStats INSTANCE = new NullHibernateStats();

    /**
     * private constructor. No need to create instances of this. Use singleton instance
     */
    private NullHibernateStats() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#clearStats()
     */
    @Override
    public void clearStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#disableStats()
     */
    @Override
    public void disableStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#enableStats()
     */
    @Override
    public void enableStats() {
        // no-op

    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getCloseStatementCount()
     */
    @Override
    public long getCloseStatementCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getCollectionStats()
     */
    @Override
    public TabularData getCollectionStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getConnectCount()
     */
    @Override
    public long getConnectCount() {
        // no-op
        return 0;
    }

    /**
     * Not supported right now
     */
    public long getDBSQLExecutionSample() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getEntityStats()
     */
    @Override
    public TabularData getEntityStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getFlushCount()
     */
    @Override
    public long getFlushCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getOptimisticFailureCount()
     */
    @Override
    public long getOptimisticFailureCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getPrepareStatementCount()
     */
    @Override
    public long getPrepareStatementCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getQueryExecutionCount()
     */
    @Override
    public long getQueryExecutionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getQueryExecutionRate()
     */
    @Override
    public double getQueryExecutionRate() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getQueryExecutionSample()
     */
    @Override
    public long getQueryExecutionSample() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getQueryStats()
     */
    @Override
    public TabularData getQueryStats() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getSessionCloseCount()
     */
    @Override
    public long getSessionCloseCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getSessionOpenCount()
     */
    @Override
    public long getSessionOpenCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getSuccessfulTransactionCount()
     */
    @Override
    public long getSuccessfulTransactionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#getTransactionCount()
     */
    @Override
    public long getTransactionCount() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#isStatisticsEnabled()
     */
    @Override
    public boolean isStatisticsEnabled() {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see HibernateStats#setStatisticsEnabled(boolean)
     */
    @Override
    public void setStatisticsEnabled(boolean flag) {
        // no-op
    }

    /**
     * @see HibernateStats#getCacheRegionStats()
     */
    @Override
    public TabularData getCacheRegionStats() {
        return null;
    }

    /**
     * @see javax.management.NotificationEmitter#removeNotificationListener(NotificationListener, NotificationFilter, Object)
     */
    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {
        /**/
    }

    /**
     * @see javax.management.NotificationBroadcaster#addNotificationListener(NotificationListener, NotificationFilter, Object)
     */
    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
        /**/
    }

    /**
     * @see javax.management.NotificationBroadcaster#getNotificationInfo()
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return null;
    }

    /**
     * @see javax.management.NotificationBroadcaster#removeNotificationListener(NotificationListener)
     */
    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        /**/
    }
}
