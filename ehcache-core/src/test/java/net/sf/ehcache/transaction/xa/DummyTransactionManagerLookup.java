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

import net.sf.ehcache.transaction.manager.TransactionManagerLookup;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class DummyTransactionManagerLookup implements TransactionManagerLookup {

    private static DummyTransactionManager transactionManager = new DummyTransactionManager();

    @Override
    public void init() {

    }

    @Override
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public synchronized void register(EhcacheXAResource resource, boolean forRecovery) {

    }

    @Override
    public synchronized void unregister(EhcacheXAResource resource, boolean forRecovery) {
    }

    @Override
    public void setProperties(Properties properties) {
    }

    static int arrayHashCode(byte[] uid) {
        int hash = 0;
        for (int i = uid.length - 1; i > 0; i--) {
            hash <<= 1;

            if (hash < 0) {
                hash |= 1;
            }

            hash ^= uid[i];
        }
        return hash;
    }

    public static byte[] longToBytes(long aLong) {
        byte[] array = new byte[8];

        array[7] = (byte) (aLong & 0xff);
        array[6] = (byte) ((aLong >> 8) & 0xff);
        array[5] = (byte) ((aLong >> 16) & 0xff);
        array[4] = (byte) ((aLong >> 24) & 0xff);
        array[3] = (byte) ((aLong >> 32) & 0xff);
        array[2] = (byte) ((aLong >> 40) & 0xff);
        array[1] = (byte) ((aLong >> 48) & 0xff);
        array[0] = (byte) ((aLong >> 56) & 0xff);

        return array;
    }

    public static class DummyTransactionManager implements TransactionManager {

        private final AtomicLong txIdGenerator = new AtomicLong();

        private DummyTransaction testTransaction;

        public DummyTransactionManager() {
        }

        @Override
        public void begin() throws NotSupportedException, SystemException {
            testTransaction = new DummyTransaction(txIdGenerator.incrementAndGet());
        }

        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
        }

        @Override
        public int getStatus() throws SystemException {
            return 0;
        }

        @Override
        public Transaction getTransaction() throws SystemException {
            return testTransaction;
        }

        @Override
        public void resume(Transaction transaction) throws IllegalStateException, InvalidTransactionException, SystemException {
            testTransaction = (DummyTransaction) transaction;
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
        }

        @Override
        public void setTransactionTimeout(int i) throws SystemException {
        }

        @Override
        public Transaction suspend() throws SystemException {
            DummyTransaction suspendedTx = testTransaction;
            testTransaction = null;
            return suspendedTx;
        }
    }

    public static class DummyTransaction implements Transaction {

        private long id;

        public DummyTransaction(long id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DummyTransaction) {
                DummyTransaction otherTx = (DummyTransaction) o;
                return otherTx.id == id;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (int) id;
        }

        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SecurityException, SystemException {
        }

        @Override
        public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException, SystemException {
            return true;
        }

        @Override
        public boolean enlistResource(XAResource xaResource) throws IllegalStateException, RollbackException, SystemException {
            return true;
        }

        @Override
        public int getStatus() throws SystemException {
            return 0;
        }

        @Override
        public void registerSynchronization(Synchronization synchronization) throws IllegalStateException, RollbackException, SystemException {
        }

        @Override
        public void rollback() throws IllegalStateException, SystemException {
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
        }
    }

    public static class DummyXid implements Xid {

        private int formatId = 123456;
        private byte[] gtrid;
        private byte[] bqual;

        public DummyXid(long gtrid, long bqual) {
            this.gtrid = longToBytes(gtrid);
            this.bqual = longToBytes(bqual);
        }

        public DummyXid(Xid xid) {
            this.formatId = xid.getFormatId();
            this.gtrid = xid.getGlobalTransactionId();
            this.bqual = xid.getBranchQualifier();
        }

        @Override
        public int getFormatId() {
            return formatId;
        }

        @Override
        public byte[] getGlobalTransactionId() {
            return gtrid;
        }

        @Override
        public byte[] getBranchQualifier() {
            return bqual;
        }

        @Override
        public int hashCode() {
            return formatId + arrayHashCode(gtrid) + arrayHashCode(bqual);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DummyXid) {
                DummyXid otherXid = (DummyXid) o;
                return formatId == otherXid.formatId &&
                        Arrays.equals(gtrid, otherXid.gtrid) &&
                        Arrays.equals(bqual, otherXid.bqual);
            }
            return false;
        }

        @Override
        public String toString() {
            return "DummyXid [" + hashCode() + "]";
        }
    }
}
