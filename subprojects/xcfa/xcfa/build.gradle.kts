plugins {
    id("java-common")
    id("antlr-grammar")
}

dependencies {
    compile(project(":theta-common"))
    compile(project(":theta-core"))
    compile(project(":theta-cfa"))
}
