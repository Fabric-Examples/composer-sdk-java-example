/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const JavaVisitor = require("./ctoparser");
const EngineGenerator = require("./enginegen");

new EngineGenerator("../composer-sdk-java-common/src/main/java/org/hyperledger/composer");
new JavaVisitor()
	.visitAsync("../composer-sdk-java-system-cto/src/main/java")
	.catch((e) => {
		console.error(e);
	});
