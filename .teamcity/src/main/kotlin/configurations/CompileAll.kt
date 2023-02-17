package configurations

import model.CIBuildModel
import model.Stage

class CompileAll(model: CIBuildModel, stage: Stage, nextGeneration: Boolean) : BaseGradleBuildType(stage = stage, init = {
    id(buildTypeId(model, nextGeneration))
    name = "Compile All ${if (nextGeneration) "NG" else ""}"
    description = "Compiles all production/test source code and warms up the build cache"

    features {
        publishBuildStatusToGithub(model)
    }

    applyDefaults(
        model,
        this,
        if (nextGeneration) "YourNextGenerationCommandLine" else "compileAllBuild -PignoreIncomingBuildReceipt=true -DdisableLocalCache=true",
        extraParameters = buildScanTag("CompileAll") + " " + "-Porg.gradle.java.installations.auto-download=false"
    )

    artifactRules = """$artifactRules
        subprojects/base-services/build/generated-resources/build-receipt/org/gradle/build-receipt.properties
    """.trimIndent()
}) {
    companion object {
        fun buildTypeId(model: CIBuildModel, nextGeneration: Boolean = false) = buildTypeId(model.projectId, nextGeneration)
        fun buildTypeId(projectId: String, nextGeneration: Boolean = false) = "${projectId}_CompileAllBuild_${if (nextGeneration) "NG" else ""}"
    }
}
