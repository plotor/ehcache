/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.id.TransactionID;
import net.sf.ehcache.transaction.local.LocalTransactionContext;
import net.sf.ehcache.transaction.lock.SoftLock;
import net.sf.ehcache.transaction.lock.SoftLockID;
import net.sf.ehcache.transaction.lock.SoftLockManager;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.collections.SerializedToolkitCache;
import org.terracotta.toolkit.collections.ToolkitMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class ReadCommittedClusteredSoftLockFactory implements SoftLockManager {
  private static final Integer DEFAULT_DUMMY_VALUE = Integer
          .valueOf(0);
  private final String cacheName;
  private final String cacheManagerName;

  private final ToolkitInstanceFactory toolkitInstanceFactory;

  // actually all we need would be a ConcurrentSet...
  private final ToolkitMap<SerializedReadCommittedClusteredSoftLock, Integer> newKeyLocks;

  // locks must be inserted in a clustered collection b/c they must be managed by the L1 before they are returned
  private final SerializedToolkitCache<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock> allLocks;

  public ReadCommittedClusteredSoftLockFactory(ToolkitInstanceFactory toolkitInstanceFactory, String cacheManagerName,
                                               String cacheName) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.cacheManagerName = cacheManagerName;
    this.cacheName = cacheName;
    allLocks = toolkitInstanceFactory.getOrCreateAllSoftLockMap(cacheManagerName, cacheName);
    newKeyLocks = toolkitInstanceFactory.getOrCreateNewSoftLocksSet(cacheManagerName, cacheName);
  }

  @Override
  public SoftLockID createSoftLockID(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
    if (newElement != null && newElement.getObjectValue() instanceof SoftLockID) {
      throw new AssertionError(
              "newElement must not contain a soft lock ID");
    }
    if (oldElement != null && oldElement.getObjectValue() instanceof SoftLockID) {
      throw new AssertionError(
              "oldElement must not contain a soft lock ID");
    }

    SoftLockID lockId = new SoftLockID(transactionID, key, newElement, oldElement);
    ClusteredSoftLockIDKey clusteredId = new ClusteredSoftLockIDKey(lockId);

    if (allLocks.containsKey(clusteredId)) {
      return lockId;
    } else {
      SerializedReadCommittedClusteredSoftLock softLock = new SerializedReadCommittedClusteredSoftLock(transactionID,
              key);

      if (allLocks.putIfAbsent(clusteredId, softLock) != null) {
        throw new AssertionError();
      } else {
        if (oldElement == null) {
          newKeyLocks.put(softLock, DEFAULT_DUMMY_VALUE);
        }
        return lockId;
      }
    }
  }

  @Override
  public SoftLock findSoftLockById(SoftLockID softLockId) {
    SerializedReadCommittedClusteredSoftLock serializedSoftLock = allLocks.get(new ClusteredSoftLockIDKey(softLockId));
    if (serializedSoftLock == null) {
      return null;
    }
    return serializedSoftLock.getSoftLock(toolkitInstanceFactory, this);
  }

  ReadCommittedClusteredSoftLock getLock(TransactionID transactionId, Object key) {
    for (Map.Entry<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock> entry : allLocks.entrySet()) {
      SerializedReadCommittedClusteredSoftLock serialized = entry.getValue();
      ReadCommittedClusteredSoftLock readCommittedSoftLock = serialized.getSoftLock(toolkitInstanceFactory, this);
      if (readCommittedSoftLock.getTransactionID().equals(transactionId) && readCommittedSoftLock.getKey().equals(key)) {
        return readCommittedSoftLock;
      }
    }
    return null;
  }

  @Override
  public Set<Object> getKeysInvisibleInContext(LocalTransactionContext currentTransactionContext, Store underlyingStore) {
    Set<Object> invisibleKeys = new HashSet<Object>();

    // all new keys added into the store are invisible
    invisibleKeys.addAll(getNewKeys());

    List<SoftLock> currentTransactionContextSoftLocks = currentTransactionContext.getSoftLocksForCache(cacheName);
    for (SoftLock softLock : currentTransactionContextSoftLocks) {
      SoftLockID softLockId = (SoftLockID) underlyingStore.getQuiet(softLock.getKey()).getObjectValue();

      if (softLock.getElement(currentTransactionContext.getTransactionId(), softLockId) == null) {
        // if the soft lock's element is null in the current transaction then the key is invisible
        invisibleKeys.add(softLock.getKey());
      } else {
        // if the soft lock's element is not null in the current transaction then the key is visible
        invisibleKeys.remove(softLock.getKey());
      }
    }

    return invisibleKeys;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<SoftLock> collectAllSoftLocksForTransactionID(TransactionID transactionID) {
    Set<SoftLock> result = new HashSet<SoftLock>();

    for (Map.Entry<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock> entry : allLocks.entrySet()) {
      SerializedReadCommittedClusteredSoftLock serialized = entry.getValue();
      ReadCommittedClusteredSoftLock softLock = serialized.getSoftLock(toolkitInstanceFactory, this);
      if (softLock.getTransactionID().equals(transactionID)) {
        result.add(softLock);
      }
    }

    return result;
  }

  @Override
  public void clearSoftLock(SoftLock softLock) {
    SoftLockID softLockId = new SoftLockID(((ReadCommittedClusteredSoftLock) softLock).getTransactionID(), softLock.getKey(), null, null);
    ClusteredSoftLockIDKey clusteredIdKey = new ClusteredSoftLockIDKey(softLockId);
    SerializedReadCommittedClusteredSoftLock serializedClusteredSoftLock = allLocks.remove(clusteredIdKey);
    newKeyLocks.remove(serializedClusteredSoftLock);
  }

  private Set<Object> getNewKeys() {
    Set<Object> result = new HashSet<Object>();
    for (SerializedReadCommittedClusteredSoftLock serialized : newKeyLocks.keySet()) {
      result.add(serialized.getSoftLock(toolkitInstanceFactory, this).getKey());
    }

    return result;
  }

  String getCacheName() {
    return cacheName;
  }

  String getCacheManagerName() {
    return cacheManagerName;
  }

}
