/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const engine = require('composer-runtime').Engine;
const FileWriter = require("./filewriter");
const version = require("./package.json");

class EngineGenerator {
	/**
	 * parse engine and output java file into given dir
	 * @param {string} dir - output dir path string
	 */
	constructor(dir) {
		this.fileWriter = new FileWriter(dir);
		this.generate();
	}

	generate() {
		console.log("generating Engine.java");
		this.fileWriter.openFile("Engine.java");
		this.fileWriter.writeLine(0, '/*');
        this.fileWriter.writeLine(0, ' * Copyright IBM Corp. 2017 All Rights Reserved.');
        this.fileWriter.writeLine(0, ' *');
        this.fileWriter.writeLine(0, ' * SPDX-License-Identifier: Apache-2.0');
        this.fileWriter.writeLine(0, ' */');
        this.fileWriter.writeLine(0, '');
		this.fileWriter.writeLine(0, '// this code is generated and should not be modified');
		this.fileWriter.writeLine(0, 'package org.hyperledger.composer;');
		this.fileWriter.writeLine(0, '');
		this.fileWriter.writeLine(0, 'public interface Engine {');
		this.fileWriter.writeLine(1, `String COMPOSER_VERSION = "${version.dependencies['composer-runtime']}";`);

		Object.getOwnPropertyNames(engine.prototype).forEach((method) => {
			const property = engine.prototype[method];
			if (typeof property !== 'function') return;
			this.writeMethod(property, method);
		});

		this.fileWriter.writeLine(0, '}');
		this.fileWriter.closeFile();
	}

	writeMethod(func, method) {
		switch (func.length) {
			case 2:
				const body = func.toString();
				const argsIndex = body.indexOf('[') + 1;
				const args = body.substring(argsIndex, body.indexOf(']', argsIndex)).replace(/'([^']+)'/g, 'String $1');
				this.fileWriter.writeLine(1, 'String ' + method + '(' + args + ') throws ComposerException;');
				break;
			case 4:
				this.fileWriter.writeLine(1, 'String ' + method + '(String func, String[] args) throws ComposerException;');
		}
	}
}

module.exports = EngineGenerator;