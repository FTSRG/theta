rootProject.name = "theta"

include(
        "common/analysis",
        "common/common",
        "common/core",
        "common/solver",
        "common/solver-z3",

        "cfa/cfa",
        "cfa/cfa-analysis",
        "cfa/cfa-cli",

        "sts/sts",
        "sts/sts-analysis",
        "sts/sts-cli",

        "xcfa/xcfa",
        "xcfa/xcfa-cli",

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