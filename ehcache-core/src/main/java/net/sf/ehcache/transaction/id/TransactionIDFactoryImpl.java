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

package net.sf.ehcache.transaction.id;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.transaction.xa.XidTransactionID;
import net.sf.ehcache.transaction.xa.XidTransactionIDImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.transaction.xa.Xid;

/**
 * A TransactionIDFactory implementation with uniqueness across a single JVM
 *
 * @author Ludovic Orban
 */
public class TransactionIDFactoryImpl extends AbstractTransactionIDFactory {

    private final ConcurrentMap<TransactionID, Decision> transactionStates = new ConcurrentHashMap<TransactionID, Decision>();

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionID createTransactionID() {
        TransactionID id = new TransactionIDImpl();
        getTransactionStates().putIfAbsent(id, Decision.IN_DOUBT);
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransactionID restoreTransactionID(TransactionIDSerializedForm serializedForm) {
        throw new UnsupportedOperationException("unclustered transaction IDs are directly deserializable!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XidTransactionID createXidTransactionID(Xid xid, Ehcache cache) {
        XidTransactionID id = new XidTransactionIDImpl(xid, cache.getName());
        // 标识当前事务状态
        getTransactionStates().putIfAbsent(id, Decision.IN_DOUBT);
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
        throw new UnsupportedOperationException("unclustered transaction IDs are directly deserializable!");
    }

    @Override
    protected ConcurrentMap<TransactionID, Decision> getTransactionStates() {
        return transactionStates;
    }

    @Override
    public Boolean isPersistent() {
        return Boolean.FALSE;
    }

    @Override
    public boolean isExpired(TransactionID transactionID) {
        return false;
    }
}
