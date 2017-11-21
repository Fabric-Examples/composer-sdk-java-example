#!/usr/bin/env node
/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const JavaVisitor = require("./ctoparser");


const args = require('yargs')
	.option('inputBNA', {
		alias: 'i',
		describe: 'input .bna file path',
	})
	.option('outputDir', {
		alias: 'o',
		describe: 'output java class directory',
	})
	.demandOption(['inputBNA', 'outputDir'],
		'Please provide both inputBNA and outputDir arguments to work with this tool')
	.help()
	.argv;

new JavaVisitor()
	.visitAsync(args.outputDir, args.inputBNA)
	.catch((e) => {
		console.error(e);
	});
