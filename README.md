# static-analysis-plugin
**TL;DR:** A Gradle plugin to easily apply the same setup of static analysis tools across different Android or Java projects.<br/>
<br/>
### Why
Gradle supports many popular static analysis and reporting tools (Checkstyle, PMD, FindBugs, JaCoCo) via a set of built-in
plugins. Using these plugins in an Android module will require an additional setup to compensate for the differences between
the model adopted by the Android plugin compared to the the Java one.<br/>
The `static-analysis-plugin` aims to provide:
- easy, Android-friendly integration for all static analysis and reporting tools,
- convenient way of sharing same setup across different projects,
- healthy, versionable and configurable defaults.

The plugin is **heavily under development** and to be considered in pre-alpha stage. At the moment not all integrations
have been completed. The table below summarises the current status.

Tool | Android | Java
:----:|:--------:|:--------:
`Checkstyle` | :white_check_mark: | :white_check_mark:
`JaCoCo` | :white_check_mark: | :white_check_mark:
`PMD` | :x: | :x:
`FindBugs` | :x: | :x:
`Lint` | :x: | :x:
<br/>
### What
A set of custom tasks are generated by this plugin to provide all the support needed.
More details about those will follow soon.
<br/>
### How
The plugin is currently released via Jitpack. To include it in your project:

1)  Add Jitpack repo to your root `build.gradle`:
```groovy
  buildscript {
    repositories {
      ...
      maven { url "https://jitpack.io" }
    }
  }
  repositories {
    ...
    maven { url "https://jitpack.io" }
  }
```
2)  Add the buildscript dependency
```groovy
buildscript {
  dependencies {
    compile 'com.github.mr-archano.static-analysis-plugin:plugin:0.1.2'
  }
}
```

License
=======

    Copyright 2016 Antonio Bertucci.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.