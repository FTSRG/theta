## Overview

This project contains the eXtended Control Flow Automata (XCFA) formalism. Its main purpose is to describe programs as
groups of graphs, where edges are annotated with program statements and each graph represents a single procedure in a
single process. The project contains:

* Classes to represent XCFAs.
* A domain specific language (DSL) to parse XCFAs from a textual representation.

Every _XCFA_ model consists of global variables and _XcfaProcess_ definitions. _XcfaProcesses_ consist of thread-local variables and _XcfaProcedure_ definitions. _XcfaProcedures_ are akin to the _CFA_ models, in the sense that they consist of local variables, _XcfaLocations_ and _XcfaEdges_; and _XcfaEdges_ contain zero or more statements.

Semantically, the _XCFA_ formalism describes an _asynchronous_ system, where processes are constantly executing statements on enabled transitions nondeterministically, until no such process remains (which either means a deadlock situation, or a completed execution). Statements are always atomic, but groups of statements can also be specified to be atomic when enclosed among _AtomicBeginStmt_ and _AtomicEndStmt_ statements. After any number of executed _AtomicBeginStmts_ a single _AtomicEndStmt_ ends the atomic block, and an _AtomicEndStmt_ is no-op without a preceding _AtomicBeginStmt_.

### Related projects

* [`cfa`](../cfa/README.md): The ancestor project of the XCFA formalism, it can represent single-process
  single-procedure programs.
* [`xcfa-cli`](../xcfa-cli/README.md): An executable tool (command line) for running analyses on XCFAs. Currently only
  CFA-like XCFAs are supported.

## XCFA formalism

An XCFA is a process- and procedure-based collection of directed graphs (`V`, `L`, `E`) with

* variables `V = {v1, v2, ..., vn}`,
* locations `L`, with dedicated initial (`l0`), final (`lf`) and error (`le`) locations,
* edges `E` between locations, labeled with statements over the variables. Statements can be
    * assignments of the form `v := expr`, where `expr` is an expression with the same type as `v`,
    * assumptions of the form `assume expr`, where `expr` is a Boolean expression,
    * havocs of the form `havoc v`,
    * boundaries of atomic blocks `AtomicBegin`, `AtomicEnd`,
    * synchronization primitives `Wait`, `Notify`, `NotifyAll`,
    * mutex primitives `lock` and `unlock` (recursive),
    * memory operation primitives `Load`, `Store` with optional annotation of `atomic @ordering` where `ordering` is a
      memory ordering primitive,
    * call statements of the form `call proc` where `proc` is a referenced procedure (by name).

### Textual representation (DSL)

An example XCFA realizing a two-threaded counter:

```
var x : int
main process counter1 {
  main procedure procedure1() {
    var i : int
    init loc L0
    loc L1
    loc L2
    loc L3
    loc L4
    final loc END
    error loc ERR

    L0 -> L1 { i <- x atomic @relaxed }
    L1 -> L2 { assume i < 5 }
    L1 -> L4 { assume not (i < 5) }
    L2 -> L3 { i := i + 1 }
    L3 -> L1 { i -> x atomic @relaxed }
    L4 -> END { assume i <= 5 }
    L4 -> ERR { assume not (i <= 5) }
  }
}
main process counter2 {
  main procedure procedure1() {
    var i : int
    init loc L0
    loc L1
    loc L2
    loc L3
    loc L4
    final loc END
    error loc ERR

    L0 -> L1 { i <- x atomic @relaxed }
    L1 -> L2 { assume i < 5 }
    L1 -> L4 { assume not (i < 5) }
    L2 -> L3 { i := i + 1 }
    L3 -> L1 { i -> x atomic @relaxed }
    L4 -> END { assume i <= 5 }
    L4 -> ERR { assume not (i <= 5) }
  }
}
```

See _src/test/resources_ for more examples and _src/main/antlr_ for the full grammar.
