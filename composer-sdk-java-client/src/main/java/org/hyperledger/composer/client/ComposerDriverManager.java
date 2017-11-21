/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.composer.ComposerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

public class ComposerDriverManager<D extends ComposerDriver<U, C>, U extends ComposerUser, C extends ComposerConnector> {
	protected final CopyOnWriteArrayList<D> registeredDrivers;
	private static final Logger logger = LoggerFactory.getLogger(ComposerDriverManager.class);

	protected ComposerDriverManager() {
		this.registeredDrivers = new CopyOnWriteArrayList<>();
	}

	protected C getConnector(String connString, Wallet<U> userWallet)
			throws ComposerException {
		if (StringUtils.isEmpty(connString)) {
			throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "ConnectionProfile cannot be empty");
		}

		logger.debug("getConnection(\"{}\")", connString);

		// Walk through the loaded registeredDrivers attempting to make a connection.
		// Remember the first exception that gets raised so we can reraise it.
		ComposerException reason = null;

		for (ComposerDriver<U, C> driver : registeredDrivers) {
			try {
				logger.debug("trying {}", driver.getClass().getName());
				if (!driver.acceptsConnectionString(connString)) {
					continue;
				}

				C con = driver.getConnector(connString, userWallet);
				if (con != null) {
					// Success!
					logger.debug("getConnector returning {}", driver.getClass().getName());
					return con;
				}
			} catch (ComposerException ex) {
				if (reason == null) {
					reason = ex;
				}
			}

		}

		// if we got here nobody could connect.
		if (reason != null) {
			logger.debug("getConnection failed. ", reason);
			throw reason;
		}

		logger.debug("getConnection: no suitable driver found for {}", connString);
		throw new ComposerException(ComposerException.INVALID_INPUT_ERROR, "No suitable driver found for " + connString);
	}

	protected C _enroll(String connString, Wallet<U> wallet, EnrollRequest request) throws ComposerException {
		C connector = getConnector(connString, wallet);
		request.enroll(connector);
		return connector;
	}

	protected String _register(String connString, Wallet<U> userWallet, RegisterRequest request) throws ComposerException {
		C connector = getConnector(connString, userWallet);
		return request.register(connector);
	}

	protected C _registerAndEnroll(String connString, Wallet<U> userWallet, RegisterRequest request) throws ComposerException {
		String secret = _register(connString, userWallet, request);
		return _enroll(connString, userWallet, request.toEnrollRequest(secret));
	}

	protected C _connect(String connString, Wallet<U> userWallet, String userId, String affiliation)
			throws ComposerException {
		C connector = getConnector(connString, userWallet);
		connector.login(new ComposerIdentity().userID(userId).affiliation(affiliation));
		return connector;
	}

	protected synchronized void _registerDriver(D driver) {
		/* Register the driver if it has not already been added to our list */
		if (driver != null && registeredDrivers.addIfAbsent(driver)) {
			logger.info("registerDriver: {}", driver);
		}
	}

	static CopyOnWriteArrayList<ComposerDriver<ComposerUser, ComposerConnector>> drivers() {
		return ComposerDriverManagerHolder.instance.registeredDrivers;
	}

	static class ComposerDriverManagerHolder {
		static ComposerDriverManager<ComposerDriver<ComposerUser, ComposerConnector>, ComposerUser , ComposerConnector> instance = new ComposerDriverManager<>();
	}

	public static synchronized void registerDriver(ComposerDriver<ComposerUser, ComposerConnector> driver) {
		ComposerDriverManagerHolder.instance._registerDriver(driver);
	}

	public static ComposerConnection enroll(String connString, Wallet<ComposerUser> userWallet, EnrollRequest request) throws ComposerException {
		return new ComposerConnection<>(ComposerDriverManagerHolder.instance._enroll(connString, userWallet, request));
	}

	public static String register(String connString, Wallet<ComposerUser> userWallet, RegisterRequest request) throws ComposerException {
		return ComposerDriverManagerHolder.instance._register(connString, userWallet, request);
	}

	public static ComposerConnection registerAndEnroll(String connString, Wallet<ComposerUser> userWallet, RegisterRequest request) throws ComposerException {
		return new ComposerConnection<>(ComposerDriverManagerHolder.instance._registerAndEnroll(connString, userWallet, request));
	}

	public static ComposerConnection connect(String connString, Wallet<ComposerUser> userWallet, String userId, String affiliation) throws ComposerException {
		return new ComposerConnection<>(ComposerDriverManagerHolder.instance._connect(connString, userWallet, userId, affiliation));
	}
}
