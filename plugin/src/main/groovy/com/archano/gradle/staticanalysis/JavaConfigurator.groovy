package com.archano.gradle.staticanalysis

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.resources.TextResource
import org.gradle.testing.jacoco.tasks.JacocoReport

class JavaConfigurator extends Configurator {

    JavaConfigurator(Project project, Severity severity) {
        super(project, severity)
    }

    @Override
    void configureCheckstyle(TextResource checkstyleRules, List<String> excludeList) {
        project.with {
            apply plugin: 'checkstyle'
            checkstyle {
                config = checkstyleRules
                ignoreFailures = severity == Severity.NONE
            }
            tasks.withType(Checkstyle).all { task ->
                configureCheckstyle(task, checkstyleRules, excludeList)
            }
        }
    }

    protected void configureCheckstyle(Checkstyle checkstyle, TextResource checkstyleRules, List<String> excludeList) {
        checkstyle.with {
            group = 'verification'
            config = checkstyleRules
            ignoreFailures = severity == Severity.NONE
            exclude excludeList
            if (severity == Severity.WARNINGS) {
                doLast {
                    File xmlReportFile = reports.xml.destination
                    File htmlReportFile = new File(xmlReportFile.absolutePath - '.xml' + '.html')
                    if (xmlReportFile.exists() && xmlReportFile.text.contains("<error ")) {
                        throw new GradleException("Checkstyle rule violations were found. See the report at: ${htmlReportFile ?: xmlReportFile}")
                    }
                }
            }
        }
    }

    @Override
    void configurePmd(TextResource pmdRules, List<String> excludeList) {
        project.with {
            apply plugin: 'pmd'
            tasks.withType(Pmd).all { task ->
                configurePmd(task, pmdRules, excludeList)
            }
        }
    }

    protected void configurePmd(Pmd pmd, TextResource pmdRules, List<String> excludeList) {
        pmd.with {
            group = 'verification'
            ignoreFailures = severity == Severity.NONE
            rulePriority = severity == Severity.WARNINGS ? 3 : 1
            ruleSetConfig = pmdRules
            exclude excludeList
            reports {
                xml.enabled = true
            }
        }
    }


    @Override
    void configureLint() {
        // TODO
    }

    @Override
    void configureJaCoCo() {
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

}
