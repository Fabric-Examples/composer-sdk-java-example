/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import org.hyperledger.composer.client.ComposerUser;
import org.hyperledger.fabric.sdk.Enrollment;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.PrivateKey;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class FabricUserTest {

	@Mock
	ComposerUser user;

	@Mock
	PrivateKey key;

	@Mock
	FabricUser mockFabricUser;

	@InjectMocks
	FabricUser fabricUser;

	@BeforeMethod(alwaysRun = true)
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetComposerUser() throws Exception {
		assertEquals(fabricUser.getComposerUser(), user);
	}

	@Test
	public void testGetName() throws Exception {
		when(user.getName()).thenReturn("name");
		assertEquals(fabricUser.getName(), "name");
		verify(user).getName();
	}

	@Test
	public void testGetRoles() throws Exception {
		when(user.getRoles()).thenReturn(Collections.emptySet());
		assertTrue(fabricUser.getRoles().isEmpty());
		verify(user).getRoles();
	}

	@Test
	public void testGetAccount() throws Exception {
		when(user.getAccount()).thenReturn("account");
		assertEquals(fabricUser.getAccount(), "account");
		verify(user).getAccount();
	}

	@Test
	public void testGetAffiliation() throws Exception {
		when(user.getAffiliation()).thenReturn("affiliation");
		assertEquals(fabricUser.getAffiliation(), "affiliation");
		verify(user).getAffiliation();
	}

	@Test
	public void testGetEnrollment() throws Exception {
		when(user.getCert()).thenReturn("cert");
		when(user.getPrivateKey()).thenReturn(key);
		Enrollment enrollment = fabricUser.getEnrollment();
		assertEquals(enrollment.getCert(), "cert");
		assertEquals(enrollment.getKey(), key);
		verify(user).getCert();
		verify(user).getPrivateKey();
	}

	@Test
	public void testGetMspId() throws Exception {
		when(user.getMspId()).thenReturn("mspId");
		assertEquals(fabricUser.getMspId(), "mspId");
		verify(user).getMspId();
	}

	@Test
	public void testIsEnrolled() throws Exception {
		when(mockFabricUser.getEnrollment()).thenReturn(mock(Enrollment.class));
		assertTrue(FabricUser.isEnrolled(mockFabricUser));
	}

	@Test
	public void testIsNotEnrolled() throws Exception {
		when(mockFabricUser.getEnrollment()).thenReturn(null);
		assertFalse(FabricUser.isEnrolled(mockFabricUser));
	}
}
