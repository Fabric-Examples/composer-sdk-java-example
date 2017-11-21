/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer;

import org.hyperledger.composer.annotation.Asset;
import org.hyperledger.composer.annotation.DataField;
import org.hyperledger.composer.annotation.Pointer;
import org.hyperledger.composer.query.SelectQuery;
import org.hyperledger.composer.system.*;
import org.mockito.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.hyperledger.composer.ComposerException.INVALID_INPUT_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class ComposerAPITest {

	private final User SAMPLE_PARTICIPANT = new User("234");
	private final SampleAsset SAMPLE_ASSET = new SampleAsset().id("123").value(123).user(SAMPLE_PARTICIPANT);

	@DataProvider
	public static Object[][] composerApiException() {
		return new Object[][]{
				{(ComposerApiFunction) api -> api.getAsset(null, null), "Cannot get asset of null class"},
				{(ComposerApiFunction) api -> api.getParticipant(null, null), "Cannot get participant of null class"},
				{(ComposerApiFunction) api -> api.removeAssets(null, Collections.singleton("213")), "Cannot remove assets with null asset class"},
				{(ComposerApiFunction) api -> api.removeAsset(null, null), "Cannot remove asset with null asset class"},
				{(ComposerApiFunction) api -> api.removeParticipants(null, Collections.singleton("213")), "Cannot remove participants with null asset class"},
				{(ComposerApiFunction) api -> api.removeParticipant(null, null), "Cannot remove participant with null asset class"},
		};
	}

	@Mock
	Engine engine;

	@Captor
	ArgumentCaptor<String> jsonCaptor;

	@Spy
	@InjectMocks
	ComposerAPI composerAPI;

	@BeforeMethod(alwaysRun = true)
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testSubmitTransaction() throws Exception {
		AddAsset addAsset = new AddAsset();
		addAsset.targetRegistry = ResourceSerializer.fromID(SampleAsset.class.getName(), AssetRegistry.class);
		addAsset.resources = new SampleAsset[]{new SampleAsset().id("123").value(123).user(new User("222"))};
		String serializedResource = ResourceSerializer.toJSONString(addAsset);
		when(engine.submitTransaction(serializedResource)).thenReturn("result");
		assertEquals(composerAPI.submitTransaction(addAsset), "result");
		verify(engine).submitTransaction(serializedResource);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "exception")
	public void testSubmitTransactionWithComposerException() throws Exception {
		AddAsset addAsset = new AddAsset();
		doThrow(new ComposerException("exception")).when(engine).submitTransaction(any());
		composerAPI.submitTransaction(addAsset);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "failed to submit transaction:.*")
	public void testSubmitTransactionWithThrowable() throws Exception {
		RemoveAsset addAsset = new RemoveAsset();
		doThrow(new RuntimeException("exception")).when(engine).submitTransaction(any());
		composerAPI.submitTransaction(addAsset);
	}

	@Test
	public void testCreateParticipant() throws Exception {
		testParticipantOperation(ComposerAPI::createParticipant, AddParticipant.class, this::addUpdateParticipantCheck);
	}

	@Test
	public void testUpdateParticipant() throws Exception {
		testParticipantOperation(ComposerAPI::updateParticipant, UpdateParticipant.class, this::addUpdateParticipantCheck);
	}

	@Test
	public void testRemoveParticipant() throws Exception {
		testParticipantOperation((api, o) -> api.removeParticipant(User.class, "123"), RemoveParticipant.class, this::removeParticipantCheck);
	}

	@Test
	public void testGetParticipant() throws Exception {
		when(engine.getResourceInRegistry(ComposerAPI.RegistryType.Participant.name(), User.class.getName(), "123"))
				.thenReturn(ResourceSerializer.toJSONString(SAMPLE_PARTICIPANT));
		assertEquals(composerAPI.getParticipant(User.class, "123"), SAMPLE_PARTICIPANT);
		verify(engine).getResourceInRegistry(ComposerAPI.RegistryType.Participant.name(), User.class.getName(), "123");
	}

	@Test
	public void testCreateAsset() throws Exception {
		testAssetOperation(ComposerAPI::createAsset, AddAsset.class, this::addUpdateAssetCheck);
	}

	@Test
	public void testUpdateAsset() throws Exception {
		testAssetOperation(ComposerAPI::updateAsset, UpdateAsset.class, this::addUpdateAssetCheck);
	}

	@Test
	public void testRemoveAsset() throws Exception {
		testAssetOperation((api, o) -> api.removeAsset(SampleAsset.class, "123"), RemoveAsset.class, this::removeAssetCheck);
	}

	@Test
	public void testGetAsset() throws Exception {
		when(engine.getResourceInRegistry(ComposerAPI.RegistryType.Asset.name(), SampleAsset.class.getName(), "123"))
				.thenReturn(ResourceSerializer.toJSONString(SAMPLE_ASSET));
		assertEquals(composerAPI.getAsset(SampleAsset.class, "123"), SAMPLE_ASSET);
		verify(engine).getResourceInRegistry(ComposerAPI.RegistryType.Asset.name(), SampleAsset.class.getName(), "123");
	}

	@Test
	public void testQuery() throws Exception {
		when(engine.executeQuery("build", "SELECT org.hyperledger.composer.SampleAsset WHERE (id='123')", "{}"))
				.thenReturn(ResourceSerializer.toJSONString(new SampleAsset[]{SAMPLE_ASSET}));
		SelectQuery select = composerAPI.select();
		List<SampleAsset> result = select.from(SampleAsset.class).where("id='123'").build().execute();
		assertEquals(result, Collections.singletonList(SAMPLE_ASSET));
	}

	@Test
	public void testQueryException() throws Exception {
		when(engine.executeQuery("build", "SELECT org.hyperledger.composer.SampleAsset WHERE (id='123')", "{}"))
				.thenReturn("illegal json");
		SelectQuery select = composerAPI.select();
		try {
			select.from(SampleAsset.class).where("id='123'").build().execute();
		} catch (ComposerException e) {
			assertEquals(e.getErrorCode(), INVALID_INPUT_ERROR);
			assertEquals(e.getMessage(), "parse executeQuery result error");
		}
	}

	@Test(dataProvider = "composerApiException")
	public void testException(ComposerApiFunction func, String message) {
		try {
			func.func(composerAPI);
		} catch (ComposerException e) {
			assertEquals(e.getErrorCode(), INVALID_INPUT_ERROR);
			assertEquals(e.getMessage(), message);
		}
	}

	private void addUpdateAssetCheck(AssetTransaction assetTx, Object o) {
		assertEquals(assetTx.resources.length, 1);
		assertEquals(assetTx.resources[0], o);
	}

	private void addUpdateParticipantCheck(ParticipantTransaction participantTx, Object o) {
		assertEquals(participantTx.resources.length, 1);
		assertEquals(participantTx.resources[0], o);
	}

	private void removeAssetCheck(AssetTransaction assetTx, Object o) {
		assertEquals(assetTx.resources.length, 0);
		assertEquals(((RemoveAsset) assetTx).resourceIds.length, 1);
		assertEquals(((RemoveAsset) assetTx).resourceIds[0], "123");
	}

	private void removeParticipantCheck(ParticipantTransaction participantTx, Object o) {
		assertEquals(participantTx.resources.length, 0);
		assertEquals(((RemoveParticipant) participantTx).resourceIds.length, 1);
		assertEquals(((RemoveParticipant) participantTx).resourceIds[0], "123");
	}

	private void testAssetOperation(TransactionFunction func, Class<? extends AssetTransaction> txClass, AssetTxCheckFunction checker) throws Exception {
		func.func(composerAPI, SAMPLE_ASSET);
		verify(engine).submitTransaction(jsonCaptor.capture());
		AssetTransaction assetTx = ResourceSerializer.fromJSON(jsonCaptor.getValue(), txClass);
		assertNotNull(assetTx);
		assertEquals(assetTx.targetRegistry.registryId, SampleAsset.class.getName());
		checker.check(assetTx, SAMPLE_ASSET);
	}

	private void testParticipantOperation(TransactionFunction func, Class<? extends ParticipantTransaction> txClass, ParticipantTxCheckFunction checker) throws Exception {
		func.func(composerAPI, SAMPLE_PARTICIPANT);
		verify(engine).submitTransaction(jsonCaptor.capture());
		ParticipantTransaction participantTx = ResourceSerializer.fromJSON(jsonCaptor.getValue(), txClass);
		assertNotNull(participantTx);
		assertEquals(participantTx.targetRegistry.registryId, User.class.getName());
		checker.check(participantTx, SAMPLE_PARTICIPANT);
	}
}

@FunctionalInterface
interface TransactionFunction {
	void func(ComposerAPI api, Object o) throws ComposerException;
}

@FunctionalInterface
interface ComposerApiFunction {
	void func(ComposerAPI api) throws ComposerException;
}

@FunctionalInterface
interface AssetTxCheckFunction {
	void check(AssetTransaction tx, Object o) throws ComposerException;
}

@FunctionalInterface
interface ParticipantTxCheckFunction {
	void check(ParticipantTransaction tx, Object o) throws ComposerException;
}

@Asset
class SampleAsset {
	@DataField(primary = true)
	String id;

	@DataField
	int value;

	@Pointer
	User user;

	public SampleAsset id(String id) {
		this.id = id;
		return this;
	}

	public SampleAsset value(int value) {
		this.value = value;
		return this;
	}

	public SampleAsset user(User user) {
		this.user = user;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SampleAsset that = (SampleAsset) o;

		if (value != that.value) return false;
		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		return user != null ? user.equals(that.user) : that.user == null;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + value;
		result = 31 * result + (user != null ? user.hashCode() : 0);
		return result;
	}
}