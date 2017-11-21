/*
 * Copyright IBM Corp. 2017 All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.composer.driver.hlfv1;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.hyperledger.composer.ComposerException;
import org.hyperledger.composer.ResourceSerializer;
import org.hyperledger.composer.client.ComposerEventListener;
import org.hyperledger.composer.client.ComposerIdentity;
import org.hyperledger.composer.client.ComposerUser;
import org.hyperledger.composer.client.Wallet;
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
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.RegistrationException;
import org.mockito.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class FabricConnectorTest {

	@Mock
	HFClient hfClient;

	@Mock
	HFCAClient caClient;

	@Mock
	Wallet<ComposerUser> wallet;

	@Mock
	ComposerIdentity composerIdentity;

	@Mock
	FabricUser fabricUser;

	@Mock
	ComposerUser composerUser;

	@Mock
	Enrollment enrollment;

	@Mock
	PrivateKey privateKey;

	@Mock
	Channel channel;

	@Mock
	SecurityContext context;

	@Mock
	QueryByChaincodeRequest queryRequest;

	@Mock
	TransactionProposalRequest invokeRequest;

	@Mock
	Peer peer;

	@Mock
	Orderer orderer;

	@Mock
	ProposalResponse response;

	@Mock
	TxReadWriteSetInfo rwsetInfo;

	@Mock
	TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo;

	@Mock
	CompletableFuture<BlockEvent.TransactionEvent> future;

	@Mock
	BlockEvent.TransactionEvent te;

	@Mock
	ComposerEventListener listener;

	@Mock
	ChaincodeEvent chaincodeEvent;

	@Captor
	ArgumentCaptor<ChaincodeEventListener> eventListenerCaptor;

	@Spy
	ConnectionOptions connectionOptions = new ConnectionOptions()
			.addOrderer("orderer", "grpc://localhost:7050")
			.addPeer("peer1", "grpc://localhost:7051")
			.addEventHub("peer1", "grpc://localhost:7053")
			.ca("http://localhost:7054")
			.addCAProperties("allowAllHostNames", "true")
			.channel("yzhchannel")
			.chaincodeId("myfoo")
			.mspId("YzhMSP")
			.invokeWaitMillis(6000 * 1000);

	@InjectMocks
	FabricConnector connector;

	@BeforeMethod(alwaysRun = true)
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "caClient not specified")
	public void testConstructorNullCA() throws Exception {
		new FabricConnector(connectionOptions, hfClient, null, wallet);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "client not specified")
	public void testConstructorNullClient() throws Exception {
		new FabricConnector(connectionOptions, null, caClient, wallet);
	}

	@Test
	public void testDisconnect() throws Exception {
		final String handle = "handle";
		when(channel.unRegisterChaincodeEventListener(handle)).thenReturn(true);
		connector.eventsHandles.add(handle);
		connector.channel = channel;
		connector.disconnect();
		verify(channel).unRegisterChaincodeEventListener(handle);
		assertTrue(connector.eventsHandles.isEmpty());
		connector.channel = null;
	}

	@Test
	public void testDisconnectOnNotConnected() throws Exception {
		connector.channel = null;
		connector.disconnect();
		verify(channel, never()).unRegisterChaincodeEventListener(any());
	}

	@Test
	public void testDisconnectWithInvalidArg() throws Exception {
		final String handle = "handle";
		doThrow(new InvalidArgumentException("exception")).when(channel).unRegisterChaincodeEventListener(any());
		connector.eventsHandles.add(handle);
		connector.channel = channel;
		connector.disconnect();
		verify(channel).unRegisterChaincodeEventListener(handle);
		assertTrue(connector.eventsHandles.isEmpty());
		connector.channel = null;
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "enrollmentID not specified")
	public void testEnrollNullUserId() throws Exception {
		when(composerIdentity.userID()).thenReturn(null);
		connector.enroll(composerIdentity, true);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "enrollmentSecret not specified")
	public void testEnrollNullSecret() throws Exception {
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.secret()).thenReturn(null);
		connector.enroll(composerIdentity, true);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = ".* loaded from persistence and has already enrolled")
	public void testEnrollAlreadyEnrolled() throws Exception {
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.secret()).thenReturn("secret");
		when(fabricUser.getEnrollment()).thenReturn(enrollment);
		when(hfClient.getUserContext()).thenReturn(fabricUser);
		connector.enroll(composerIdentity, false);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "msg")
	public void testEnrollCaFail() throws Exception {
		final String userId = "userId", secret = "secret";
		when(composerIdentity.userID()).thenReturn(userId);
		when(composerIdentity.secret()).thenReturn(secret);
		when(hfClient.getUserContext()).thenReturn(fabricUser);
		when(caClient.enroll(userId, secret)).thenThrow(new EnrollmentException("msg"));
		connector.enroll(composerIdentity, true);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "msg")
	public void testEnrollSetUserContextFail() throws Exception {
		final String userId = "userId", secret = "secret", cert = "cert";
		when(composerIdentity.userID()).thenReturn(userId);
		when(composerIdentity.secret()).thenReturn(secret);
		when(hfClient.getUserContext()).thenReturn(fabricUser);
		when(caClient.enroll(userId, secret)).thenReturn(enrollment);
		when(enrollment.getCert()).thenReturn(cert);
		when(enrollment.getKey()).thenReturn(privateKey);
		doThrow(new InvalidArgumentException("msg")).when(hfClient).setUserContext(any());
		connector.enroll(composerIdentity, true);
	}

	@Test
	public void testEnroll() throws Exception {
		final String userId = "userId", secret = "secret", cert = "cert";
		when(composerIdentity.userID()).thenReturn(userId);
		when(composerIdentity.secret()).thenReturn(secret);
		when(hfClient.getUserContext()).thenReturn(fabricUser);
		when(caClient.enroll(userId, secret)).thenReturn(enrollment);
		when(enrollment.getCert()).thenReturn(cert);
		when(enrollment.getKey()).thenReturn(privateKey);
		connector.enroll(composerIdentity, true);
		verify(hfClient).setUserContext(argThat(user -> userId.equals(user.getName()) &&
				cert.equals(user.getEnrollment().getCert()) &&
				privateKey.equals(user.getEnrollment().getKey())));

		verify(wallet).update(argThat(user -> userId.equals(user.getName()) &&
				cert.equals(user.getCert()) &&
				privateKey.equals(user.getPrivateKey())));
	}

	@Test
	public void testEnrollNotExisting() throws Exception {
		final String userId = "userId", secret = "secret", cert = "cert";
		when(composerIdentity.userID()).thenReturn(userId);
		when(composerIdentity.secret()).thenReturn(secret);
		when(hfClient.getUserContext()).thenReturn(null);
		when(caClient.enroll(userId, secret)).thenReturn(enrollment);
		when(enrollment.getCert()).thenReturn(cert);
		when(enrollment.getKey()).thenReturn(privateKey);
		connector.enroll(composerIdentity, false);
		verify(hfClient).setUserContext(argThat(user -> userId.equals(user.getName()) &&
				cert.equals(user.getEnrollment().getCert()) &&
				privateKey.equals(user.getEnrollment().getKey())));

		verify(wallet).add(argThat(user -> userId.equals(user.getName()) &&
				cert.equals(user.getCert()) &&
				privateKey.equals(user.getPrivateKey())));
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "msg")
	public void testEnrollFailToSetContext() throws Exception {
		final String userId = "userId", id = "id", secret = "secret";
		when(composerIdentity.getId()).thenReturn(id);
		when(composerIdentity.userID()).thenReturn(userId);
		when(composerIdentity.secret()).thenReturn(secret);
		when(hfClient.getUserContext()).thenReturn(null);
		when(wallet.get(id)).thenReturn(composerUser);
		doThrow(new InvalidArgumentException("msg")).when(hfClient).setUserContext(any());
		connector.enroll(composerIdentity, true);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "composerIdentity not specified")
	public void test_loginNullComposerIdentity() throws Exception {
		connector._login(null);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "enrollmentID not specified")
	public void test_loginNoUserIdComposerIdentity() throws Exception {
		when(composerIdentity.userID()).thenReturn(null);
		connector._login(composerIdentity);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "Invalid userId: userId, not existed or not enrolled.")
	public void test_loginNotEnrolled() throws Exception {
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.getId()).thenReturn("id");
		when(wallet.get("id")).thenReturn(null);
		connector._login(composerIdentity);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "msg")
	public void test_loginInitChannelInvalidArgumentException() throws Exception {
		connector.channel = null;
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.getId()).thenReturn("id");
		when(wallet.get("id")).thenReturn(composerUser);
		when(composerUser.getCert()).thenReturn("cert");
		when(composerUser.getPrivateKey()).thenReturn(privateKey);
		doThrow(new InvalidArgumentException("msg")).when(hfClient).newChannel("yzhchannel");
		connector._login(composerIdentity);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "msg")
	public void test_loginInitChannelException() throws Exception {
		connector.channel = null;
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.getId()).thenReturn("id");
		when(wallet.get("id")).thenReturn(composerUser);
		when(composerUser.getCert()).thenReturn("cert");
		when(composerUser.getPrivateKey()).thenReturn(privateKey);
		when(hfClient.newChannel("yzhchannel")).thenReturn(channel);
		doThrow(new TransactionException("msg")).when(channel).initialize();
		connector._login(composerIdentity);
	}

	@Test
	public void test_login() throws Exception {
		connector.channel = null;
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.getId()).thenReturn("id");
		when(wallet.get("id")).thenReturn(composerUser);
		when(composerUser.getCert()).thenReturn("cert");
		when(composerUser.getPrivateKey()).thenReturn(privateKey);
		when(hfClient.newChannel("yzhchannel")).thenReturn(channel);
		connector._login(composerIdentity);
		assertEquals(connector.context.user().getEnrollment().getKey(), privateKey);
		assertEquals(connector.context.user().getEnrollment().getCert(), "cert");

		verify(hfClient).newOrderer("orderer", "grpc://localhost:7050", null);
		verify(channel).addOrderer(any());
		verify(hfClient).newPeer("peer1", "grpc://localhost:7051", null);
		verify(channel).addPeer(any());
		verify(hfClient).newEventHub("peer1", "grpc://localhost:7053", null);
		verify(channel).addEventHub(any());
		verify(channel).setTransactionWaitTime(6000 * 1000);
		verify(channel).initialize();
	}

	@Test
	public void test_loginTwice() throws Exception {
		connector.channel = channel;
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.getId()).thenReturn("id");
		when(wallet.get("id")).thenReturn(composerUser);
		when(composerUser.getCert()).thenReturn("cert1");
		when(composerUser.getPrivateKey()).thenReturn(privateKey);
		doThrow(new InvalidArgumentException("msg")).when(hfClient).newChannel("yzhchannel");
		connector._login(composerIdentity);

		assertEquals(connector.context.user().getEnrollment().getKey(), privateKey);
		assertEquals(connector.context.user().getEnrollment().getCert(), "cert1");
		verify(hfClient, never()).newOrderer(anyString(), anyString(), any());
		verify(channel, never()).addOrderer(any());
		verify(hfClient, never()).newPeer(anyString(), anyString(), any());
		verify(channel, never()).addPeer(any());
		verify(hfClient, never()).newEventHub(anyString(), anyString(), any());
		verify(channel, never()).addEventHub(any());
		verify(channel, never()).setTransactionWaitTime(anyInt());
		verify(channel, never()).initialize();
	}


	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "Not logged in yet")
	public void test_queryChaincodeWithNoSecurityContext() throws Exception {
		connector.context = null;
		connector._queryChaincode("func", "arg0");
	}

	@Test
	public void test_queryChaincode() throws Exception {
		final Set<Peer> peers = Collections.singleton(peer);
		final String result = "hello world", func = "func", arg = "arg0";
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newQueryProposalRequest()).thenReturn(queryRequest);
		when(channel.getPeers()).thenReturn(peers);
		when(channel.queryByChaincode(queryRequest, peers)).thenReturn(Collections.singleton(response));

		FabricConnector connector = spy(this.connector);
		doReturn(new FabricConnector.ValidatedResponse(false, result))
				.when(connector).validateResponse(anyCollection());
		connector.context = context;
		connector.channel = channel;
		assertEquals(connector._queryChaincode(func, arg), result);
		verify(queryRequest).setChaincodeID(any());
		verify(queryRequest).setFcn(func);
		verify(queryRequest).setArgs(argThat((ArgumentMatcher<String[]>)
				argument -> argument.length == 1 && arg.equals(argument[0])));
		verify(queryRequest).setProposalWaitTime(6000 * 1000);
		verify(channel).queryByChaincode(queryRequest, peers);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "no peer specified")
	public void test_queryChaincodeWithNoPeer() throws Exception {
		final String func = "func", arg = "arg0";
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newQueryProposalRequest()).thenReturn(queryRequest);
		when(channel.getPeers()).thenReturn(Collections.emptySet());
		connector.context = context;
		connector.channel = channel;
		connector._queryChaincode(func, arg);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "exception")
	public void test_queryChaincodeWithException() throws Exception {
		final Set<Peer> peers = Collections.singleton(peer);
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newQueryProposalRequest()).thenReturn(queryRequest);
		when(channel.getPeers()).thenReturn(peers);
		when(channel.queryByChaincode(queryRequest, peers)).thenReturn(Collections.singleton(response));

		FabricConnector connector = spy(this.connector);
		doThrow(new ComposerException("exception")).when(connector).validateResponse(anyCollection());
		connector.context = context;
		connector.channel = channel;
		connector._queryChaincode("func", "arg0");
	}

	@Test(expectedExceptions = ComposerException.class,
			expectedExceptionsMessageRegExp = "Failed to receive commit notification for transaction within the timeout period")
	public void test_invokeChaincodeWithFutureException() throws Exception {
		final Set<Peer> peers = Collections.singleton(peer);
		final Set<Orderer> orderers = Collections.singleton(orderer);
		final String result = "result", func = "func", arg = "arg0";
		final Set<ProposalResponse> responses = Collections.singleton(response);
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newTransactionProposalRequest()).thenReturn(invokeRequest);
		when(channel.getPeers()).thenReturn(peers);
		when(channel.getOrderers()).thenReturn(orderers);
		when(channel.sendTransactionProposal(invokeRequest, peers)).thenReturn(responses);
		when(channel.sendTransaction(responses, orderers)).thenReturn(future);
		doThrow(new TimeoutException()).when(future).get(anyLong(), any());

		FabricConnector connector = spy(this.connector);
		doReturn(new FabricConnector.ValidatedResponse(true, result))
				.when(connector).validateResponse(anyCollection());
		connector.context = context;
		connector.channel = channel;
		connector._invokeChaincode(func, null, arg);
	}

	@Test(expectedExceptions = ComposerException.class,
			expectedExceptionsMessageRegExp = "msg")
	public void test_invokeChaincodeWithTransactionException() throws Exception {
		final Set<Peer> peers = Collections.singleton(peer);
		final Set<Orderer> orderers = Collections.singleton(orderer);
		final String result = "result", func = "func", arg = "arg0";
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newTransactionProposalRequest()).thenReturn(invokeRequest);
		when(channel.getPeers()).thenReturn(peers);
		when(channel.getOrderers()).thenReturn(orderers);
		doThrow(new ProposalException("msg")).when(channel).sendTransactionProposal(invokeRequest, peers);

		FabricConnector connector = spy(this.connector);
		doReturn(new FabricConnector.ValidatedResponse(true, result))
				.when(connector).validateResponse(anyCollection());
		connector.context = context;
		connector.channel = channel;
		connector._invokeChaincode(func, null, arg);
	}

	@Test(expectedExceptions = ComposerException.class,
			expectedExceptionsMessageRegExp = "sendTransaction failed msg")
	public void test_invokeChaincodeWithExecutionException() throws Exception {
		final Set<Peer> peers = Collections.singleton(peer);
		final Set<Orderer> orderers = Collections.singleton(orderer);
		final String result = "result", func = "func", arg = "arg0", txId = "txId";
		final Set<ProposalResponse> responses = Collections.singleton(response);
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newTransactionProposalRequest()).thenReturn(invokeRequest);
		when(channel.getPeers()).thenReturn(peers);
		when(channel.getOrderers()).thenReturn(orderers);
		when(channel.sendTransactionProposal(invokeRequest, peers)).thenReturn(responses);
		when(channel.sendTransaction(responses, orderers)).thenReturn(future);
		doThrow(new ExecutionException(new RuntimeException("msg"))).when(future).get(anyLong(), any());
		when(te.getTransactionID()).thenReturn(txId);

		FabricConnector connector = spy(this.connector);
		doReturn(new FabricConnector.ValidatedResponse(true, result))
				.when(connector).validateResponse(anyCollection());
		connector.context = context;
		connector.channel = channel;
		connector._invokeChaincode(func, null, arg);
	}

	@Test(expectedExceptions = ComposerException.class,
			expectedExceptionsMessageRegExp = "Transaction\\(txId\\) failed: msg")
	public void test_invokeChaincodeWithTransactionEventException() throws Exception {
		final Set<Peer> peers = Collections.singleton(peer);
		final Set<Orderer> orderers = Collections.singleton(orderer);
		final String result = "result", func = "func", arg = "arg0", txId = "txId";
		final Set<ProposalResponse> responses = Collections.singleton(response);
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newTransactionProposalRequest()).thenReturn(invokeRequest);
		when(channel.getPeers()).thenReturn(peers);
		when(channel.getOrderers()).thenReturn(orderers);
		when(channel.sendTransactionProposal(invokeRequest, peers)).thenReturn(responses);
		when(channel.sendTransaction(responses, orderers)).thenReturn(future);
		doThrow(new ExecutionException(new TransactionEventException("msg", te)))
				.when(future).get(anyLong(), any());
		when(te.getTransactionID()).thenReturn(txId);

		FabricConnector connector = spy(this.connector);
		doReturn(new FabricConnector.ValidatedResponse(true, result))
				.when(connector).validateResponse(anyCollection());
		connector.context = context;
		connector.channel = channel;
		connector._invokeChaincode(func, null, arg);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "exception")
	public void test_invokeChaincodeWithSetTransientMapException() throws Exception {
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newTransactionProposalRequest()).thenReturn(invokeRequest);
		doThrow(new InvalidArgumentException("exception")).when(invokeRequest).setTransientMap(anyMap());
		connector._invokeChaincode("func", new HashMap<>(), "arg0");
	}

	@Test
	public void test_invokeChaincode() throws Exception {
		final Set<Peer> peers = Collections.singleton(peer);
		final Set<Orderer> orderers = Collections.singleton(orderer);
		final String result = "hello world", func = "func", arg = "arg0";
		final Set<ProposalResponse> responses = Collections.singleton(response);
		final HashMap<String, byte[]> map = new HashMap<>();
		when(context.user()).thenReturn(fabricUser);
		when(hfClient.newTransactionProposalRequest()).thenReturn(invokeRequest);
		when(channel.getPeers()).thenReturn(peers);
		when(channel.getOrderers()).thenReturn(orderers);
		when(channel.sendTransactionProposal(invokeRequest, peers)).thenReturn(responses);
		when(channel.sendTransaction(responses, orderers)).thenReturn(future);
		when(future.get(anyLong(), any())).thenReturn(null);

		FabricConnector connector = spy(this.connector);
		doReturn(new FabricConnector.ValidatedResponse(true, result))
				.when(connector).validateResponse(anyCollection());
		connector.context = context;
		connector.channel = channel;
		assertEquals(connector._invokeChaincode(func, map, arg), result);
		verify(invokeRequest).setChaincodeID(any());
		verify(invokeRequest).setArgs(argThat((ArgumentMatcher<String[]>)
				argument -> argument.length == 1 && arg.equals(argument[0])));
		verify(invokeRequest).setFcn(func);
		verify(invokeRequest).setProposalWaitTime(6000 * 1000);
		verify(invokeRequest).setTransientMap(map);
		verify(channel).sendTransactionProposal(invokeRequest, peers);
		verify(channel).sendTransaction(responses, orderers);
		verify(future).get(anyLong(), any());
	}

	@Test
	public void testOn() throws Exception {
		TestEvent e0 = new TestEvent("id1", 1, "msg1");
		TestEvent e1 = new TestEvent("id2", 2, "msg2");
		when(chaincodeEvent.getPayload()).thenReturn(ResourceSerializer.toJSONString(new Event[]{e0, e1}).getBytes());
		connector.channel = channel;
		connector.on(listener);

		verify(channel).registerChaincodeEventListener(any(), any(), eventListenerCaptor.capture());
		eventListenerCaptor.getValue().received("ccId", null, chaincodeEvent);
		verify(listener).onEvents(argThat(e -> e.length == 2 && e[0].equals(e0) && e[1].equals(e1)));
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "exception")
	public void testOnWithException() throws Exception {
		doThrow(new InvalidArgumentException("exception")).when(channel).registerChaincodeEventListener(any(), any(), any());
		connector.channel = channel;
		connector.on(listener);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "msg")
	public void testHandleProposalExceptionWithPlanDescription() throws Exception {
		FabricConnector.handleProposalException("Sending proposal to peer1 failed because of: gRPC failure=Status{code=UNKNOWN, description=msg, cause=null}");
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "msg")
	public void testHandleProposalExceptionWithMessage() throws Exception {
		FabricConnector.handleProposalException("Sending proposal to peer1 failed because of: gRPC failure=Status{code=UNKNOWN, description=chaincode error (status: 500, message: msg), cause=null}; line: 1, column: 8]");
	}

	@Test
	public void testValidateResponse() throws Exception {
		final String result = "response";
		when(response.isVerified()).thenReturn(true);
		when(response.getStatus()).thenReturn(ChaincodeResponse.Status.SUCCESS);
		when(response.getChaincodeActionResponseReadWriteSetInfo()).thenReturn(rwsetInfo);
		when(rwsetInfo.getNsRwsetInfos()).thenReturn(Collections.singleton(nsRwsetInfo));
		when(nsRwsetInfo.getRwset()).thenReturn(KvRwset.KVRWSet.newBuilder().build());
		when(response.getChaincodeActionResponsePayload()).thenReturn(result.getBytes());

		FabricConnector.ValidatedResponse r = connector.validateResponse(Collections.singleton(this.response));
		assertFalse(r.needCommit);
		assertEquals(r.response, result);

		verify(response).isVerified();
		verify(response).getStatus();
		verify(response).getChaincodeActionResponseReadWriteSetInfo();
		verify(rwsetInfo).getNsRwsetInfos();
		verify(nsRwsetInfo).getRwset();
		verify(response).getChaincodeActionResponsePayload();
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "No results were returned from the request")
	public void testValidateEmptyResponse() throws Exception {
		connector.validateResponse(Collections.emptySet());
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "msg")
	public void testValidateFailedResponse() throws Exception {
		when(response.isVerified()).thenReturn(false);
		when(response.getStatus()).thenReturn(ChaincodeResponse.Status.FAILURE);
		when(response.getMessage()).thenReturn("msg");
		connector.validateResponse(Collections.singleton(this.response));
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "exception")
	public void testValidateResponseWithException() throws Exception {
		when(response.isVerified()).thenReturn(true);
		when(response.getStatus()).thenReturn(ChaincodeResponse.Status.SUCCESS);
		when(response.getChaincodeActionResponseReadWriteSetInfo()).thenThrow(new InvalidArgumentException("ignore"));
		when(response.getChaincodeActionResponsePayload()).thenThrow(new InvalidArgumentException("exception"));

		connector.validateResponse(Collections.singleton(this.response));
	}

	@Test
	public void testToRegistrationRequest() throws Exception {
		String userId = "userId", affiliation = "affiliation", type = "type";
		RegistrationRequest request = FabricConnector.toRegistrationRequest(new ComposerIdentity()
				.userID(userId).affiliation(affiliation)
				.type(type).issuer(true).maxEnrollments(123));
		assertEquals(request.getEnrollmentID(), userId);
		assertEquals(request.getAffiliation(), affiliation);
		assertEquals(request.getType(), type);
		assertEquals(request.getMaxEnrollments(), 123);
		Attribute attr = request.getAttributes().iterator().next();
		assertEquals(attr.getName(), "hf.RegisterRoles");
		assertEquals(attr.getValue(), "client");
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "id may not be null")
	public void testToRegistrationRequestWithException() throws Exception {
		FabricConnector.toRegistrationRequest(new ComposerIdentity());
	}

	@Test
	public void testRegister() throws Exception {
		final String secret = "secret";
		connector.context = context;
		when(context.user()).thenReturn(fabricUser);
		when(caClient.register(any(), any())).thenReturn(secret);
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.affiliation()).thenReturn("affiliation");
		assertEquals(connector.register(composerIdentity), secret);
		verify(caClient).register(any(), any());
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "exception")
	public void testRegisterWithIllegalArgument() throws Exception {
		connector.context = context;
		when(context.user()).thenReturn(fabricUser);
		when(caClient.register(any(), any())).thenThrow(new org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException("exception"));
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.affiliation()).thenReturn("affiliation");
		connector.register(composerIdentity);
	}

	@Test(expectedExceptions = ComposerException.class, expectedExceptionsMessageRegExp = "exception")
	public void testRegisterWithRegistrationException() throws Exception {
		connector.context = context;
		when(context.user()).thenReturn(fabricUser);
		when(caClient.register(any(), any())).thenThrow(new RegistrationException("exception", new Exception()));
		when(composerIdentity.userID()).thenReturn("userId");
		when(composerIdentity.affiliation()).thenReturn("affiliation");
		connector.register(composerIdentity);
	}

}