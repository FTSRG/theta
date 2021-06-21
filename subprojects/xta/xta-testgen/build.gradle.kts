plugins {
    id("java-common")
}

dependencies {
    compile(project(":theta-xta"))
    compile(project(":theta-xta-analysis"))
    compile(project(":theta-solver-z3"))
}
