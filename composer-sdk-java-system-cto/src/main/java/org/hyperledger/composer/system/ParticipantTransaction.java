/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// this code is generated and should not be modified
package org.hyperledger.composer.system;

@org.hyperledger.composer.annotation.Transaction
public abstract class ParticipantTransaction extends org.hyperledger.composer.system.RegistryTransaction {
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=false, embedded=true)
	public Object[] resources;
}
