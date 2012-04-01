package org.phancock.gradleplugins.readme

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.apache.tools.ant.filters.ReplaceTokens

/**
 * A Gradle Plugin for creating a README.html from the Textile template README.textile.
 * Functionality is provided to bind template variables as part of the build process (see README.html ;-))
 *
 */
public class ReadmePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        createPluginConventions(project)
        createReadmeTask(project)
    }

    private void createPluginConventions(project) {
        project.extensions.add('readme', ReadmeConvention)
        project.readme {
            src = project.relativePath('readme')
            dest = project.relativePath('.')
            templateVars = new TemplateVars()
        }
    }

    private void createReadmeTask(project) {
        project.task('createReadme') << {
            def readme = project.readme
            project.copy {
                from(readme.src)
                into(readme.dest)
                include ('README.textile')
                filter(ReplaceTokens, tokens: readme.templateVars)
            }
            project.ant {
                taskdef(classpath: project.rootProject.buildscript.configurations.classpath.asPath,
                        resource: 'org/eclipse/mylyn/wikitext/core/util/anttask/tasks.properties')
                'wikitext-to-html'(markupLanguage: 'Textile') {
                    fileset(dir: readme.dest) { include(name: 'README.textile') }
                    readme.stylesheets.each { url ->
                        stylesheet(url: url)
                    }
                }
            }
        }
    }
}

class ReadmeConvention {
    def src
    def dest
    def templateVars
    def stylesheets = []
}

class TemplateVars {
    @Delegate
    Map delegate = [:]

    @Override
    public void leftShift(File f) {
        mergeConfig(f.toURL())
    }

    @Override
    public void leftShift(String s) {
        mergeConfig(new File(s).toURL())
    }

    @Override
    public void leftShift(URL url) {
    }

    private void mergeConfig(URL url) {
        delegate << new ConfigSlurper().parse(url)
    }

    @Override
    public String toString() {
        delegate.toString()
    }
}
