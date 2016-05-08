package com.archano.gradle.staticanalysis

import org.gradle.api.Project

enum Severity {

    NONE, ERRORS, WARNINGS

    static Severity from(Project project) {
        project.hasProperty('severity') ? valueOf(project.severity.toUpperCase()) : NONE
    }

}
