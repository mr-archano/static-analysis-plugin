package com.archano.gradle.staticanalysis

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.resources.TextResource

class StaticAnalysisPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        List<String> excludeList = ['**//**Test.java']

        project.configurations {
            staticAnalysis
        }
        project.dependencies {
            staticAnalysis PluginConstants.RULES
        }

        TextResource checkstyleRules = project.resources.text.fromArchiveEntry(project.configurations.staticAnalysis, 'checkstyle/modules.xml')
        TextResource pmdRules = project.resources.text.fromArchiveEntry(project.configurations.staticAnalysis, 'pmd/rules.xml')

        Configurator.create(project).with {
            configureCheckstyle(checkstyleRules, excludeList)
            configurePmd(pmdRules, excludeList)
            configureLint()
            configureJaCoCo()
        }
    }

}
