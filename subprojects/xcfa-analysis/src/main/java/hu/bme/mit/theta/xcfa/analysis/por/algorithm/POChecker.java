/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.xcfa.analysis.por.algorithm;

import hu.bme.mit.theta.xcfa.XCFA;
import hu.bme.mit.theta.xcfa.analysis.por.expl.ExecutableTransitionBase;
import hu.bme.mit.theta.xcfa.analysis.por.expl.ExecutableTransitionUtils;
import hu.bme.mit.theta.xcfa.analysis.por.expl.ImmutableExplState;
import hu.bme.mit.theta.xcfa.analysis.por.expl.ProcessTransitions;
import hu.bme.mit.theta.xcfa.analysis.por.expl.Transition;
import hu.bme.mit.theta.xcfa.analysis.por.expl.TransitionUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class POChecker extends XcfaChecker {

    private static class AmpleSetFactory {
        private final Collection<ProcessTransitions> allTransitionsByProcess;

        public AmpleSetFactory(XCFA xcfa) {
            allTransitionsByProcess = TransitionUtils.getProcessTransitions(xcfa);
        }

        private void collectDependingProcesses(ProcessTransitions t, Collection<XCFA.Process> alreadyProcessed,
                                               Set<XCFA.Process> result) {
            t.transitionStream().forEach(tr->collectDependingProcesses(tr, alreadyProcessed, result));
        }

        private final Map<Transition, Collection<XCFA.Process>> dependingProcessCache = new HashMap<>();

        private void collectDependingProcesses(Transition t, Collection<XCFA.Process> alreadyProcessed,
                                               Set<XCFA.Process> result) {
            Collection<XCFA.Process> collection;
            if (!dependingProcessCache.containsKey(t)) {
                var res = new HashSet<XCFA.Process>();
                for (var x : allTransitionsByProcess) {
                    //if (alreadyProcessed.contains(x.getProcess())) {
                    //    continue;
                    //}
                    //if (result.contains(x.getProcess())) {
                    //    continue;
                    //}
                    if (x.getProcess() == t.getProcess()) {
                        continue;
                    }
                    if (DependencyUtils.depends(x, t)) {
                        res.add(x.getProcess());
                    }
                }
                dependingProcessCache.put(t, res);
                collection = res;
            } else {
                collection = dependingProcessCache.get(t);
            }

            collection.stream().filter(x->!alreadyProcessed.contains(x))
                    .forEach(result::add);
        }

        // input is a non empty set of enabled transitions
        private Collection<XCFA.Process> collectAmpleSet(ImmutableExplState state) {
            HashSet<XCFA.Process> ampleSet = new HashSet<>();
            HashSet<XCFA.Process> notYetProcessed = new HashSet<>();

            Collection<ProcessTransitions> enabledProcesses = TransitionUtils.getProcessTransitions(state).stream()
                    .filter(ProcessTransitions::hasAnyEnabledTransition)
                    .collect(Collectors.toUnmodifiableList());

            // add a transition
            ProcessTransitions first = enabledProcesses.iterator().next();
            notYetProcessed.add(first.getProcess());

            // add dependencies transitively
            while (!notYetProcessed.isEmpty()) {
                XCFA.Process nextProcess = notYetProcessed.iterator().next();
                ampleSet.add(nextProcess);

                notYetProcessed.remove(nextProcess);
                var nextProcessTransition = enabledProcesses.stream().filter(x->x.getProcess()==nextProcess).findAny();
                nextProcessTransition.ifPresent(processTransitions -> collectDependingProcesses(processTransitions, ampleSet, notYetProcessed));

                // Add disabled transitions' dependencies, which may activate the transition.
                // More precisely: all processes' transitions that may enable this disabled transition.
                // Look at partialorder-test.xcfa for more information
                nextProcessTransition.ifPresent(t->t.disabledStream()
                            .forEach(
                                // TODO collect enabling transitions, not depending ones
                                tr -> collectDependingProcesses(tr, ampleSet, notYetProcessed)
                            )
                );
            }
            return ampleSet;
        }

        // Does not contain the logic of C3 about cycles, they are handled in the DFS
        public Collection<XCFA.Process> returnAmpleSet(ImmutableExplState expl) {
            Collection<ExecutableTransitionBase> enabled = ExecutableTransitionUtils.getExecutableTransitions(expl)
                    .collect(Collectors.toUnmodifiableList());
            if (enabled.isEmpty()) { // C0
                return Collections.emptySet();
            }
            return collectAmpleSet(expl);
        }
    }

    AmpleSetFactory factory;

    POChecker(XCFA xcfa, Config config) {
        super(xcfa,config);
        factory = new AmpleSetFactory(xcfa);
    }

    @Override
    protected void onNodePushed(DfsNodeBase node) {
        //
    }

    @Override
    protected DfsNodeBase initialNode(ImmutableExplState state) {
        return new DfsNode(state, null);
    }

    private final class DfsNode extends DfsNodeBase {

        boolean optimized = true;

        private Stream<ProcessTransitions> collectTransitions(ImmutableExplState state, Collection<XCFA.Process> processes) {
            return TransitionUtils.getProcessTransitions(state).stream()
                    .filter(x->processes.contains(x.getProcess()));
        }

        DfsNode(ImmutableExplState state, @Nullable Transition lastTransition) {
            super(state, lastTransition);
            collectTransitions(state,
                    factory.returnAmpleSet(state)
            ).forEach(this::push);
        }

        @Override
        public DfsNodeBase nodeFrom(ImmutableExplState state, ExecutableTransitionBase lastTransition) {
            return new DfsNode(state, lastTransition);
        }
    }

}