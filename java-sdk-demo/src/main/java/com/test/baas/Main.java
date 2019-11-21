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

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.NetworkConfig.OrgInfo;
import org.hyperledger.fabric.sdk.NetworkConfig.UserInfo;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InfoException;
import org.yaml.snakeyaml.Yaml;

public class Main {

	private static final Log logger = LogFactory.getLog(Main.class);
	private static String connectionProfilePath;

	private static String channelName = "channel";
	private static String userName = "user";
	private static String secret = "User@1234";
	private static String chaincodeName = "fabcar";
	private static String chaincodeVersion = "1.0";

	public static void main(String[] args) {
		System.err.println(System.getProperty("user.dir"));
		connectionProfilePath = System.getProperty("user.dir") + "/bcs-esunego-channel-sdk-config.yaml";
		File f = new File(connectionProfilePath);
		try {

			NetworkConfig networkConfig = NetworkConfig.fromYamlFile(f);
			// set netty channel builder options to avoid error "gRPC message exceeds
			// maximum size"
			for (String peerName : networkConfig.getPeerNames()) {
				Properties peerProperties = networkConfig.getPeerProperties(peerName);
				if (peerProperties == null) {
					peerProperties = new Properties();
				}
				String orgId = getOrgIdByPeer(networkConfig, peerName);
				peerProperties.setProperty("hostnameOverride", peerName);
				peerProperties.setProperty("clientCertFile", GetTlsCert(connectionProfilePath, orgId));
				peerProperties.setProperty("clientKeyFile", GetTlsKey(connectionProfilePath, orgId));
				peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 100 * 1024 * 1024);
				networkConfig.setPeerProperties(peerName, peerProperties);
			}
			for (String ordererName : networkConfig.getOrdererNames()) {
				Properties ordererProperties = networkConfig.getOrdererProperties(ordererName);
				if (ordererProperties == null) {
					ordererProperties = new Properties();
				}
				ordererProperties.setProperty("hostnameOverride", ordererName);
				ordererProperties.setProperty("clientCertFile", GetTlsCert(connectionProfilePath, "ordererorg"));
				ordererProperties.setProperty("clientKeyFile", GetTlsKey(connectionProfilePath, "ordererorg"));
				ordererProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 100 * 1024 * 1024);
				networkConfig.setOrdererProperties(ordererName, ordererProperties);
			}
			for (String item : networkConfig.getEventHubNames()) {
				Properties p = networkConfig.getEventHubsProperties(item);
				if (p == null) {
					p = new Properties();
				}
				String orgId = getOrgIdByPeer(networkConfig, item);
				p.setProperty("hostnameOverride", item);
				p.setProperty("clientCertFile", GetTlsCert(connectionProfilePath, orgId));
				p.setProperty("clientKeyFile", GetTlsKey(connectionProfilePath, orgId));
				networkConfig.setEventHubProperties(item, p);
			}

			NetworkConfig.CAInfo caInfo = networkConfig.getClientOrganization().getCertificateAuthorities().get(0);
			System.err.println(caInfo);

			/***
			 * 下面获取用户、注册用户、获取管理员用户。目前华为云不支持获取用户、注册用户—— 2019/11/21 14:26 by 姜阳
			 */
//			FabricUser user = getFabricUser(clientOrg, caInfo);
//			FabricUser user = registerFabricUser(clientOrg, caInfo, userName);
			FabricUser user = genFabricUser(networkConfig);
			System.err.println(user);

			HFClient client = HFClient.createNewInstance();
			client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
			client.setUserContext(user);

			Channel channel = client.loadChannelFromConfig(channelName, networkConfig);
			channel.initialize();

			channel.registerBlockListener(blockEvent -> {
				logger.info(String.format("Receive block event (number %s) from %s", blockEvent.getBlockNumber(),
						blockEvent.getPeer()));
			});
			/**
			 * 打印区块链
			 */
//			printChannelInfo(client, channel);
			/**
			 * 执行链码fabcar示例，包含查询和创建以及改变车主changeCarOwner
			 */
			executeChaincode(client, channel);
			logger.info("Shutdown channel.");
			channel.shutdown(true);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("exception", e);
		}
	}

	private static String getOrgIdByPeer(NetworkConfig config, String peerName) {
		for (OrgInfo o : config.getOrganizationInfos()) {
			for (String p : o.getPeerNames()) {
				if (p.equals(peerName)) {
					return o.getName();
				}
			}
		}
		return "";
	}

	private static String GetTlsCert(String configFile, String orgId) {
		String msp = getCryptoPath(configFile, orgId);
		int index = msp.lastIndexOf("msp");
		String ret = msp.substring(0, index) + "tls/server.crt";
		logger.debug("tls cert for " + orgId + ",path:" + ret);
		return ret;
	}

	private static String GetTlsKey(String configFile, String orgId) {
		String msp = getCryptoPath(configFile, orgId);
		int index = msp.lastIndexOf("msp");
		String ret = msp.substring(0, index) + "tls/server.key";
		logger.debug("tls key for " + orgId + ",path:" + ret);
		return ret;
	}

	private static String getCryptoPath(String configFile, String orgId) {
		JsonObject root = null;
		try {
			InputStream stream = new FileInputStream(configFile);
			Yaml yaml = new Yaml();
			Map<String, Object> map = yaml.load(stream);
			root = Json.createObjectBuilder(map).build();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JsonObject orgs = root.getJsonObject("organizations");
		for (Map.Entry o : orgs.entrySet()) {
			if (orgId.equals(o.getKey())) {
				JsonObject v = (JsonObject) o.getValue();
				return v.getString("cryptoPath");
			}
		}
		return "";
	}

	private static void lineBreak() {
		logger.info("=============================================================");
	}

	private static void executeChaincode(HFClient client, Channel channel)
			throws ProposalException, InvalidArgumentException, UnsupportedEncodingException, InterruptedException,
			ExecutionException, TimeoutException, ServiceDiscoveryException {
		lineBreak();
		ChaincodeExecuter executer = new ChaincodeExecuter(chaincodeName, chaincodeVersion);
		executer.queryTransaction(client, channel, "queryAllCars", "");
		executer.executeTransaction(client, channel, "changeCarOwner", "CAR0", "hahaha ");
		executer.executeTransaction(client, channel, "createCar", "CAR11", "AAAAA", "BBBBB", "CCCCC", "DDDDDD");
		executer.queryTransaction(client, channel, "queryCar", "CAR0");
		executer.queryTransaction(client, channel, "queryCar", "CAR11");

	}

	private static void printChannelInfo(HFClient client, Channel channel)
			throws ProposalException, InvalidArgumentException, IOException {
		lineBreak();
		BlockchainInfo channelInfo = channel.queryBlockchainInfo();

		logger.info("Channel height: " + channelInfo.getHeight());
		for (long current = channelInfo.getHeight() - 1; current > -1; --current) {
			BlockInfo returnedBlock = channel.queryBlockByNumber(current);
			final long blockNumber = returnedBlock.getBlockNumber();

			logger.info(String.format("Block #%d has previous hash id: %s", blockNumber,
					Hex.encodeHexString(returnedBlock.getPreviousHash())));
			logger.info(String.format("Block #%d has data hash: %s", blockNumber,
					Hex.encodeHexString(returnedBlock.getDataHash())));
			logger.info(String.format("Block #%d has calculated block hash is %s", blockNumber,
					Hex.encodeHexString(SDKUtils.calculateBlockHash(client, blockNumber,
							returnedBlock.getPreviousHash(), returnedBlock.getDataHash()))));
		}

	}

	private static FabricUser genFabricUser(NetworkConfig networkConfig)
			throws UnsupportedEncodingException, FileNotFoundException, IOException, NoSuchProviderException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		NetworkConfig.OrgInfo clientOrg = networkConfig.getClientOrganization();
		FabricUser user = new FabricUser();
		String msp = clientOrg.getMspId();
		user.setName("user1admin");
		user.setMspId(msp);
		File privateKeyStoreFile = new File(
				"D:/code/huawei/blockchain/javasdkdemo/config/44d64af3f48f5517afbd17ecaa5118abaee59fb7.peer/msp/keystore/10c5b083-e5b2-e3a8-ea7d-efafdc9a3040_sk");
		System.err.println(privateKeyStoreFile);
		File certificateFile = new File(
				"D:/code/huawei/blockchain/javasdkdemo/config/44d64af3f48f5517afbd17ecaa5118abaee59fb7.peer/"
						+ "msp/admincerts/"
						+ "Admin@44d64af3f48f5517afbd17ecaa5118abaee59fb7.peer-44d64af3f48f5517afbd17ecaa5118abaee59fb7.default.svc.cluster.local-cert.pem");

		System.err.println(certificateFile);

		String signedCert = new String(IOUtils.toByteArray(new FileInputStream(certificateFile)), "UTF-8");

		PrivateKey privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(privateKeyStoreFile)));

		final PrivateKey privateKeyFinal = privateKey;
		user.setEnrollment(new Enrollment() {
			@Override
			public PrivateKey getKey() {
				return privateKeyFinal;
			}

			@Override
			public String getCert() {
				return signedCert;
			}
		});
		return user;
	}

	static PrivateKey getPrivateKeyFromBytes(byte[] data)
			throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
		final Reader pemReader = new StringReader(new String(data));

		final PrivateKeyInfo pemPair;
		try (PEMParser pemParser = new PEMParser(pemReader)) {
			pemPair = (PrivateKeyInfo) pemParser.readObject();
		}
		// .setProvider(BouncyCastleProvider.PROVIDER_NAME)
		// 华为云不同于fabric示例，不设置setProvider
		PrivateKey privateKey = new JcaPEMKeyConverter().getPrivateKey(pemPair);

		return privateKey;
	}

	private static FabricUser getFabricUser(NetworkConfig.OrgInfo clientOrg, NetworkConfig.CAInfo caInfo)
			throws MalformedURLException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException,
			InfoException, EnrollmentException {
		HFCAClient hfcaClient = HFCAClient.createNewInstance(caInfo);
		HFCAInfo cainfo = hfcaClient.info();
		lineBreak();
		logger.info("CA name: " + cainfo.getCAName());
		logger.info("CA version: " + cainfo.getVersion());

		// Persistence is not part of SDK.

		logger.info("Going to enroll user: " + userName);
		Enrollment enrollment = hfcaClient.enroll(userName, secret);
		logger.info("Enroll user: " + userName + " successfully.");

		FabricUser user = new FabricUser();
		user.setMspId(clientOrg.getMspId());
		user.setName(userName);
		user.setOrganization(clientOrg.getName());
		user.setEnrollment(enrollment);
		return user;
	}

	private static void out(String format, Object... args) {

		System.err.flush();
		System.out.flush();

		System.out.println(format(format, args));
		System.err.flush();
		System.out.flush();

	}

	private static FabricUser registerFabricUser(NetworkConfig.OrgInfo clientOrg, NetworkConfig.CAInfo caInfo,
			String username) throws Exception {
		HFCAClient hfcaClient = HFCAClient.createNewInstance(caInfo);
		HFCAInfo cainfo = hfcaClient.info();
		lineBreak();
		out("CA name: " + cainfo.getCAName());
		out("CA version: " + cainfo.getVersion());

		out("Going to enroll user: " + username);

		Collection<UserInfo> registrars = caInfo.getRegistrars();
		UserInfo registrar = registrars.iterator().next();
		registrar.setEnrollment(hfcaClient.enroll(registrar.getName(), registrar.getEnrollSecret()));
		RegistrationRequest rr = new RegistrationRequest(username, "org1.department1");
		String secret = hfcaClient.register(rr, registrar);
		System.err.println(secret);

		// Persistence is not part of SDK.
		FabricUser user = new FabricUser();
		user.setMspId(clientOrg.getMspId());
		user.setName(username);
		user.setOrganization(clientOrg.getName());
		user.setEnrollment(hfcaClient.enroll(username, secret));
		out("Enroll user: " + username + " successfully.");
		return user;
	}
}
