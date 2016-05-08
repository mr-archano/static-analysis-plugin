package com.archano.gradle.staticanalysis

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.resources.TextResource
import org.gradle.logging.ConsoleRenderer
import org.gradle.testing.jacoco.tasks.JacocoReport

class StaticAnalysisPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        Severity severity = Severity.from(project)
        List<String> excludeList = ['**//**Test.java']

        boolean isAndroidApp = project.plugins.hasPlugin('com.android.application')
        boolean isAndroidLib = project.plugins.hasPlugin('com.android.library')
        boolean isJavaProject = project.plugins.hasPlugin('java')

        project.configurations {
            staticAnalysis
        }
        project.dependencies {
            staticAnalysis PluginConstants.RULES
        }

        TextResource checkstyleRules = project.resources.text.fromArchiveEntry(project.configurations.staticAnalysis, 'checkstyle/modules.xml')
        TextResource pmdRules = project.resources.text.fromArchiveEntry(project.configurations.staticAnalysis, 'pmd/rules.xml')

        if (isJavaProject) {
            applyJavaCheckstyle(project, checkstyleRules, severity, excludeList)
            applyJavaPmd(project, pmdRules, severity, excludeList)
            applyJavaJacoco(project)
        } else if (isAndroidApp || isAndroidLib) {
            def variants = isAndroidApp ? project.android.variants : project.android.libraryVariants
            applyAndroidCheckstyle(project, variants, checkstyleRules, severity, excludeList)
            applyAndroidPmd(project, variants, pmdRules, severity, excludeList)
            applyAndroidLint(project, severity)
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
            tasks.withType(Checkstyle).all { task ->
                task.group = 'verification'
                task.exclude excludeList
            }
        }
        handleSeverityInCheckstyleReport(severity, project)
    }

    private void handleSeverityInCheckstyleReport(Severity severity, Project project) {
        if (severity == Severity.WARNINGS) {
            project.tasks.withType(Checkstyle).all { task ->
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
        project.with {
            apply plugin: 'jacoco'
            jacoco {
                toolVersion = '0.7.2.201409121644'
            }
            tasks.withType(JacocoReport).all { task ->
                task.group = 'verification'
            }
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
        handleSeverityInCheckstyleReport(severity, project)
    }

    private void applyAndroidJacoco(Project project, variants) {
        applyJavaJacoco(project)
        variants.all { variant ->
            def unitTestTask = project.tasks.getByName("test${variant.name.capitalize()}UnitTest")
            if (!unitTestTask) {
                return
            }
            def jacocoTask = project.tasks.create("jacoco${variant.name.capitalize()}UnitTest", JacocoReport)
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

    private void applyAndroidLint(Project project, Severity severity) {
        project.android {
            lintOptions {
                abortOnError(severity == Severity.ERRORS || severity == Severity.WARNINGS)
                warningsAsErrors(severity == Severity.WARNINGS)
            }
        }
    }

    private void applyJavaPmd(Project project, TextResource pmdRules, Severity severity, List<String> excludeList) {
        project.with {
            apply plugin: 'pmd'
            pmd {
                ruleSetConfig = pmdRules
                ignoreFailures = true // we resort to our own failure handling to consider severity level
            }
            tasks.withType(Pmd).all { task ->
                task.group = 'verification'
                task.exclude excludeList
                task.reports {
                    xml.enabled = true
                }
                handleSeverityInPmdReport(task, severity)
            }
        }
    }

    private void applyAndroidPmd(Project project, variants, TextResource pmdRules, Severity severity, List<String> excludeList) {
        project.with {
            apply plugin: 'pmd'
            def check = project.tasks['check']
            android.sourceSets.all { sourceSet ->
                def sourceDirs = sourceSet.java.srcDirs
                def notEmptyDirs = sourceDirs.findAll { it.list()?.length > 0 }
                if (notEmptyDirs.empty) {
                    return
                }
                Pmd pmd = project.tasks.create("pmd${sourceSet.name.capitalize()}", Pmd)
                pmd.with {
                    group = 'verification'
                    description = "Run PMD analysis for ${sourceSet.name} classes"
                    ignoreFailures = true // we resort to our own failure handling to consider severity level
                    source = sourceSet.java.srcDirs
                    ruleSetConfig = pmdRules
                    exclude excludeList
                    reports {
                        xml.enabled = true
                    }
                }
                handleSeverityInPmdReport(pmd, severity)
                variants.all { variant ->
                    pmd.mustRunAfter variant.javaCompile
                }
                check.dependsOn pmd
            }
        }
    }

    private void handleSeverityInPmdReport(Pmd pmd, Severity severity) {
        if (severity != Severity.NONE) {
            pmd.doLast {
                File xmlReportFile = pmd.reports.xml.destination
                File htmlReportFile = new File(xmlReportFile.absolutePath - '.xml' + '.html')
                if (xmlReportFile.exists()) {
                    CharSequence pattern = (severity == Severity.WARNINGS ? "priority=\"[123]\"" : "priority=\"1\"")
                    def violations = xmlReportFile.text.findAll(pattern)
                    if (!violations.empty) {
                        throw new GradleException("${violations.size()} Fatal PMD rule violations were found. See the report at: ${new ConsoleRenderer().asClickableFileUrl(htmlReportFile ?: xmlReportFile)}")
                    }
                }
            }
        }
    }

}
