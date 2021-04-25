plugins {
    id("java-common")
    id("cli-tool")
}

dependencies {
    compile(project(":theta-xcfa"))
    compile(project(":theta-solver-z3"))
    compile(project(":theta-cfa-analysis"))
    compile(project(":theta-cfa"))
}

application {
    mainClassName = "hu.bme.mit.theta.xcfa.cli.stateless.XcfaCli"
}
