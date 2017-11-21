/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// this code is generated and should not be modified
package org.hyperledger.composer.system;

@org.hyperledger.composer.annotation.Asset
public class HistorianRecord extends Object {
	@org.hyperledger.composer.annotation.DataField(primary=true, optional=false, embedded=true)
	public String transactionId;
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=false, embedded=true)
	public String transactionType;
	@org.hyperledger.composer.annotation.Pointer(optional=false)
	public Object transactionInvoked;
	@org.hyperledger.composer.annotation.Pointer(optional=true)
	public Object participantInvoking;
	@org.hyperledger.composer.annotation.Pointer(optional=true)
	public Identity identityUsed;
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=true, embedded=true)
	public Event[] eventsEmitted;
	@org.hyperledger.composer.annotation.DataField(primary=false, optional=false, embedded=true)
	public java.util.Date transactionTimestamp;
}
