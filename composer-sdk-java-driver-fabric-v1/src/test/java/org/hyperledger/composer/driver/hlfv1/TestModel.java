/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.composer.annotation.*;
import org.hyperledger.composer.client.ComposerUser;
import org.hyperledger.composer.client.Wallet;
import org.hyperledger.composer.system.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Asset
class SampleAsset {
	@DataField(primary = true)
	private String assetId;

	@DataField
	private String value;

	@Pointer
	private User owner;

	public SampleAsset assetId(String assetId) {
		this.assetId = assetId;
		return this;
	}

	public SampleAsset value(String value) {
		this.value = value;
		return this;
	}

	public SampleAsset owner(User owner) {
		this.owner = owner;
		return this;
	}

	public String value() {
		return value;
	}

	public User owner() {
		return owner;
	}

	@Override
	public String toString() {
		return "SampleAsset{" +
				"assetId='" + assetId + '\'' +
				", value='" + value + '\'' +
				", owner='" + owner + '\'' +
				'}';
	}

	public String assetId() {
		return assetId;
	}

	public String toJson() {
		return "{'assetId':'" + assetId + "', 'value':'" + value + "', " +
				"'owner': {'name': '" + owner.name() + "', 'role': '" + owner.role() + "'}}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SampleAsset that = (SampleAsset) o;

		return (assetId != null ? assetId.equals(that.assetId) : that.assetId == null)
				&& (value != null ? value.equals(that.value) : that.value == null)
				&& (owner != null ? owner.equals(that.owner) : that.owner == null);
	}

	@Override
	public int hashCode() {
		int result = assetId != null ? assetId.hashCode() : 0;
		result = 31 * result + (value != null ? value.hashCode() : 0);
		result = 31 * result + (owner != null ? owner.hashCode() : 0);
		return result;
	}
}

@Participant
class User {
	@DataField(primary = true)
	private String id;

	@DataField
	private String name;

	@DataField
	private String role;

	public String id() {
		return id;
	}

	public User id(String id) {
		this.id = id;
		return this;
	}

	public String name() {
		return name;
	}

	public User name(String name) {
		this.name = name;
		return this;
	}

	public String role() {
		return role;
	}

	public User role(String role) {
		this.role = role;
		return this;
	}

	@Override
	public String toString() {
		return "User{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", role='" + role + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		User user = (User) o;

		if (id != null ? !id.equals(user.id) : user.id != null) return false;
		if (name != null ? !name.equals(user.name) : user.name != null) return false;
		return role != null ? role.equals(user.role) : user.role == null;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (role != null ? role.hashCode() : 0);
		return result;
	}
}

@Transaction
class SampleAssetUpdateTransaction {
	@DataField
	private SampleAsset asset;

	@DataField
	private String newValue;

	public SampleAssetUpdateTransaction asset(SampleAsset asset) {
		this.asset = asset;
		return this;
	}

	public SampleAssetUpdateTransaction newValue(String newValue) {
		this.newValue = newValue;
		return this;
	}

	public SampleAsset asset() {
		return this.asset;
	}

	public String newValue() {
		return newValue;
	}

	@Override
	public String toString() {
		return "SampleAssetUpdateTransaction{" +
				"asset=" + asset +
				", newValue='" + newValue + '\'' +
				'}';
	}
}

class TestWallet implements Wallet<ComposerUser> {

	private final Map<String, ComposerUser> map = new HashMap<>();
	private final Logger logger = LoggerFactory.getLogger(TestWallet.class);

	@Override
	public List<ComposerUser> list() {
		List<ComposerUser> result = new LinkedList<>();
		result.addAll(map.values());
		return result;
	}

	@Override
	public boolean contains(String id) {
		return map.containsKey(id);
	}

	@Override
	public ComposerUser get(String id) {
		return map.get(id);
	}

	@Override
	public ComposerUser add(ComposerUser value) {
		ComposerUser put = map.put(value.getId(), value);
		log();
		return put;
	}

	@Override
	public ComposerUser update(ComposerUser value) {
		map.put(value.getId(), value);
		log();
		return value;
	}

	@Override
	public ComposerUser remove(String id) {
		ComposerUser remove = map.remove(id);
		log();
		return remove;
	}

	private void log() {
		logger.info("wallet: {}", map);
	}
}

class ConnectionProfile {
	private static String connectionString;

	static {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		try {
			connectionString = mapper.writeValueAsString(new ConnectionOptions()
					.addOrderer("orderer", "grpc://localhost:7050")
					.addPeer("peer1", "grpc://localhost:7051")
					.addEventHub("peer1", "grpc://localhost:7053")
					.ca("http://localhost:7054")
					.channel("yzhchannel")
					.chaincodeId("myfoo")
					.mspId("YzhMSP")
					.invokeWaitMillis(6000 * 1000));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	static String toConnectionString() {
		return connectionString;
	}
}

@org.hyperledger.composer.annotation.Event
class TestEvent extends Event {
	@DataField
	String message;

	public TestEvent() {
	}

	TestEvent(String id, int date, String message) {
		this.eventId = id;
		this.timestamp = new Date(date);
		this.message = message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TestEvent event = (TestEvent) o;

		if (message != null ? !message.equals(event.message) : event.message != null) return false;
		if (eventId != null ? !eventId.equals(event.eventId) : event.eventId != null) return false;
		return timestamp != null ? timestamp.equals(event.timestamp) : event.timestamp == null;
	}

	@Override
	public int hashCode() {
		int result = message != null ? message.hashCode() : 0;
		result = 31 * result + (eventId != null ? eventId.hashCode() : 0);
		result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
		return result;
	}
}