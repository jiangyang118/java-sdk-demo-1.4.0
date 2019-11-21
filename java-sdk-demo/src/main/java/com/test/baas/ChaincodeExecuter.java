/*
 *  Copyright 2018 Aliyun.com All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.test.baas;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;

public class ChaincodeExecuter {
	private static final Log logger = LogFactory.getLog(ChaincodeExecuter.class);

	private String chaincodeName;
	private String version;
	private ChaincodeID ccId;
	// waitTime can be adjusted to avoid timeout for connection to external network
	private long waitTime = 10000;

	public ChaincodeExecuter(String chaincodeName, String version) {
		this.chaincodeName = chaincodeName;
		this.version = version;

		ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(chaincodeName).setVersion(version);
		ccId = chaincodeIDBuilder.build();
	}

	public String getChaincodeName() {
		return chaincodeName;
	}

	public void setChaincodeName(String chaincodeName) {
		this.chaincodeName = chaincodeName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public long getWaitTime() {
		return waitTime;
	}

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}

	public String queryTransaction(HFClient client, Channel channel, String func, String... args)
			throws InvalidArgumentException, ProposalException, UnsupportedEncodingException, ServiceDiscoveryException,
			InterruptedException, ExecutionException, TimeoutException {
		QueryByChaincodeRequest query = client.newQueryProposalRequest();
		query.setChaincodeID(ccId);
		query.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);

		query.setFcn(func);
		query.setArgs(args);
		query.setProposalWaitTime(waitTime);
		Map<String, byte[]> tm2 = new HashMap<>();
		tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
		tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
		query.setTransientMap(tm2);

		Collection<ProposalResponse> resps = channel.queryByChaincode(query);
		String payload = null;
		for (ProposalResponse proposalResponse : resps) {
			if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
				logger.error("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: "
						+ proposalResponse.getStatus() + ". Messages: " + proposalResponse.getMessage()
						+ ". Was verified : " + proposalResponse.isVerified());
			} else {
				payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
				logger.info(String.format("Query payload from peer {%s } returned {%s }",
						proposalResponse.getPeer().getName(), payload));
				break;
			}
		}
		if (null == payload) {
			return "{}";
		}
		return payload;
	}

	public Boolean executeTransaction(HFClient client, Channel channel, String func, String... args)
			throws InvalidArgumentException, ProposalException, UnsupportedEncodingException, ServiceDiscoveryException,
			InterruptedException, ExecutionException, TimeoutException {
		TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
		transactionProposalRequest.setChaincodeID(ccId);
		transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);

		transactionProposalRequest.setFcn(func);
		transactionProposalRequest.setArgs(args);
		transactionProposalRequest.setProposalWaitTime(waitTime);
		Map<String, byte[]> tm2 = new HashMap<>();
		tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
		tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
		tm2.put("result", ":)".getBytes(UTF_8)); /// This should be returned see chaincode.
		transactionProposalRequest.setTransientMap(tm2);

		List<ProposalResponse> successful = new LinkedList<ProposalResponse>();
		List<ProposalResponse> failed = new LinkedList<ProposalResponse>();

		Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest);

		for (ProposalResponse response : transactionPropResp) {

			if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
				String payload = new String(response.getChaincodeActionResponsePayload());
				logger.info(String.format("[√] Got success response from peer %s => payload: %s",
						response.getPeer().getName(), payload));
				successful.add(response);
			} else {
				String status = response.getStatus().toString();
				String msg = response.getMessage();
				logger.warn(String.format("[×] Got failed response from peer %s => %s: %s ",
						response.getPeer().getName(), status, msg));
				failed.add(response);
			}
		}
		// Check that all the proposals are consistent with each other. We should have
		// only one set
		// where all the proposals above are consistent.
		Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils
				.getProposalConsistencySets(transactionPropResp);
		if (proposalConsistencySets.size() != 1) {
			logger.warn(String.format("Expected only one set of consistent proposal responses but got { %s}: { %s}"
					+ proposalConsistencySets.size(), args));
			return false;
		}

		if (failed.size() > 0) {
			ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
			logger.warn(
					String.format("Not enough endorsers for { %s}: { %s}. endorser error: { %s}, Was verified: { %s}",
							args, failed.size(), firstTransactionProposalResponse.getMessage(),
							firstTransactionProposalResponse.isVerified()));
			return false;
		}

		logger.info("Sending transaction to orderers...");
		channel.sendTransaction(successful).thenApply(transactionEvent -> {
			logger.info("Orderer response: txid" + transactionEvent.getTransactionID());
			logger.info("Orderer response: block number: " + transactionEvent.getBlockEvent().getBlockNumber());
			return true;
		}).exceptionally(e -> {
			logger.error("Orderer exception happened: ", e);
			return false;
		}).get(waitTime, TimeUnit.SECONDS);
		return true;
	}
}
