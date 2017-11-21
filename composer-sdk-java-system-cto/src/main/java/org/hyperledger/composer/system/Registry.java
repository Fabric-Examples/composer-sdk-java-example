/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// this code is generated and should not be modified
package org.hyperledger.composer.system;

@org.hyperledger.composer.annotation.Asset
public abstract class Registry extends Object {
	@org.hyperledger.composer.annotation.DataField(primary=true, optional=false, embedded=true)
	public String registryId;
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=false, embedded=true)
	public String name;
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=false, embedded=true)
	public String type;
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=false, embedded=true)
	public boolean system;
}
