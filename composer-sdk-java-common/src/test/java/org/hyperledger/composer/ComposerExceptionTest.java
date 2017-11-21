/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ComposerExceptionTest {

	@Test
	public void testComposerException() {
		ComposerException e = new ComposerException(404, "message1", new RuntimeException());
		assertEquals(e.getErrorCode(), 404);
		assertEquals(e.getMessage(), "message1");

		e = new ComposerException("message2");
		assertEquals(e.getErrorCode(), 500);
		assertEquals(e.getMessage(), "message2");

		e = new ComposerException(new RuntimeException());
		assertEquals(e.getErrorCode(), 500);
		assertEquals(e.getMessage(), "java.lang.RuntimeException");

		e = new ComposerException(new ComposerException(404, "404message"));
		assertEquals(e.getErrorCode(), 404);
		assertEquals(e.getMessage(), "404message");

		e = new ComposerException(new RuntimeException("xxx not exists"));
		assertEquals(e.getErrorCode(), 404);
		assertEquals(e.getMessage(), "xxx not exists");

		e = new ComposerException(new RuntimeException("xxx already exists"));
		assertEquals(e.getErrorCode(), 1009);
		assertEquals(e.getMessage(), "xxx already exists");

		assertEquals(new ComposerException(233, "233"), new ComposerException(233, "233"));
		assertEquals(new ComposerException(233, "233").hashCode(), new ComposerException(233, "233").hashCode());

		e = new ComposerException(new RuntimeException(new ComposerException(111, "111")));
		assertEquals(e.getErrorCode(), 111);
		assertEquals(e.getMessage(), "111");

	}
}
