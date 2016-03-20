import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class GenerateConstants extends DefaultTask {

  @Input
  String pluginVersion

  @Input
  String pluginGroup

  @Input
  File outputDir

  @TaskAction
  void generateConstants() {
    File constantsFile = new File(outputDir, 'com/archano/gradle/staticanalysis/PluginConstants.java')
    if (constantsFile.parentFile.exists() || constantsFile.parentFile.mkdirs()) {
      constantsFile.write "package com.archano.gradle.staticanalysis;\n" +
              "\n" +
              "public final class PluginConstants {\n" +
              "\tpublic static final String VERSION = \"$pluginVersion\";\n" +
              "\tpublic static final String RULES = \"$pluginGroup:rules:$pluginVersion\";\n" +
              "}"
    } else {
      throw new GradleException("Impossible to generate file $constantsFile")
    }
  }

}
