/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.util;

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.testng.Assert.*;

public class ZipHolderTest {

	@Test
	public void testZip() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipHolder holder = new ZipHolder(out);
		byte[] content = "world".getBytes();
		holder.addPart("hello", content);
		holder.end();
		ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
		ZipEntry nextEntry = in.getNextEntry();
		assertNotNull(nextEntry);
		assertEquals(nextEntry.getName(), "hello");
		assertEquals(in.read(content), content.length);
		assertEquals(new String(content), "world");
		assertNull(in.getNextEntry());
	}
}