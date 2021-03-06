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

package net.sf.ehcache.transaction.xa;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.statistics.StatisticBuilder;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.AbstractTransactionStore;
import net.sf.ehcache.transaction.error.TransactionException;
import net.sf.ehcache.transaction.error.TransactionInterruptedException;
import net.sf.ehcache.transaction.error.TransactionTimeoutException;
import net.sf.ehcache.transaction.id.TransactionIDFactory;
import net.sf.ehcache.transaction.lock.SoftLock;
import net.sf.ehcache.transaction.lock.SoftLockID;
import net.sf.ehcache.transaction.lock.SoftLockManager;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.commands.StorePutCommand;
import net.sf.ehcache.transaction.xa.commands.StoreRemoveCommand;
import net.sf.ehcache.transaction.xa.statistics.XaCommitOutcome;
import net.sf.ehcache.transaction.xa.statistics.XaRecoveryOutcome;
import net.sf.ehcache.transaction.xa.statistics.XaRollbackOutcome;
import net.sf.ehcache.util.LargeSet;
import net.sf.ehcache.util.SetAsList;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.observer.OperationObserver;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;

/**
 * @author Ludovic Orban
 */
public class XATransactionStore extends AbstractTransactionStore {

    private static final Logger LOG = LoggerFactory.getLogger(XATransactionStore.class.getName());

    private final TransactionManagerLookup transactionManagerLookup;
    private final TransactionIDFactory transactionIdFactory;
    private final ElementValueComparator comparator;
    private final SoftLockManager softLockManager;
    private final Ehcache cache;
    private final EhcacheXAResourceImpl recoveryResource;

    /** 记录 Transaction 与对应 XAResource 之间的映射关系，每个事务绑定一个 XAResource */
    private final ConcurrentHashMap<Transaction, EhcacheXAResource> transactionToXAResourceMap = new ConcurrentHashMap<Transaction, EhcacheXAResource>();
    private final ConcurrentHashMap<Transaction, Long> transactionToTimeoutMap = new ConcurrentHashMap<Transaction, Long>();

    private final OperationObserver<XaCommitOutcome> commitObserver = StatisticBuilder.operation(XaCommitOutcome.class)
            .of(this).named("xa-commit").tag("xa-transactional").build();
    private final OperationObserver<XaRollbackOutcome> rollbackObserver = StatisticBuilder.operation(XaRollbackOutcome.class)
            .of(this).named("xa-rollback").tag("xa-transactional").build();
    private final OperationObserver<XaRecoveryOutcome> recoveryObserver = StatisticBuilder.operation(XaRecoveryOutcome.class)
            .of(this).named("xa-recovery").tag("xa-transactional").build();

    /**
     * Constructor
     *
     * @param transactionManagerLookup the transaction manager lookup implementation
     * @param softLockManager the soft lock manager
     * @param transactionIdFactory the transaction ID factory
     * @param cache the cache
     * @param store the underlying store
     */
    public XATransactionStore(TransactionManagerLookup transactionManagerLookup,
                              SoftLockManager softLockManager,
                              TransactionIDFactory transactionIdFactory,
                              Ehcache cache,
                              Store store,
                              ElementValueComparator comparator) {
        super(store);
        this.transactionManagerLookup = transactionManagerLookup;
        this.transactionIdFactory = transactionIdFactory;
        this.comparator = comparator;
        if (transactionManagerLookup.getTransactionManager() == null) {
            throw new TransactionException("no JTA transaction manager could be located, cannot bind twopc cache with JTA");
        }
        this.softLockManager = softLockManager;
        this.cache = cache;

        // this xaresource is for initial registration and recovery
        this.recoveryResource = new EhcacheXAResourceImpl(
                cache,
                underlyingStore,
                transactionManagerLookup,
                softLockManager,
                transactionIdFactory,
                comparator,
                commitObserver,
                rollbackObserver,
                recoveryObserver);
        transactionManagerLookup.register(recoveryResource, true);
    }

    @Override
    public void dispose() {
        super.dispose();
        transactionManagerLookup.unregister(recoveryResource, true);
    }

    private Transaction getCurrentTransaction() throws SystemException {
        // 如果使用的是 bitronix，则这里是 BitronixTransaction
        Transaction transaction = transactionManagerLookup.getTransactionManager().getTransaction();
        if (transaction == null) {
            throw new TransactionException("JTA transaction not started");
        }
        return transaction;
    }

    /**
     * Get or create the XAResource of this XA store
     *
     * @return the EhcacheXAResource of this store
     * @throws SystemException when something goes wrong with the transaction manager
     */
    public EhcacheXAResourceImpl getOrCreateXAResource() throws SystemException {
        // 获取对应的 Transaction 实现，例如 BitronixTransaction
        Transaction transaction = getCurrentTransaction();
        EhcacheXAResourceImpl xaResource = (EhcacheXAResourceImpl) transactionToXAResourceMap.get(transaction);
        if (xaResource == null) {
            LOG.debug("creating new XAResource");
            // 新建一个 XAResource 对象
            xaResource = new EhcacheXAResourceImpl(
                    cache,
                    underlyingStore,
                    transactionManagerLookup,
                    softLockManager,
                    transactionIdFactory,
                    comparator,
                    commitObserver,
                    rollbackObserver,
                    recoveryObserver);
            transactionToXAResourceMap.put(transaction, xaResource);
            // 注册一个 CleanupXAResource，用于在提交或回滚事务时清空当前 Transaction
            xaResource.addTwoPcExecutionListener(new CleanupXAResource(getCurrentTransaction()));
        }
        return xaResource;
    }

    private XATransactionContext getTransactionContext() {
        try {
            Transaction transaction = getCurrentTransaction();
            EhcacheXAResourceImpl xaResource = (EhcacheXAResourceImpl) transactionToXAResourceMap.get(transaction);
            if (xaResource == null) {
                return null;
            }
            XATransactionContext transactionContext = xaResource.getCurrentTransactionContext();

            if (transactionContext == null) {
                transactionManagerLookup.register(xaResource, false);
                LOG.debug("creating new XA context");
                transactionContext = xaResource.createTransactionContext();
                xaResource.addTwoPcExecutionListener(new UnregisterXAResource());
            } else {
                transactionContext = xaResource.getCurrentTransactionContext();
            }

            LOG.debug("using XA context {}", transactionContext);
            return transactionContext;
        } catch (SystemException e) {
            throw new TransactionException("cannot get the current transaction", e);
        } catch (RollbackException e) {
            throw new TransactionException("transaction rolled back", e);
        }
    }

    private XATransactionContext getOrCreateTransactionContext() {
        try {
            // 获取当前 Transaction 对应的 XAResource 对象
            EhcacheXAResourceImpl xaResource = this.getOrCreateXAResource();
            // 获取对应的 XATransactionContext
            XATransactionContext transactionContext = xaResource.getCurrentTransactionContext();

            if (transactionContext == null) {
                // 注册当前 XAResource 到 TM
                transactionManagerLookup.register(xaResource, false);
                LOG.debug("creating new XA context");
                transactionContext = xaResource.createTransactionContext();
                xaResource.addTwoPcExecutionListener(new UnregisterXAResource());
            } else {
                transactionContext = xaResource.getCurrentTransactionContext();
            }

            LOG.debug("using XA context {}", transactionContext);
            return transactionContext;
        } catch (SystemException e) {
            throw new TransactionException("cannot get the current transaction", e);
        } catch (RollbackException e) {
            throw new TransactionException("transaction rolled back", e);
        }
    }

    /**
     * This class is used to clean up the transactionToTimeoutMap after a transaction
     * committed or rolled back.
     */
    private final class CleanupTimeout implements Synchronization {
        private final Transaction transaction;

        private CleanupTimeout(final Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(final int status) {
            transactionToTimeoutMap.remove(transaction);
        }
    }

    /**
     * This class is used to clean up the transactionToXAResourceMap after a transaction
     * committed or rolled back.
     */
    private final class CleanupXAResource implements XAExecutionListener {
        private final Transaction transaction;

        private CleanupXAResource(Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public void beforePrepare(EhcacheXAResource xaResource) {
        }

        @Override
        public void afterCommitOrRollback(EhcacheXAResource xaResource) {
            transactionToXAResourceMap.remove(transaction);
        }
    }

    /**
     * This class is used to unregister the XAResource after a transaction
     * committed or rolled back.
     */
    private final class UnregisterXAResource implements XAExecutionListener {

        @Override
        public void beforePrepare(EhcacheXAResource xaResource) {
        }

        @Override
        public void afterCommitOrRollback(EhcacheXAResource xaResource) {
            transactionManagerLookup.unregister(xaResource, false);
        }
    }

    /**
     * @return milliseconds left before timeout
     */
    private long assertNotTimedOut() {
        try {
            if (Thread.interrupted()) {
                throw new TransactionInterruptedException("transaction interrupted");
            }

            Transaction transaction = getCurrentTransaction();
            Long timeoutTimestamp = transactionToTimeoutMap.get(transaction);
            long now = MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
            if (timeoutTimestamp == null) {
                long timeout;
                EhcacheXAResource xaResource = transactionToXAResourceMap.get(transaction);
                if (xaResource != null) {
                    int xaResourceTimeout = xaResource.getTransactionTimeout();
                    timeout = MILLISECONDS.convert(xaResourceTimeout, TimeUnit.SECONDS);
                } else {
                    int defaultTransactionTimeout = cache.getCacheManager().getTransactionController().getDefaultTransactionTimeout();
                    timeout = MILLISECONDS.convert(defaultTransactionTimeout, TimeUnit.SECONDS);
                }
                timeoutTimestamp = now + timeout;
                transactionToTimeoutMap.put(transaction, timeoutTimestamp);
                try {
                    transaction.registerSynchronization(new CleanupTimeout(transaction));
                } catch (RollbackException e) {
                    throw new TransactionException("transaction has been marked as rollback only", e);
                }
                return timeout;
            } else {
                long timeToExpiry = timeoutTimestamp - now;
                if (timeToExpiry <= 0) {
                    throw new TransactionTimeoutException("transaction timed out");
                } else {
                    return timeToExpiry;
                }
            }
        } catch (SystemException e) {
            throw new TransactionException("cannot get the current transaction", e);
        } catch (XAException e) {
            throw new TransactionException("cannot get the XAResource transaction timeout", e);
        }
    }

    /* transactional methods */

    /**
     * {@inheritDoc}
     */
    @Override
    public Element get(Object key) {
        LOG.debug("cache {} get {}", cache.getName(), key);
        XATransactionContext context = getTransactionContext();
        Element element;
        if (context == null) {
            element = getFromUnderlyingStore(key);
        } else {
            element = context.get(key);
            if (element == null && !context.isRemoved(key)) {
                element = getFromUnderlyingStore(key);
            }
        }
        return element;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getQuiet(Object key) {
        LOG.debug("cache {} getQuiet {}", cache.getName(), key);
        XATransactionContext context = getTransactionContext();
        Element element;
        if (context == null) {
            element = getQuietFromUnderlyingStore(key);
        } else {
            element = context.get(key);
            if (element == null && !context.isRemoved(key)) {
                element = getQuietFromUnderlyingStore(key);
            }
        }
        return element;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        LOG.debug("cache {} getSize", cache.getName());
        XATransactionContext context = getOrCreateTransactionContext();
        int size = underlyingStore.getSize();
        return Math.max(0, size + context.getSizeModifier());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTerracottaClusteredSize() {
        try {
            Transaction transaction = transactionManagerLookup.getTransactionManager().getTransaction();
            if (transaction == null) {
                return underlyingStore.getTerracottaClusteredSize();
            }
        } catch (SystemException se) {
            throw new TransactionException("cannot get the current transaction", se);
        }

        LOG.debug("cache {} getTerracottaClusteredSize", cache.getName());
        XATransactionContext context = getOrCreateTransactionContext();
        int size = underlyingStore.getTerracottaClusteredSize();
        return size + context.getSizeModifier();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        LOG.debug("cache {} containsKey", cache.getName(), key);
        XATransactionContext context = getOrCreateTransactionContext();
        return !context.isRemoved(key) && (context.getAddedKeys().contains(key) || underlyingStore.containsKey(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getKeys() {
        LOG.debug("cache {} getKeys", cache.getName());
        XATransactionContext context = getOrCreateTransactionContext();
        Set<Object> keys = new LargeSet<Object>() {

            @Override
            public int sourceSize() {
                return underlyingStore.getSize();
            }

            @Override
            public Iterator<Object> sourceIterator() {
                return underlyingStore.getKeys().iterator();
            }
        };
        keys.addAll(context.getAddedKeys());
        keys.removeAll(context.getRemovedKeys());
        return new SetAsList<Object>(keys);
    }

    private Element getFromUnderlyingStore(final Object key) {
        while (true) {
            long timeLeft = assertNotTimedOut();
            LOG.debug("cache {} underlying.get key {} not timed out, time left: " + timeLeft, cache.getName(), key);

            Element element = underlyingStore.get(key);
            if (element == null) {
                return null;
            }
            Object value = element.getObjectValue();
            if (value instanceof SoftLockID) {
                SoftLockID softLockId = (SoftLockID) value;
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                if (softLock == null) {
                    LOG.debug("cache {} underlying.get key {} soft lock died, retrying...", cache.getName(), key);
                    continue;
                } else {
                    try {
                        LOG.debug("cache {} key {} soft locked, awaiting unlock...", cache.getName(), key);
                        if (softLock.tryLock(timeLeft)) {
                            softLock.clearTryLock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                return element;
            }
        }
    }

    private Element getQuietFromUnderlyingStore(final Object key) {
        while (true) {
            long timeLeft = assertNotTimedOut();
            LOG.debug("cache {} underlying.getQuiet key {} not timed out, time left: " + timeLeft, cache.getName(), key);

            Element element = underlyingStore.getQuiet(key);
            if (element == null) {
                return null;
            }
            Object value = element.getObjectValue();
            if (value instanceof SoftLockID) {
                SoftLockID softLockId = (SoftLockID) value;
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                if (softLock == null) {
                    LOG.debug("cache {} underlying.getQuiet key {} soft lock died, retrying...", cache.getName(), key);
                    continue;
                } else {
                    try {
                        LOG.debug("cache {} key {} soft locked, awaiting unlock...", cache.getName(), key);
                        if (softLock.tryLock(timeLeft)) {
                            softLock.clearTryLock();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                return element;
            }
        }
    }

    private Element getCurrentElement(final Object key, final XATransactionContext context) {
        Element previous = context.get(key);
        if (previous == null && !context.isRemoved(key)) {
            previous = getQuietFromUnderlyingStore(key);
        }
        return previous;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean put(Element element) throws CacheException {
        LOG.debug("cache {} put {}", cache.getName(), element);
        // this forces enlistment so the XA transaction timeout can be propagated to the XA resource
        // 获取当前事务对应的上下文对象，期间会尝试注册 XAResource 到 TM
        final XATransactionContext transactionContext = getOrCreateTransactionContext();

        // 获取 key 对应的旧值
        Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
        return internalPut(new StorePutCommand(oldElement, element));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        LOG.debug("cache {} putWithWriter {}", cache.getName(), element);
        // this forces enlistment so the XA transaction timeout can be propagated to the XA resource
        getOrCreateTransactionContext();

        Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
        if (writerManager != null) {
            writerManager.put(element);
        } else {
            cache.getWriterManager().put(element);
        }
        return internalPut(new StorePutCommand(oldElement, element));
    }

    private boolean internalPut(final StorePutCommand putCommand) {
        // 获取待写入的元素值
        final Element element = putCommand.getElement();
        if (element == null) {
            return true;
        }
        // 获取当前事务对应的上下文
        XATransactionContext context = getOrCreateTransactionContext();
        // In case this key is currently being updated...
        boolean isNull = underlyingStore.get(element.getKey()) == null; // 判断当前 key 是否不存在
        if (isNull) {
            isNull = context.get(element.getKey()) == null;
        }
        // 添加 command 到事务上下文中
        context.addCommand(putCommand, element);
        return isNull;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element remove(Object key) {
        LOG.debug("cache {} remove {}", cache.getName(), key);
        // this forces enlistment so the XA transaction timeout can be propagated to the XA resource
        getOrCreateTransactionContext();

        Element oldElement = getQuietFromUnderlyingStore(key);
        return removeInternal(new StoreRemoveCommand(key, oldElement));
    }

    private Element removeInternal(final StoreRemoveCommand command) {
        Element element = command.getEntry().getElement();
        getOrCreateTransactionContext().addCommand(command, element);
        return element;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        LOG.debug("cache {} removeWithWriter {}", cache.getName(), key);
        // this forces enlistment so the XA transaction timeout can be propagated to the XA resource
        getOrCreateTransactionContext();

        Element oldElement = getQuietFromUnderlyingStore(key);
        if (writerManager != null) {
            writerManager.remove(new CacheEntry(key, null));
        } else {
            cache.getWriterManager().remove(new CacheEntry(key, null));
        }
        return removeInternal(new StoreRemoveCommand(key, oldElement));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() throws CacheException {
        LOG.debug("cache {} removeAll", cache.getName());
        List keys = getKeys();
        for (Object key : keys) {
            remove(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element putIfAbsent(Element element) throws NullPointerException {
        LOG.debug("cache {} putIfAbsent {}", cache.getName(), element);
        XATransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getObjectKey(), context);

        if (previous == null) {
            Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
            context.addCommand(new StorePutCommand(oldElement, element), element);
        }

        return previous;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        LOG.debug("cache {} removeElement {}", cache.getName(), element);
        XATransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        if (previous != null && comparator.equals(previous, element)) {
            Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
            context.addCommand(new StoreRemoveCommand(element.getObjectKey(), oldElement), element);
            return previous;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(Element old, Element element, ElementValueComparator comparator)
            throws NullPointerException, IllegalArgumentException {
        LOG.debug("cache {} replace2 {}", cache.getName(), element);
        XATransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        boolean replaced = false;
        if (previous != null && comparator.equals(previous, old)) {
            Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
            context.addCommand(new StorePutCommand(oldElement, element), element);
            replaced = true;
        }
        return replaced;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element replace(Element element) throws NullPointerException {
        LOG.debug("cache {} replace1 {}", cache.getName(), element);
        XATransactionContext context = getOrCreateTransactionContext();
        Element previous = getCurrentElement(element.getKey(), context);

        if (previous != null) {
            Element oldElement = getQuietFromUnderlyingStore(element.getObjectKey());
            context.addCommand(new StorePutCommand(oldElement, element), element);
        }
        return previous;
    }

}
