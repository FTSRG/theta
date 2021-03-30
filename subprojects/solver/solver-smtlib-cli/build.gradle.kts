plugins {
    id("java-common")
    id("cli-tool")
}

dependencies {
    compile(project(":theta-solver-smtlib"))
}

application {
    mainClassName = "hu.bme.mit.theta.solver.smtlib.cli.SmtLibCli"
}
