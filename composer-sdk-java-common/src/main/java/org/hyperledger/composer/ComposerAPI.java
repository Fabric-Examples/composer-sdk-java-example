/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer;


import org.apache.commons.lang3.StringUtils;
import org.hyperledger.composer.query.SelectQuery;
import org.hyperledger.composer.system.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hyperledger.composer.ComposerException.INVALID_INPUT_ERROR;
import static org.hyperledger.composer.ComposerException.INVALID_REQUEST_CODE;


public class ComposerAPI {
	protected Engine engine;

	public ComposerAPI(Engine engine) {
		this.engine = engine;
	}

	/**
	 * Submit a transaction object into Composer
	 *
	 * @param transaction
	 * @throws ComposerException
	 */
	public String submitTransaction(Object transaction) throws ComposerException {
		String transactionJson = ResourceSerializer.toJSONString(transaction);
		try {
			return engine.submitTransaction(transactionJson);
		} catch (Throwable e) {
			if (e instanceof ComposerException) {
				throw (ComposerException) e;
			}
			throw new ComposerException(INVALID_REQUEST_CODE, "failed to submit transaction: " + transactionJson, e);
		}
	}

	/**
	 * Create a participant in Composer
	 *
	 * @param participant
	 * @throws ComposerException
	 */
	@SuppressWarnings("unchecked")
	public <T> void createParticipant(T participant) throws ComposerException {
		if (participant == null) return;
		createParticipants(Collections.singleton(participant));
	}

	/**
	 * Create multiple participants in Composer by batch
	 *
	 * @param participants
	 * @throws ComposerException
	 */
	@SuppressWarnings("unchecked")
	public <T> void createParticipants(Collection<T> participants) throws ComposerException {
		if (participants == null || participants.isEmpty()) return;
		AddParticipant transaction = new AddParticipant();
		transaction.targetRegistry = ResourceSerializer.fromID(participants.iterator().next().getClass().getName(), ParticipantRegistry.class);
		transaction.resources = participants.toArray();
		submitTransaction(transaction);
	}

	/**
	 * Remove a participant from Composer
	 *
	 * @param participantId
	 * @throws ComposerException
	 */
	public <T> void removeParticipant(Class<T> participantClass, String participantId) throws ComposerException {
		if (StringUtils.isEmpty(participantId)) return;
		removeParticipants(participantClass, Collections.singleton(participantId));
	}

	/**
	 * Remove multiple participants from Composer by batch
	 *
	 * @param participantIds The Ids of all the participants to be removed
	 * @throws ComposerException
	 */
	public <T> void removeParticipants(Class<T> participantClass, Collection<String> participantIds) throws ComposerException {
		if (participantIds == null || participantIds.isEmpty()) return;
		if (participantClass == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "Cannot remove participants with null asset class");
		}
		RemoveParticipant transaction = new RemoveParticipant();
		transaction.targetRegistry = ResourceSerializer.fromID(participantClass.getName(), ParticipantRegistry.class);
		transaction.resources = new Object[0];
		transaction.resourceIds = participantIds.toArray(new String[participantIds.size()]);
		submitTransaction(transaction);
	}

	/**
	 * Update one participant in Composer
	 *
	 * @param participant
	 * @throws ComposerException
	 */
	@SuppressWarnings("unchecked")
	public <T> void updateParticipant(T participant) throws ComposerException {
		if (participant == null) return;
		updateParticipants(Collections.singleton(participant));
	}

	/**
	 * Update multiple participants in Composer in batch
	 *
	 * @param participants
	 * @throws ComposerException
	 */
	@SuppressWarnings("unchecked")
	public <T> void updateParticipants(Collection<T> participants) throws ComposerException {
		if (participants == null || participants.isEmpty()) return;
		UpdateParticipant participant = new UpdateParticipant();
		participant.targetRegistry = ResourceSerializer.fromID(participants.iterator().next().getClass().getName(), ParticipantRegistry.class);
		participant.resources = participants.toArray();
		submitTransaction(participant);
	}

	/**
	 * Get one participant from Composer by ID
	 *
	 * @param participantId
	 * @return User    The user object
	 * @throws ComposerException
	 */
	public <T> T getParticipant(Class<T> participantClass, String participantId) throws ComposerException {
		if (participantClass == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "Cannot get participant of null class");
		}
		return ResourceSerializer.fromJSON(engine.getResourceInRegistry(
				RegistryType.Participant.name(), participantClass.getName(), participantId), participantClass);
	}

	/**
	 * Create an asset in Composer<br>
	 * If the ID of the given asset is occupied, a ComposerException will be thrown
	 *
	 * @param asset The asset object
	 * @throws ComposerException
	 */
	@SuppressWarnings("unchecked")
	public <T> void createAsset(T asset) throws ComposerException {
		if (asset == null) return;
		createAssets(Collections.singleton(asset));
	}

	/**
	 * Create assets in Composer in batch<br>
	 * When any of the IDs of the given assets is occupied in Composer, the whole creation will fail, no asset would be created and a ComposerException will be thrown
	 *
	 * @param assets The collection that holds all the assets to be created
	 * @throws ComposerException
	 */
	@SuppressWarnings("unchecked")
	public <T> void createAssets(Collection<T> assets) throws ComposerException {
		if (assets == null || assets.isEmpty()) return;
		AddAsset transaction = new AddAsset();
		transaction.targetRegistry = ResourceSerializer.fromID(assets.iterator().next().getClass().getName(), AssetRegistry.class);
		transaction.resources = assets.toArray();
		submitTransaction(transaction);
	}

	/**
	 * Remove an asset in Composer
	 *
	 * @param assetClass
	 * @param id
	 * @throws ComposerException
	 */
	public <T> void removeAsset(Class<T> assetClass, String id) throws ComposerException {
		if (assetClass == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "Cannot remove asset with null asset class");
		}
		removeAssets(assetClass, Collections.singleton(id));
	}

	/**
	 * Remove multiple assets in Composer. Assets are specified by their ID
	 *
	 * @param assetClass
	 * @param assetIds
	 * @throws ComposerException
	 */
	public <T> void removeAssets(Class<T> assetClass, Collection<String> assetIds) throws ComposerException {
		if (assetIds == null || assetIds.isEmpty()) return;
		if (assetClass == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "Cannot remove assets with null asset class");
		}
		RemoveAsset transaction = new RemoveAsset();
		transaction.targetRegistry = ResourceSerializer.fromID(assetClass.getName(), AssetRegistry.class);
		transaction.resources = new Object[0];
		transaction.resourceIds = assetIds.toArray(new String[assetIds.size()]);
		submitTransaction(transaction);
	}

	/**
	 * Update an asset in Composer
	 *
	 * @param asset
	 * @throws ComposerException
	 */

	@SuppressWarnings("unchecked")
	public <T> void updateAsset(T asset) throws ComposerException {
		if (asset == null) return;
		updateAssets(Collections.singleton(asset));
	}

	/**
	 * Update multiple assets in Composer by batch
	 *
	 * @param assets
	 * @throws ComposerException
	 */
	@SuppressWarnings("unchecked")
	public <T> void updateAssets(Collection<T> assets) throws ComposerException {
		if (assets == null || assets.isEmpty()) return;
		UpdateAsset transaction = new UpdateAsset();
		transaction.targetRegistry = ResourceSerializer.fromID(assets.iterator().next().getClass().getName(), AssetRegistry.class);
		transaction.resources = assets.toArray();
		submitTransaction(transaction);
	}

	/**
	 * Find one asset from Composer by ID
	 *
	 * @param assetClass
	 * @param assetId
	 * @return
	 * @throws ComposerException
	 */
	@SuppressWarnings("unchecked")
	public <T> T getAsset(Class<T> assetClass, String assetId) throws ComposerException {
		if (assetClass == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "Cannot get asset of null class");
		}
		return ResourceSerializer.fromJSON(engine.getResourceInRegistry(
				RegistryType.Asset.name(), assetClass.getName(), assetId), assetClass);
	}

	public SelectQuery select() {
		return new SelectQuery(this);
	}

	public <T> List<T> executeQuery(Class<T> clazz, String type, String query, String parameters) throws ComposerException {
		try {
			String result = engine.executeQuery(type, query, parameters);
			return ResourceSerializer.arrayFromJSON(result, clazz);
		} catch (IllegalArgumentException e) {
			throw new ComposerException(INVALID_INPUT_ERROR, "parse executeQuery result error", e);
		}
	}

	public enum RegistryType {
		Participant, Asset, Transaction
	}
}
