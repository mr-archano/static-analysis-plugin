package com.archano.gradle.staticanalysis

import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.resources.TextResource
import org.gradle.testing.jacoco.tasks.JacocoReport

class AndroidConfigurator extends JavaConfigurator {

    private final Object variants

    AndroidConfigurator(Project project, Severity severity, Object variants) {
        super(project, severity)
        this.variants = variants
    }

    @Override
    void configureCheckstyle(TextResource checkstyleRules, List<String> excludeList) {
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
                    description = "Run Checkstyle analysis for ${sourceSet.name} classes"
                    source = sourceSet.java.srcDirs
                    classpath = project.files("$project.buildDir/intermediates/classes/")
                }
                configureCheckstyle(checkstyle, checkstyleRules, excludeList)
                variants.all { variant ->
                    checkstyle.mustRunAfter variant.javaCompile
                }
                check.dependsOn checkstyle
            }
        }
    }

    @Override
    void configurePmd(TextResource pmdRules, List<String> excludeList) {
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
                pmd.source = sourceSet.java.srcDirs
                pmd.description = "Run PMD analysis for ${sourceSet.name} classes"
                configurePmd(pmd, pmdRules, excludeList)
                variants.all { variant ->
                    pmd.mustRunAfter variant.javaCompile
                }
                check.dependsOn pmd
            }
        }
    }

    @Override
    void configureLint() {
        project.android {
            lintOptions {
                abortOnError(severity == Severity.ERRORS || severity == Severity.WARNINGS)
                warningsAsErrors(severity == Severity.WARNINGS)
            }
        }
    }

    @Override
    void configureJaCoCo() {
        super.configureJaCoCo()
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
}
