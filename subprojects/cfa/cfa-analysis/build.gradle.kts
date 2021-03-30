plugins {
    id("java-common")
}

dependencies {
    compile(project(":theta-analysis"))
    compile(project(":theta-cfa"))
    compile(project(":theta-common"))
    compile(project(":theta-core"))
    testImplementation(project(":theta-solver-z3"))
    testImplementation(project(":theta-solver-smtlib"))
}
