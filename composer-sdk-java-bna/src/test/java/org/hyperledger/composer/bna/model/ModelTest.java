/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.bna.model;

import org.hyperledger.composer.annotation.Asset;
import org.hyperledger.composer.annotation.Concept;
import org.hyperledger.composer.annotation.Transaction;
import org.hyperledger.composer.bna.part.CTOPart;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ModelTest {
	@Test
	public void testIsPointerType() throws Exception {
		assertTrue(Model.isPointerType(TestModel.class.getDeclaredField("asset")));
		assertFalse(Model.isPointerType(TestModel.class.getDeclaredField("notPointer")));
	}

	@Test
	public void testAddField() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock);
		model.name("model").namespace("ns").type(Asset.class).parent("pns", "pName");
		FieldModel primary = mock(FieldModel.class);
		when(primary.toString()).thenReturn("\to string hello");
		when(primary.name()).thenReturn("hello");
		model.addField(primary, true);
		FieldModel nonPrimary = mock(FieldModel.class);
		when(nonPrimary.toString()).thenReturn("\to string world");
		model.addField(nonPrimary, false);
		assertEquals(model.toString(), "asset model identified by hello {\n\to string hello\n\to string world\n}\n");
		verify(primary).name();
	}

	@Test
	public void testExtends() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Asset.class).parent("pns", "pName");
		model.addField(when(mock(FieldModel.class).toString()).thenReturn("\to string world").getMock(), false);
		assertEquals(model.toString(), "asset model extends pName {\n\to string world\n}\n");
	}

	@Test
	public void testTransactionExtends() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Transaction.class).parent("pns", "pName");
		model.addField(when(mock(FieldModel.class).toString()).thenReturn("\to string world").getMock(), false);
		assertEquals(model.toString(), "transaction model extends pName {\n\to string world\n}\n");
	}

	@Test
	public void testConceptExtends() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Concept.class).parent("pns", "pName");
		model.addField(when(mock(FieldModel.class).toString()).thenReturn("\to string world").getMock(), false);
		assertEquals(model.toString(), "concept model extends pName {\n\to string world\n}\n");
	}

	@Test
	public void testTransactionNoExtends() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Transaction.class).parent(null, null);
		model.addField(when(mock(FieldModel.class).toString()).thenReturn("\to string world").getMock(), false);
		assertEquals(model.toString(), "transaction model {\n\to string world\n}\n");
	}

	@Test
	public void testConceptNoExtends() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Concept.class).parent(null, null);
		model.addField(when(mock(FieldModel.class).toString()).thenReturn("\to string world").getMock(), false);
		assertEquals(model.toString(), "concept model {\n\to string world\n}\n");
	}

	@Test(expectedExceptions = IllegalArgumentException.class,
			expectedExceptionsMessageRegExp = "primary key of String type must be given for ns.model")
	public void testNoExtendsNoPrimary() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Asset.class);
		model.addField(when(mock(FieldModel.class).toString()).thenReturn("\to string world").getMock(), false);
		System.out.println(model.toString());
	}

	@Test(expectedExceptions = IllegalArgumentException.class,
			expectedExceptionsMessageRegExp = "Duplicate primary key: t1, t2")
	public void testDuplicatePrimaryKey() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Asset.class);
		model.addField(when(mock(FieldModel.class).name()).thenReturn("t1").getMock(), true);
		model.addField(when(mock(FieldModel.class).name()).thenReturn("t2").getMock(), true);
	}

	@Test(expectedExceptions = IllegalArgumentException.class,
			expectedExceptionsMessageRegExp = "No primary key for transaction type:ns.model")
	public void testTransactionWithPrimaryKey() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Transaction.class);
		model.addField(null, true);
	}

	@Test(expectedExceptions = IllegalArgumentException.class,
			expectedExceptionsMessageRegExp = "No field named 'transactionId' for transaction type:ns.model")
	public void testTransactionWithIdField() throws Exception {
		CTOPart mock = mock(CTOPart.class);
		Model model = new Model(mock).name("model").namespace("ns").type(Transaction.class);
		model.addField(when(mock(FieldModel.class).name()).thenReturn("transactionId").getMock(), false);
	}
}