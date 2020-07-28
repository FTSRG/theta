/*
 * Copyright 2019 Budapest University of Technology and Economics
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
package hu.bme.mit.theta.xcfa.alt.expl;

import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.xcfa.XCFA;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

final class ProcessStates {

    @Nonnull
    private final Map<XCFA.Process, ProcessState> states;

    private ProcessStates(@Nonnull Map<XCFA.Process, ProcessState> states) {
        this.states = states;
    }

    public Map<XCFA.Process, ProcessState> getStates() {
        return states;
    }

    public ProcessState getStateOf(XCFA.Process process) {
        return getStates().get(process);
    }

    public static ProcessStates copyOf(ProcessStates old, ExplState.Factory0 factory) {
        return new ProcessStates(
                old.getStates().entrySet().stream()
                        .collect(MapCollector.collector(
                                Map.Entry::getKey,
                                p->ProcessState.copyOf(p.getValue(), factory)
                        ))
        );
    }

    static ProcessStates buildInitial(XCFA xcfa, ExplState.Factory0 factory) {
        return new ProcessStates(
                xcfa.getProcesses().stream()
                        .collect(MapCollector.collector(
                                p->p,
                                p->ProcessState.initial(p, factory)
                        ))
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessStates that = (ProcessStates) o;
        return getStates().equals(that.getStates());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStates());
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder().addAll(states.values().stream().map(x->x.getActiveCallState().getLocation())).toString();
    }
}
