plugins {
    id("java-common")
}

dependencies {
    compile(project(":theta-common"))
    compile(project(":theta-core"))
    compile(project(":theta-solver"))
    implementation(Deps.z3)
}
