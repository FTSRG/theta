rootProject.name = "theta"

include(
        "common/analysis",
        "common/common",
        "common/core",

        "solver/solver",
        "solver/solver-z3",
        "solver/solver-smtlib",
        "solver/solver-smtlib-cli",

        "cfa/cfa",
        "cfa/cfa-analysis",
        "cfa/cfa-cli",

        "sts/sts",
        "sts/sts-analysis",
        "sts/sts-cli",

        "xta/xta",
        "xta/xta-analysis",
        "xta/xta-cli",

        "xsts/xsts",
        "xsts/xsts-analysis",
        "xsts/xsts-cli"
)

for (project in rootProject.children) {
    val projectPath = project.name
    val projectName = projectPath.split("/").last()
    project.projectDir = file("subprojects/$projectPath")
    project.name = "${rootProject.name}-$projectName"
}