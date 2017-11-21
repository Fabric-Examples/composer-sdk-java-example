/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.util;

import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.bna.part.ACLPart;
import org.hyperledger.composer.bna.part.BNAPart;
import org.hyperledger.composer.bna.part.PackageJsonPart;
import org.hyperledger.composer.bna.part.QueryPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class BnaGenerator {

	private static final Logger logger = LoggerFactory.getLogger(BnaGenerator.class);

	private JavaParser javaParser;
	private ZipHolder zipHolder;
	private String packageJsonName;
	private String packageJsonVersion;
	private String packageJsonDescription;

	public BnaGenerator(JavaParser javaParser, ZipHolder zipHolder, String packageJsonName,
	                    String packageJsonVersion, String packageJsonDescription) {
		this.javaParser = javaParser;
		this.zipHolder = zipHolder;
		this.packageJsonName = packageJsonName;
		this.packageJsonVersion = packageJsonVersion;
		this.packageJsonDescription = packageJsonDescription;
	}

	public void generate() throws ComposerException {
		try {
			List<BNAPart> bnaParts = new LinkedList<>(javaParser.parseCTOModel());
			QueryPart queryPart = javaParser.parseQueryModel();
			if (queryPart != null) {
				bnaParts.add(queryPart);
			}
			bnaParts.add(new PackageJsonPart(packageJsonName, packageJsonVersion, packageJsonDescription));
			bnaParts.add(new ACLPart());
			for (BNAPart part : bnaParts) {
				logger.info("add bna part {}: {}", part.entryName(), part.toString());
				zipHolder.addPart(part.entryName(), part.toString().getBytes());
			}
			zipHolder.end();
		} catch (IOException e) {
			throw new ComposerException(e.getMessage());
		}
	}
}
