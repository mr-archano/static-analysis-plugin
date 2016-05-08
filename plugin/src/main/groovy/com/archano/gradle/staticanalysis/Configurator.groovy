package com.archano.gradle.staticanalysis

import org.gradle.api.Project
import org.gradle.api.resources.TextResource

abstract class Configurator {

    protected final Project project
    protected final Severity severity

    static Configurator create(Project project) {
        Severity severity = Severity.from(project)
        boolean isJavaProject = project.plugins.hasPlugin('java')
        boolean isAndroidApp = project.plugins.hasPlugin('com.android.application')
        boolean isAndroidLib = project.plugins.hasPlugin('com.android.library')

        if (isJavaProject) {
            return new JavaConfigurator(project, severity)
        } else if (isAndroidApp || isAndroidLib) {
            def variants = isAndroidApp ? project.android.variants : project.android.libraryVariants
            return new AndroidConfigurator(project, severity, variants)
        } else {
            throw new UnsupportedOperationException('Only Java or Android projects are supported.')
        }
    }

    Configurator(Project project, Severity severity) {
        this.project = project
        this.severity = severity
    }

    abstract void configureCheckstyle(TextResource checkstyleRules, List<String> excludeList)

    abstract void configurePmd(TextResource pmdRules, List<String> excludeList)

    abstract void configureLint()

    abstract void configureJaCoCo()

}
