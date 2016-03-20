package com.archano.gradle.staticanalysis

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.resources.TextResource
import org.gradle.testing.jacoco.tasks.JacocoReport

class StaticAnalysisPlugin implements Plugin<Project> {

    enum Severity {
        NONE, ERRORS, WARNINGS

        static Severity from(Project project) {
            project.hasProperty('severity') ? valueOf(project.severity.toUpperCase()) : NONE
        }
    }

    @Override
    void apply(Project project) {
        Severity severity = Severity.from(project)
        List<String> excludeList = ['**//**Test.java']

        boolean isAndroidApp = project.plugins.hasPlugin('com.android.application')
        boolean isAndroidLib = project.plugins.hasPlugin('com.android.library')
        boolean isJavaProject = project.plugins.hasPlugin('java')

        project.configurations{
            staticAnalysis
        }
        project.dependencies {
            staticAnalysis PluginConstants.RULES
        }

        TextResource checkstyleRules = project.resources.text.fromArchiveEntry(project.configurations.staticAnalysis, 'checkstyle/modules.xml')

        if (isJavaProject) {
            applyJavaCheckstyle(project, checkstyleRules, severity, excludeList)
            applyJavaJacoco(project)
        } else if (isAndroidApp || isAndroidLib) {
            def variants = isAndroidApp ? project.android.variants : project.android.libraryVariants
            applyAndroidCheckstyle(project, variants, checkstyleRules, severity, excludeList)
            applyAndroidJacoco(project, variants)
        }
    }

    private void applyJavaCheckstyle(Project project, TextResource rules, Severity severity, List<String> excludeList) {
        project.with {
            apply plugin: 'checkstyle'
            checkstyle {
                config = rules
                ignoreFailures = severity == Severity.NONE
            }
            tasks.withType(Checkstyle).all { it.exclude excludeList }
        }
        handleSeverityInReport(severity, project)
    }

    private void handleSeverityInReport(Severity severity, Project project) {
        if (severity == Severity.WARNINGS) {
            project.tasks.withType(Checkstyle).each { task ->
                task << {
                    File xmlReportFile = task.reports.xml.destination
                    File htmlReportFile = new File(xmlReportFile.absolutePath - '.xml' + '.html')
                    if (xmlReportFile.exists() && xmlReportFile.text.contains("<error ")) {
                        throw new GradleException("Checkstyle rule violations were found. See the report at: ${htmlReportFile ?: xmlReportFile}")
                    }
                }
            }
        }
    }

    private void applyJavaJacoco(Project project) {
        project.apply plugin: 'jacoco'
        project.jacoco {
            toolVersion = '0.7.2.201409121644'
        }
    }

    private void applyAndroidCheckstyle(Project project, variants, TextResource rules, Severity severity, List<String> excludeList) {
        project.with {
            apply plugin: 'checkstyle'
            def check = project.tasks['check']
            android.sourceSets.all { sourceSet ->
                def sourceDirs = sourceSet.java.srcDirs
                def notEmptyDirs = sourceDirs.findAll { it.list()?.length > 0 }
                if (notEmptyDirs.empty) {
                    return
                }
                Checkstyle checkstyle = project.tasks.create("checkstyle${sourceSet.name.capitalize()}", Checkstyle)
                checkstyle.with {
                    group = 'verification'
                    description = "Run Checkstyle analysis for ${sourceSet.name} classes"
                    source = sourceSet.java.srcDirs
                    classpath = project.files("$project.buildDir/intermediates/classes/")
                    config = rules
                    ignoreFailures = severity == Severity.NONE
                    exclude excludeList
                }
                variants.all { variant ->
                    checkstyle.mustRunAfter variant.javaCompile
                }
                check.dependsOn checkstyle
            }
        }
        handleSeverityInReport(severity, project)
    }

    private void applyAndroidJacoco(Project project, variants) {
        applyJavaJacoco(project)
        variants.all { variant ->
            def unitTestTask = project.tasks.getByName("test${variant.name.capitalize()}UnitTest")
            if (!unitTestTask) {
                return
            }
            def jacocoName = "jacoco${variant.name.capitalize()}UnitTest"
            def jacocoTask = project.tasks.create(jacocoName, JacocoReport)
            jacocoTask.with {
                group = "verification"
                description = "Generate Jacoco reports for ${variant.name}"
                classDirectories = project.fileTree(variant.javaCompile.destinationDir)
                sourceDirectories = variant.javaCompile.source
                executionData new File("${project.buildDir}/jacoco/${unitTestTask.name}.exec")
                reports {
                    xml.enabled = true
                    html.enabled = true
                }
                dependsOn unitTestTask
            }
        }
    }

}
