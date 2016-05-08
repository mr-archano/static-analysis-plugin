package com.archano.gradle.staticanalysis

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.resources.TextResource
import org.gradle.logging.ConsoleRenderer
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
                task.group = 'verification'
                task.exclude excludeList
                handleSeverityInCheckstyleReport(task)
            }
        }
    }

    protected void handleSeverityInCheckstyleReport(Checkstyle checkstyle) {
        if (severity == Severity.WARNINGS) {
            checkstyle.doLast {
                File xmlReportFile = checkstyle.reports.xml.destination
                File htmlReportFile = new File(xmlReportFile.absolutePath - '.xml' + '.html')
                if (xmlReportFile.exists() && xmlReportFile.text.contains("<error ")) {
                    throw new GradleException("Checkstyle rule violations were found. See the report at: ${htmlReportFile ?: xmlReportFile}")
                }
            }
        }
    }

    @Override
    void configurePmd(TextResource pmdRules, List<String> excludeList) {
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
                handleSeverityInPmdReport(task)
            }
        }
    }

    protected void handleSeverityInPmdReport(Pmd pmd) {
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
