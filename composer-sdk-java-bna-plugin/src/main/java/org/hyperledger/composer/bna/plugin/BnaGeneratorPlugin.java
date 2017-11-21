/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.TaskAction;
import org.hyperledger.composer.bna.util.BnaGenerator;
import org.hyperledger.composer.bna.util.JavaParser;
import org.hyperledger.composer.bna.util.ZipHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;

public class BnaGeneratorPlugin extends DefaultTask implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getExtensions().create("bna", BNAGeneratorExtension.class, project);
		project.getTasks().create("generate", BnaGeneratorPlugin.class)
				.dependsOn(project.getTasksByName("classes", false))
				.setGroup("bna");
	}

	@TaskAction
	public void generate() throws Exception {
		BNAGeneratorExtension extension = getProject().getExtensions().getByType(BNAGeneratorExtension.class);
		FileOutputStream bnaOutputStream = new FileOutputStream(extension.getOutputDir());
		JavaParser javaParser = new JavaParser(getSourceFiles());
		ZipHolder zipHolder = new ZipHolder(bnaOutputStream);
		String name = getProject().getName();
		String version = getProject().getVersion().toString();
		String description = extension.getDescription();
		BnaGenerator bnaGenerator = new BnaGenerator(javaParser, zipHolder, name, version, description);
		bnaGenerator.generate();
	}

	private List<File> getSourceFiles() {
		List<File> sourceFiles = new LinkedList<>();
		getProject().getConfigurations().findByName("runtime").forEach(sourceFiles::add);
		getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
				.getByName("main").getOutput().getClassesDirs().forEach(sourceFiles::add);
		return sourceFiles;
	}

}
