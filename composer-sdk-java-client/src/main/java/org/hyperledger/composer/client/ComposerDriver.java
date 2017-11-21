/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.hyperledger.composer.ComposerException;

public interface ComposerDriver<U extends ComposerUser, C extends ComposerConnector> {

	boolean acceptsConnectionString(String connStr);

	C getConnector(String connString, Wallet<U> userWallet) throws ComposerException;
}
