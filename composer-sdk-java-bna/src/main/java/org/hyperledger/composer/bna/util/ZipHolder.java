/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ZipHolder {
	private final ZipOutputStream zip;

	public ZipHolder(OutputStream out) {
		this.zip = new ZipOutputStream(out);
	}

	public void addPart(String path, byte[] content) throws IOException {
		addPart(path, new ByteArrayInputStream(content));
	}

	public void end() throws IOException {
		this.zip.finish();
		this.zip.close();
	}

	private void addPart(String path, InputStream in) throws IOException {
		zip.putNextEntry(new ZipEntry(path));
		byte[] buffer = new byte[4 * 1024];
		for (int n; (n = in.read(buffer)) != -1; ) {
			zip.write(buffer, 0, n);
		}
		zip.closeEntry();
	}
}
