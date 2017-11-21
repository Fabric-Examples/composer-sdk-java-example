/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// this code is generated and should not be modified
package org.hyperledger.composer.system;

@org.hyperledger.composer.annotation.Transaction
public class StartBusinessNetwork extends Object {
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=false, embedded=true)
	public String businessNetworkArchive;
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=true, embedded=true)
	public String logLevel;
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=true, embedded=true)
	public Object[] bootstrapTransactions;
}
