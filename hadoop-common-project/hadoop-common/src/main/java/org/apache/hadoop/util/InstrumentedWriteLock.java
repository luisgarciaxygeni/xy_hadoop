/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;

import org.apache.hadoop.classification.VisibleForTesting;
import org.slf4j.Logger;

/**
 * This is a wrap class of a <code>WriteLock</code>.
 * It extends the class {@link InstrumentedLock}, and can be used to track
 * whether a specific write lock is being held for too long and log
 * warnings if so.
 *
 * The logged warnings are throttled so that logs are not spammed.
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
public class InstrumentedWriteLock extends InstrumentedLock {

  private final ReentrantReadWriteLock readWriteLock;
  private volatile long writeLockHeldTimeStamp = 0;

  public InstrumentedWriteLock(String name, Logger logger,
      ReentrantReadWriteLock readWriteLock,
      long minLoggingGapMs, long lockWarningThresholdMs) {
    this(name, logger, readWriteLock, minLoggingGapMs, lockWarningThresholdMs,
        new Timer());
  }

  @VisibleForTesting
  InstrumentedWriteLock(String name, Logger logger,
      ReentrantReadWriteLock readWriteLock,
      long minLoggingGapMs, long lockWarningThresholdMs, Timer clock) {
    super(name, logger, readWriteLock.writeLock(), minLoggingGapMs,
        lockWarningThresholdMs, clock);
    this.readWriteLock = readWriteLock;
  }

  @Override
  public void unlock() {
    boolean needReport = readWriteLock.getWriteHoldCount() == 1;
    long localWriteReleaseTime = getTimer().monotonicNow();
    long localWriteAcquireTime = writeLockHeldTimeStamp;
    getLock().unlock();
    if (needReport) {
      writeLockHeldTimeStamp = 0;
      check(localWriteAcquireTime, localWriteReleaseTime, true);
    }
  }

  /**
   * Starts timing for the instrumented write lock.
   */
  @Override
  protected void startLockTiming() {
    if (readWriteLock.getWriteHoldCount() == 1) {
      writeLockHeldTimeStamp = getTimer().monotonicNow();
    }
  }
}
