/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.ResourceSerializer;
import org.hyperledger.composer.client.*;
import org.hyperledger.composer.system.Event;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.RegistrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hyperledger.composer.ComposerException.*;

public class FabricConnector implements ComposerConnector {
	private ChaincodeID ccId;
	private ConnectionOptions connectOptions;
	private HFClient client;
	private HFCAClient caClient;
	private Wallet<ComposerUser> wallet;
	Set<String> eventsHandles;
	Channel channel;
	SecurityContext context;

	private Logger logger = LoggerFactory.getLogger(FabricConnector.class);

	FabricConnector() {
	}

	FabricConnector(ConnectionOptions connectOptions, HFClient client, HFCAClient caClient, Wallet<ComposerUser> wallet)
			throws ComposerException {
		this();
		if (client == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "client not specified");
		}
		if (caClient == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "caClient not specified");
		}
		this.wallet = wallet;
		// FIXME: setVersion().setPath().build();
		this.ccId = ChaincodeID.newBuilder().setName(connectOptions.chaincodeId()).build();
		this.connectOptions = connectOptions;
		this.client = client;
		this.caClient = caClient;
		this.eventsHandles = new HashSet<>();
	}

	private void initChannel() throws InvalidArgumentException, TransactionException {
		if (channel != null) {
			return;
		}
		List<Orderer> orderers = new LinkedList<>();
		for (Host orderer : connectOptions.orderers()) {
			orderers.add(client.newOrderer(orderer.name, orderer.url, orderer.properties));
		}

		channel = client.newChannel(connectOptions.channel());

		for (Orderer orderer : orderers) {
			logger.debug("Adding orderer URL {}", orderer);
			channel.addOrderer(orderer);
		}

		for (Host peer : connectOptions.peers()) {
			logger.debug("Adding peer URL {}", peer);
			// TODO: check if the peer belongs to the channel
			channel.addPeer(client.newPeer(peer.name, peer.url, peer.properties));
		}

		for (Host event : connectOptions.events()) {
			logger.debug("Setting event hub URL {}", event);
			EventHub eventHub = client.newEventHub(event.name, event.url, event.properties);
			channel.addEventHub(eventHub);
		}
		channel.setTransactionWaitTime(connectOptions.invokeWaitMillis());
		// TODO: check if the chaincode is installed on the channel

		logger.debug("Initialize channel with user context");
		channel.initialize();
	}

	@Override
	public void disconnect() {
		if (channel != null) {
			for(Iterator<String> handles = eventsHandles.iterator(); handles.hasNext(); handles.remove()) {
				try {
					channel.unRegisterChaincodeEventListener(handles.next());
				} catch (InvalidArgumentException e) {
					logger.warn("fail to unRegisterChaincodeEventListener", e);
				}
			}
		}
		// TODO no corresponding apis
	}

	@Override
	public void enroll(ComposerIdentity composerIdentity, boolean ignoreExisting) throws ComposerException {
		if (StringUtils.isEmpty(composerIdentity.userID())) {
			throw new ComposerException(INVALID_INPUT_ERROR, "enrollmentID not specified");
		}

		if (StringUtils.isEmpty(composerIdentity.secret())) {
			throw new ComposerException(INVALID_INPUT_ERROR, "enrollmentSecret not specified");
		}

		FabricUser user = getUserContext(composerIdentity.getId());
		boolean update = false;
		if (user != null) {
			if (!ignoreExisting && FabricUser.isEnrolled(user)) {
				throw new ComposerException(INVALID_INPUT_ERROR,
						String.format("%s loaded from persistence and has already enrolled", user));
			}
			update = true;
		}

		if (!FabricUser.isEnrolled(user)) {
			logger.debug("Submitting enrollment request");
			Enrollment enrollment;
			try {
				enrollment = caClient.enroll(composerIdentity.userID(), composerIdentity.secret());
			} catch (Exception e) {
				throw new ComposerException(FABRIC_SDK_ERROR, e.getMessage(), e.getCause());
			}
			logger.debug("Successfully enrolled, creating user object");
			ComposerUser composerUser = new ComposerUser(composerIdentity.userID(), connectOptions.mspId(),
					composerIdentity.affiliation(), enrollment.getKey(), enrollment.getCert());
			user = new FabricUser(composerUser);
			logger.debug("Persisting user context into key value store");
			try {
				client.setUserContext(user);
			} catch (InvalidArgumentException e) {
				throw new ComposerException(FABRIC_SDK_ERROR, e.getMessage(), e.getCause());
			}
			if (update) {
				wallet.update(composerUser);
			} else {
				wallet.add(composerUser);
			}
		}
	}

	@Override
	public void _login(ComposerIdentity composerIdentity) throws ComposerException {
		if (composerIdentity == null) {
			throw new ComposerException(INVALID_INPUT_ERROR, "composerIdentity not specified");
		}
		if (StringUtils.isEmpty(composerIdentity.userID())) {
			throw new ComposerException(INVALID_INPUT_ERROR, "enrollmentID not specified");
		}
		FabricUser user;
		user = getUserContext(composerIdentity.getId());

		if (FabricUser.isEnrolled(user)) {
			logger.debug("{} loaded from persistence and has already enrolled", user);
		} else {
			throw new ComposerException(INVALID_INPUT_ERROR,
					String.format("Invalid userId: %s, not existed or not enrolled.", composerIdentity.userID()));
		}
		logger.debug("Creating new security context");
		this.context = new SecurityContext(user);
		try {
			initChannel();
		} catch (InvalidArgumentException e) {
			throw new ComposerException(INVALID_INPUT_ERROR, e.getMessage(), e.getCause());
		} catch (TransactionException e) {
			throw new ComposerException(FABRIC_SDK_ERROR, e.getMessage(), e.getCause());
		}
	}

	private FabricUser getUserContext(String id) throws ComposerException {
		FabricUser user = (FabricUser) client.getUserContext();
		if (user != null)
			return user;
		ComposerUser composerUser = wallet.get(id);
		if (composerUser == null) {
			logger.debug("Requested user '{}' not loaded from the state store on this Client instance: name - {}", id,
					id);
			return null;
		}
		user = new FabricUser(composerUser);

		logger.debug("Requested user '{}' loaded successfully from the state store on this Client instance: name - {}",
				id, id);
		try {
			client.setUserContext(user);
		} catch (InvalidArgumentException e) {
			throw new ComposerException(INVALID_INPUT_ERROR, e.getMessage(), e.getCause());
		}
		return user;
	}

	@Override
	public String _queryChaincode(String functionName, String... args) throws ComposerException {
		checkSecurityContext();

		QueryByChaincodeRequest request = client.newQueryProposalRequest();
		request.setChaincodeID(ccId);
		request.setArgs(args);
		request.setFcn(functionName);
		request.setProposalWaitTime(connectOptions.invokeWaitMillis());

		Collection<ProposalResponse> responses;
		try {
			long start = System.currentTimeMillis();
			responses = channel.queryByChaincode(request, randomPeer());
			logger.debug("Received {} results(s) from invoking the chaincode", responses.size());

			ValidatedResponse result = validateResponse(responses);
			logger.debug("Query takes {}s", (System.currentTimeMillis() - start) / 1000.0, result.needCommit);
			return result.response;
		} catch (Exception e) {
			return handleProposalException(e.getMessage());
		}
	}

	private Collection<Peer> randomPeer() {
		Collection<Peer> peers = channel.getPeers();
		if (peers.isEmpty()) {
			throw new IllegalArgumentException("no peer specified");
		}
		int peerSize = peers.size();
		return Collections.singleton(peers.toArray(new Peer[peerSize])[new Random().nextInt(peerSize)]);
	}

	@Override
	public String _invokeChaincode(String functionName, Map<String, byte[]> transientMap, String... args) throws ComposerException {
		checkSecurityContext();

		TransactionProposalRequest request = client.newTransactionProposalRequest();
		request.setChaincodeID(ccId);
		request.setArgs(args);
		request.setFcn(functionName);
		request.setProposalWaitTime(connectOptions.invokeWaitMillis());
		if (transientMap != null) {
			try {
				request.setTransientMap(transientMap);
			} catch (InvalidArgumentException e) {
				throw new ComposerException(INVALID_INPUT_ERROR, e.getMessage());
			}
		}
		try {
			long start = System.currentTimeMillis();
			Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(request, randomPeer());
			logger.debug("Received {} results(s) from invoking the chaincode", proposalResponses.size());

			ValidatedResponse result = validateResponse(proposalResponses);
			logger.debug("Endorsing takes {}s, need to commit {}", (System.currentTimeMillis() - start) / 1000.0, result.needCommit);

			if (result.needCommit) {
				start = System.currentTimeMillis();

				BlockEvent.TransactionEvent response = channel.sendTransaction(proposalResponses, channel.getOrderers())
						.get(connectOptions.invokeWaitMillis(), TimeUnit.MILLISECONDS);
				logger.trace("Received response from orderer: {}", response);
				logger.debug("Committing takes {}s", (System.currentTimeMillis() - start) / 1000.0);
			}
			return result.response;
		} catch (InterruptedException | TimeoutException e) {
			throw new ComposerException(INTERNAL_ERROR_CODE, "Failed to receive commit notification for transaction within the timeout period", e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof TransactionEventException) {
				BlockEvent.TransactionEvent te = ((TransactionEventException) cause).getTransactionEvent();
				if (te != null) {
					throw new ComposerException("Transaction(" + te.getTransactionID() + ") failed: " + cause.getMessage());
				}
			}
			throw new ComposerException("sendTransaction failed " + cause.getMessage());
		} catch (ProposalException | InvalidArgumentException e) {
			return handleProposalException(e.getMessage());
		}
	}

	@Override
	public void on(ComposerEventListener listener) throws ComposerException {
		try {
			this.channel.registerChaincodeEventListener(Pattern.compile(".*"), Pattern.compile("composer"), (handle, blockEvent, chaincodeEvent) -> {
				Event[] events = ResourceSerializer.fromJSON(new String(chaincodeEvent.getPayload()), Event[].class);
				listener.onEvents(events);
			});
		} catch (InvalidArgumentException e) {
			throw new ComposerException(INVALID_INPUT_ERROR, e.getMessage());
		}
	}

	static String handleProposalException(String message) throws ComposerException {
		int start = message.indexOf("message: "), end;
		if (start >= 0) {
			end = message.lastIndexOf(')');
			message = message.substring(start + "message: ".length(), end);
		} else {
			start = message.indexOf("description=");
			if (start >= 0) {
				end = message.lastIndexOf(", cause=");
				message = message.substring(start + "description=".length(), end);
			}
		}
		throw new ComposerException(new RuntimeException(message));
	}

	static class ValidatedResponse {
		boolean needCommit;
		String response;

		ValidatedResponse() {
		}

		ValidatedResponse(boolean needCommit, String response) {
			this();
			this.needCommit = needCommit;
			this.response = response;
		}
	}

	ValidatedResponse validateResponse(Collection<ProposalResponse> responses) throws ComposerException {
		if (responses == null || responses.isEmpty()) {
			throw new ComposerException(INVALID_INPUT_ERROR, "No results were returned from the request");
		}

		ProposalResponse response = responses.iterator().next();
		if (!response.isVerified() || response.getStatus() != ChaincodeResponse.Status.SUCCESS) {
			handleProposalException(response.getMessage());
		}
		ValidatedResponse result = new ValidatedResponse();
		try {
			for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfos : response.getChaincodeActionResponseReadWriteSetInfo().getNsRwsetInfos()) {
				KvRwset.KVRWSet rwset = nsRwsetInfos.getRwset();
				logger.debug("namespace:{}, read set:{}, write set:{}", nsRwsetInfos.getNamespace(), rwset.getReadsCount(), rwset.getWritesCount());
				result.needCommit = result.needCommit || rwset.getWritesCount() > 0;
			}
		} catch (Exception e) {
			logger.warn("error parsing rwset info", e);
		}

		try {
			result.response = new String(response.getChaincodeActionResponsePayload());
		} catch (InvalidArgumentException e) {
			throw new ComposerException(INVALID_INPUT_ERROR, e.getMessage());
		}
		logger.trace("Receive {} from peer {}", result, response.getPeer() == null ? "unknown" : response.getPeer().getName());
		return result;
	}

	private void checkSecurityContext() throws ComposerException {
		if (context == null || context.user() == null) {
			throw new ComposerException(INVALID_REQUEST_CODE, "Not logged in yet");
		}
	}

	static RegistrationRequest toRegistrationRequest(ComposerIdentity identity) throws ComposerException {
		RegistrationRequest result;
		try {
			result = new RegistrationRequest(identity.userID(), identity.affiliation());
			result.setMaxEnrollments(identity.maxEnrollments());
			result.setSecret(identity.secret());
			result.setType(identity.type());
			if (identity.issuer()) {
				// Everyone we create can register clients.
				identity.addAttr("hf.RegisterRoles", "client");
				// Everyone we create can register clients that can register
				// clients.
				// Don't think this is needed anymore
				// addAttr("hf.RegisterDelegateRoles", "client");
			}
			for (Pair<String, String> attr : identity.attrs()) {
				Attribute a = new Attribute(attr.getLeft(), attr.getRight());
				result.addAttribute(a);
			}
		} catch (Exception e) {
			throw new ComposerException(INVALID_INPUT_ERROR, e.getMessage());
		}
		return result;
	}

	@Override
	public String register(ComposerIdentity request) throws ComposerException {
		checkSecurityContext();
		try {
			return caClient.register(toRegistrationRequest(request), context.user());
		} catch (org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException e) {
			throw new ComposerException(INVALID_INPUT_ERROR, e.getMessage());
		} catch (RegistrationException e) {
			throw new ComposerException(FABRIC_SDK_ERROR, e.getMessage(), e.getCause());
		}
	}

}
