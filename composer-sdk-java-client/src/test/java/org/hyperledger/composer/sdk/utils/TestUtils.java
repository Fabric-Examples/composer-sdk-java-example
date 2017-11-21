/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.sdk.utils;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

public class TestUtils {

	static public Process LauchComposerConnector(String port, Logger logger) throws InterruptedException {
		logger.info("Starting composer-connector-server using port {}...", port);
		Process process;
		try {
			process = new ProcessBuilder(
					"node","cli.js","-p", port)
					.directory(new File("../node_modules/composer-connector-server"))
					.redirectOutput(ProcessBuilder.Redirect.INHERIT)
					.redirectError(ProcessBuilder.Redirect.INHERIT).start();
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot launch composer-connector-server, please make sure it is installed.", e);
		}
		Thread.sleep(2000);
		return process;
	}
}
