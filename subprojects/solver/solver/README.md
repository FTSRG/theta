This project provides common interfaces over different SMT solvers.
SMT solvers can be used to query satisfiability of expressions and to generate interpolants and unsat cores.
There is a separate project for each concrete solver that wraps it to match the common interface.
In order to be able to exchange solvers easily, the common interfaces should be preferred (except when instantiating the concrete solvers).
See [`package-info.java`](src/main/java/hu/bme/mit/theta/solver/package-info.java) in the root package for more information.