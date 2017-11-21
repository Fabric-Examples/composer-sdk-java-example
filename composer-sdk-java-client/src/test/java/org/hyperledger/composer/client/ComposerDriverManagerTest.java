/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.client;

import org.hyperledger.composer.ComposerException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;


public class ComposerDriverManagerTest {
	@Mock
	ComposerDriver driver;

	@Mock
	Wallet<ComposerUser> wallet;

	@Mock
	ComposerConnector connector;

	@Mock
	EnrollRequest enrollRequest;

	@Mock
	RegisterRequest registerRequest;

	@BeforeMethod(alwaysRun = true)
	public void init() {
		MockitoAnnotations.initMocks(this);
		ComposerDriverManager.registerDriver(driver);
	}

	@AfterMethod(alwaysRun = true)
	public void restoreDrivers() {
		ComposerDriverManager.ComposerDriverManagerHolder.instance.registeredDrivers.clear();
	}

	@Test
	public void testRegisterDriver() throws Exception {
		assertEquals(ComposerDriverManager.drivers().size(), 1);
		assertTrue(ComposerDriverManager.drivers().contains(driver));

		ComposerDriverManager.registerDriver(driver);
		assertEquals(ComposerDriverManager.drivers().size(), 1);
		assertTrue(ComposerDriverManager.drivers().contains(driver));

		ComposerDriverManager.registerDriver(null);
		assertEquals(ComposerDriverManager.drivers().size(), 1);
		assertTrue(ComposerDriverManager.drivers().contains(driver));
	}

	@Test
	public void testRegisterAndEnroll() throws Exception {
		final String secret = "secret", connStr = "connStr";
		when(driver.acceptsConnectionString(connStr)).thenReturn(true);
		when(driver.getConnector(connStr, wallet)).thenReturn(connector);
		when(registerRequest.register(connector)).thenReturn(secret);
		when(registerRequest.toEnrollRequest(secret)).thenReturn(enrollRequest);

		assertNotNull(ComposerDriverManager.registerAndEnroll(connStr, wallet, registerRequest));

		verify(driver, times(2)).acceptsConnectionString(connStr);
		verify(driver, times(2)).getConnector(connStr, wallet);
		verify(registerRequest).register(connector);
		verify(registerRequest).toEnrollRequest(secret);
		verify(enrollRequest).enroll(connector);
	}

	@Test
	public void testConnect() throws Exception {
		final String connStr = "connStr", affiliation = "affiliation", userId = "userId";
		when(driver.acceptsConnectionString(connStr)).thenReturn(true);
		when(driver.getConnector(connStr, wallet)).thenReturn(connector);

		assertNotNull(ComposerDriverManager.connect(connStr, wallet, userId, affiliation));

		verify(driver).acceptsConnectionString(connStr);
		verify(driver).getConnector(connStr, wallet);
		verify(connector).login(argThat(id -> affiliation.equals(id.affiliation()) && userId.equals(id.userID())));
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "ConnectionProfile cannot be empty")
	public void testConnectWithEmptyConnString() throws Exception {
		ComposerDriverManager.connect(null, wallet, "", "");
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "No suitable driver found for connStr")
	public void testConnectWithIgnoredDriver() throws Exception {
		final String connStr = "connStr";
		when(driver.acceptsConnectionString(connStr)).thenReturn(false);
		ComposerDriverManager.connect(connStr, wallet, "", "");
	}
	
	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "exception")
	public void testConnectWithGetConnectorException() throws Exception {
		final String connStr = "connStr";
		when(driver.acceptsConnectionString(connStr)).thenReturn(true);
		doThrow(new ComposerException("exception")).when(driver).getConnector(connStr, wallet);

		ComposerDriverManager.connect(connStr, wallet, "", "");
	}
}