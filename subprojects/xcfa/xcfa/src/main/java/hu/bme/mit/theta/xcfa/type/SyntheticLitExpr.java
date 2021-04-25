/*
 * Copyright 2021 Budapest University of Technology and Economics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.bme.mit.theta.xcfa.type;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.NullaryExpr;
import hu.bme.mit.theta.core.type.xcfa.SyntheticType;
import hu.bme.mit.theta.xcfa.model.XcfaProcess;

import java.util.Objects;
import java.util.Optional;

/**
 * Uses a recursive mutex.
 * Return values are always Optional<SyntheticLitExpr>.
 * Empty is returned when the transition cannot be done, but it isn't an error.
 * For example locking the mutex when locked on another process returns empty.
 * invalid() is returned when the transition is due to bad usage of locks.
 * It may means an error of the system under verification.
 * Unlocking an unlocked mutex is such an example.
 * Precondition checks are only used when the order of operations cannot happen
 * under any circumstances, it means an error in the verifier.
 * Currently the only error could be when there is something between an enterWait and exitWait.
 */
public final class SyntheticLitExpr extends NullaryExpr<SyntheticType> implements LitExpr<SyntheticType> {

	private final XcfaProcess lockedOn;
	private final ImmutableSet<XcfaProcess> blockedProcesses;
	/**
	 * num == -1 -> invalid state, error reached
	 * num == 0 -> valid state, unlocked
	 * num > 0 -> how many times has it been locked in this process (reentrant/recursive mutex)
	 */
	private final int num;

	private SyntheticLitExpr(XcfaProcess lockedOn, int num, ImmutableSet<XcfaProcess> blockedProcesses) {
		if (num > 0)
			this.lockedOn = lockedOn;
		else
			this.lockedOn = null;
		this.num = num;
		this.blockedProcesses = blockedProcesses;
		checkState();
	}

	/**
	 * Means an invalid usage of locks
	 */
	private static Optional<SyntheticLitExpr> invalid() {
		return Optional.of(LazyHolder.BOTTOM);
	}

	public static SyntheticLitExpr unlocked() {
		return LazyHolder.INSTANCE;
	}

	private void checkState() {
		if (num == -1)
			Preconditions.checkState(lockedOn == null);
		if (num == 0)
			Preconditions.checkState(lockedOn == null);
		if (num > 0)
			Preconditions.checkState(lockedOn != null);
		if (lockedOn != null)
			Preconditions.checkState(!blockedProcesses.contains(lockedOn));
	}

	/**
	 * Lock is in invalid state, indicates error reached
	 */
	public boolean isInvalid() {
		return num < 0;
	}

	public boolean isLocked() {
		return lockedOn != null;
	}

	public XcfaProcess getOwnerProcess() {
		return lockedOn;
	}

	public boolean isValid() {
		return !isInvalid();
	}

	/**
	 * Releases lock, adds process to waitSet
	 */
	public Optional<SyntheticLitExpr> enterWait(XcfaProcess waitOn) {
		if (!isLocked()) {
			// wait should be called when locked on the mutex.
			return invalid();
		}
		if (getOwnerProcess() != waitOn) {
			// wait() without lock()
			return invalid();
		}

		Preconditions.checkState(num == 1, "Wait/notify with non-reentrant" +
				" usage of locks is not supported .");
		return Optional.of(new SyntheticLitExpr(null, 0,
				ImmutableSet.<XcfaProcess>builder()
						.addAll(blockedProcesses)
						.add(waitOn).build()
		));
	}

	/**
	 * Regrabs lock.
	 * Optional.empty() means the transition is disabled.
	 * Bottom is on bad lock usage.
	 */
	public Optional<SyntheticLitExpr> exitWait(XcfaProcess waitOn) {
		if (blockedProcesses.contains(waitOn)) {
			return Optional.empty();
		}
		if (isLocked()) {
			Preconditions.checkState(getOwnerProcess() != waitOn,
					"Found exitWait when locked on the owner process. This should never happen." +
							"Bad XCFA could be the cause.");
			return Optional.empty();
		}
		return Optional.of(new SyntheticLitExpr(waitOn, num + 1, blockedProcesses));
	}

	public Optional<SyntheticLitExpr> signalAll(XcfaProcess calledFrom) {
		return Optional.of(new SyntheticLitExpr(lockedOn, num, ImmutableSet.of()));
	}

	/**
	 * Tries to wake a given process. Only active when the process to wake is in blocked state.
	 * Unit transition of a signal/notify stmt.
	 * processToWake==null is special case: it's active only when there is no active edge.
	 */
	public Optional<SyntheticLitExpr> tryWakeProcess(XcfaProcess calledFrom, XcfaProcess processToWake) {
		if (processToWake == null) {
			if (blockedProcesses.isEmpty())
				return Optional.of(this);
			return Optional.empty();
		}
		if (blockedProcesses.contains(processToWake)) {
			var builder = ImmutableSet.<XcfaProcess>builder();
			blockedProcesses.forEach(p -> {
				if (p != processToWake)
					builder.add(p);
			});
			return Optional.of(new SyntheticLitExpr(lockedOn, num, builder.build()));
		}
		return Optional.empty();
	}

	public Optional<SyntheticLitExpr> lock(XcfaProcess lockOn) {
		Preconditions.checkState(!blockedProcesses.contains(lockOn),
				"Error! Probably a lock stmt between enterWait and exitWait on the same process.");
		if (lockedOn == null) {
			return Optional.of(new SyntheticLitExpr(lockOn, 1, blockedProcesses));
		} else if (lockOn == lockedOn) {
			return Optional.of(new SyntheticLitExpr(lockedOn, num + 1, blockedProcesses));
		}
		return Optional.empty();
	}

	public Optional<SyntheticLitExpr> unlock(XcfaProcess unlockOn) {
		Preconditions.checkState(!blockedProcesses.contains(unlockOn),
				"Error! Probably an unlock stmt between enterWait and exitWait on the same process.");
		if (lockedOn == null) {
			// unlocking when already unlocked -> bad locking
			return invalid();
		} else if (unlockOn == lockedOn) {
			return Optional.of(new SyntheticLitExpr(lockedOn, num - 1, blockedProcesses));
		}
		// unlocking when locked... But somehow on an other process?
		return invalid();
	}

	@Override
	public SyntheticType getType() {
		return SyntheticType.getInstance();
	}

	@Override
	public SyntheticLitExpr eval(Valuation val) {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SyntheticLitExpr that = (SyntheticLitExpr) o;
		return num == that.num &&
				Objects.equals(lockedOn, that.lockedOn)
				&& Objects.equals(blockedProcesses, that.blockedProcesses);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lockedOn, num);
	}

	@Override
	public String toString() {
		return Utils.lispStringBuilder("SyntheticLitExpr").add(
				Utils.lispStringBuilder("LockedOn").add(lockedOn != null ? lockedOn : "None")
		).add(
				Utils.lispStringBuilder("Times").add(num)
		).toString();
	}

	private static class LazyHolder {
		private static final SyntheticLitExpr BOTTOM = new SyntheticLitExpr(null, -1, ImmutableSet.of());
		private static final SyntheticLitExpr INSTANCE = new SyntheticLitExpr(null, 0, ImmutableSet.of());
	}
}
