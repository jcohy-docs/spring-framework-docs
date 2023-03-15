package com.jcohy.docs.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.jcohy.gradle.asciidoctor.AsciidoctorConventionsPlugin;
import io.github.jcohy.gradle.conventions.ConventionsPlugin;
import io.github.jcohy.gradle.deployed.DeployedPlugin;
import org.asciidoctor.gradle.jvm.AbstractAsciidoctorTask;
import org.asciidoctor.gradle.jvm.AsciidoctorJExtension;
import org.asciidoctor.gradle.jvm.AsciidoctorJPlugin;
import org.asciidoctor.gradle.jvm.AsciidoctorTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.Sync;

/**
 * Copyright: Copyright (c) 2021 <a href="https://www.jcohy.com" target="_blank">jcohy.com</a>
 *
 * <p> Description:
 *
 * @author jiac
 * @version 1.0.0 2021/7/5:23:11
 * @since 1.0.0
 */
public class JcohyAsciidoctorPlugins implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        plugins.apply(AsciidoctorJPlugin.class);
        plugins.apply(AsciidoctorConventionsPlugin.class);
        plugins.apply(ConventionsPlugin.class);
        plugins.apply(DeployedPlugin.class);
        createAsciidoctorMultiPageTask(project);
        project.getTasks().withType(AbstractAsciidoctorTask.class,(asciidoctorTask) -> {
            configureAsciidoctorTask(project, asciidoctorTask);
            asciidoctorTask.setGroup("documentation");
        });
        project.getConfigurations().getByName("asciidoctorExtensions",(configuration) -> {
            configuration.getDependencies().add(project.getDependencies()
                    .create("org.asciidoctor:asciidoctorj-pdf:1.5.3"));
        });
    }

    private void createAsciidoctorMultiPageTask(Project project) {
        project.getTasks().create("asciidoctorMultipage", AsciidoctorTask.class,asciidoctorTask -> {
            asciidoctorTask.sources("*.adoc");
            replaceLogo(project,asciidoctorTask);
        });
    }

    private void configureAsciidoctorTask(Project project, AbstractAsciidoctorTask asciidoctorTask) {
        asciidoctorTask.languages("zh-cn");
        if(asciidoctorTask.getName().equals("asciidoctor") || asciidoctorTask.getName().equals("asciidoctorPdf")) {
            asciidoctorTask.sources("index.singleadoc");
        }
        asciidoctorTask.setLogDocuments(true);
        configureCommonAttributes(project, asciidoctorTask);
        project.getExtensions().getByType(AsciidoctorJExtension.class).fatalWarnings(false);
        project.getTasks()
                .withType(Sync.class, (sync -> sync.from("src/main/resources", (spec) -> {
                    spec.into("main/resources");
                    spec.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
                })));
    }

    private void configureCommonAttributes(Project project, AbstractAsciidoctorTask asciidoctorTask) {
        Map<String, Object> attributes = ProjectVersion.getAttributesMap();
        attributes.put("spring-boot-xsd-version",getVersion());
        Map<String, Object> docsUrlMaps = ProjectVersion.getDocsUrlMaps();
        addAsciidoctorTaskAttributes(project,attributes);
        asciidoctorTask.attributes(attributes);
        asciidoctorTask.attributes(docsUrlMaps);
    }

    private String getVersion() {
        String[] versionEl = ProjectVersion.SPRING_BOOT.getVersion().split("\\.");
        return versionEl[0] + "." + versionEl[1];
    }

    private void addAsciidoctorTaskAttributes(Project project,Map<String, Object> attributes) {
        attributes.put("rootProject", project.getRootProject().getProjectDir());
        attributes.put("sources-root", project.getProjectDir() + "/src");
        attributes.put("image-resource", project.getRootDir() + "/src/docs/asciidoc/images");
        attributes.put("spring-api-doc", "https://docs.spring.io/" + project.getName());
        attributes.put("doc-root","https://docs.jcohy.com");
        attributes.put("spring-docs-prefix","https://docs.spring.io/spring-framework/docs/");
        attributes.put("gh-samples-url","https://github.com/spring-projects/spring-security/master/");
        attributes.put("docs-java", project.getRootDir() + "/src/main/java/org/springframework/docs");
    }

    private void replaceLogo(Project project, AbstractAsciidoctorTask asciidoctorTask) {
        // 替换 logo
        asciidoctorTask.doLast((replaceIcon) -> {
            String language = asciidoctorTask.getLanguages().contains("zh-cn") ? "/zh-cn": "";
            project.delete(project.getBuildDir() + "/docs/asciidocMultipage" + language+ "/img/banner-logo.svg");
            try {
                Files.copy(Objects.requireNonNull(AsciidoctorConventionsPlugin.class.getResourceAsStream("/data/images/banner-logo.svg")),
                        Paths.get(project.getBuildDir() + "/docs/asciidocMultipage" + language+ "/img/banner-logo.svg"));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
