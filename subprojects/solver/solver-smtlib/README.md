## Overview

This project wraps a generic solver that supports the SMT-LIB2 interface into our common interfaces for solvers (located in the [solver](..) project).

### Related projects

* [`solver`](../solver/README.md): Contains the generic utilities and solver interface of Theta.
* [`solver-smtlib-cli`](../solver-smtlib-cli/README.md): Command line tool for managing the SMT-LIB compatible solvers.

## SMT-LIB2

The [SMT-LIB2 standard](http://smtlib.cs.uiowa.edu/) is an international initiative, whose goal is to partly provide a common input and output language for SMT solvers. The standard is adapted by many SMT solvers, such as Z3, MathSAT, CVC4, etc...

However, there are some limitation to this standard, both in terms of its content and its practical application by the solvers that support it:

- There are no standard way of denoting an array literal in SMT-LIB. Each solver that support the said theory used to come up with a solution of their own in this matter. This lead to the creation of the `as-array` (Z3), and the `as const` (CVC4, MathSAT) constructs. Nowadays, solvers tend to support the `as const` construct (event the newer versions of Z3).
- There are no standard way of denoting interpolation. Solvers that support interpolation (Z3, SMTInterpol, MathSAT) have their own extension added to SMT-LIB to support this feature.
- The support for the standard tends to be loose in some solvers. For example, MathSAT expects a numeric argument to `push` and `pop`, does not default to 1, like the standard. Moreover, MathSAT uses a custom output language when outputting models, that is not part of the standard. 

This list is unfortunately far from being complete, so one has to be careful when integrating a new solver with SMT-LIB support.

## Architecture

The issues of compatibility described in the earlier section warranted to be considered in the architecture. The component is designed to be a composite of subcomponents along well-defined interfaced that make it possible to replace complete subcomponents if for the sake of a solver.

### Generic architecture interface

In general, to have an SMT-LIB supporting solver, components implementing the following interfaces has to be developed:

- `SmtLibTransformationManager`: Transforms anything in Theta to their SMT-LIB counterpart. It uses further components for this feature:
    - `SmtLibTypeTransformer`: Transforms a Theta type to and SMT-LIB type.
    - `SmtLibDeclTransformer`: Transforms a Theta declaration to an SMT-LIB declaration.
    - `SmtLibExprTransformer`: Transforms a Theta expression to an SMT-LIB expression.
    - `SmtLibSymbolTable`: Caches the Theta declarations and their SMT-LIB counterpart.
- `SmtLibTermTransformer`: Transforms an SMT-LIB expression to a Theta expression. It provides methods that ensure type-safety.
- `SmtLibSolverBinary`: Provides an interface to communicate with the binary of the solver.
- `SmtLibSolverInstaller`: An interface to support installation scripts and solver management.

The SMT-LIB grammar that the component supports is defined using ANTLR. The ANTLR grammar can be found in the source folder (`SMTLIBv2.g4`).

The components above are integrated into a working solver implementation with Theta's solver interface by the followin classes:
- `SmtLibSolver`: Provides a solver implementing Theta's `Solver` and `UCSolver` interface supporting basic satisfiability checks, model querying and unsat core querying.
- `BaseSmtLibItpSolver`: Provides a solver implementinf Theta`s `ItpSolver` interface supporting interpolating solvers. **Note**: As interpolation is not part of the SMT-LIB standard, this class is abstract. Each solver supporting interpolation has to extend this class and configure it properly.

### Generic interface implementation

The interfaces above have a default, generic implementation that works with solvers that follow the SMT-LIB standard fully. These generic classes can be found in the `hu.bme.mit.theta.solver.smtlib.impl.generic` package.

### Solver specific implementations

Right now, the solver-smtlib subproject supports the following solvers. Each package contains the specialization of the interfaces above that communicate with the said solver:

- **Z3** (`hu.bme.mit.theta.solver.smtlib.impl.z3`): The solver supports basic satisfiability problems for numerous theories (integers, rationals, quantifiers, arrays, bitvectors, functions), and supports interpolation as well (binary, sequential and tree interpolation) up to version 4.7.
- **CVC4** (`hu.bme.mit.theta.solver.smtlib.impl.cvc4`): The solver supports basic satisfiability problems for numerous theories (integers, rationals, quantifiers, arrays, bitvectors).