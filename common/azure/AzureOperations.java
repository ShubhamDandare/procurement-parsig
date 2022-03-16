package com.kpmg.rcm.sourcing.common.azure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusFailureReason;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpmg.rcm.sourcing.common.config.RCMBeanFactory;
import com.kpmg.rcm.sourcing.common.config.properties.AzureProperties;
import com.kpmg.rcm.sourcing.common.config.properties.ServiceBusProperties;
import com.kpmg.rcm.sourcing.common.util.CommonConstants;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AzureOperations {

	CloudBlobClient cloudBlobClient;

	AzureProperties azureProperties;

	@Value("${filepath.deleteAfterUpload:false}")
	private Boolean deleteAfterUpload;

	private String activeProfile;

	@Autowired
	AzureOperations(AzureProperties azureProperties, @Value("${spring.profiles.active:default}") String activeProfile) {
		this.activeProfile = activeProfile;
		if (!activeProfile.equals(CommonConstants.LOCAL_PROFILE)) {
			this.azureProperties = azureProperties;
			createBlobClient();
		}
	}

	/**
	 * Method to create azure blob client for blob operations
	 */
	private void createBlobClient() {
		try {
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(azureProperties.getConnectionString());
			this.cloudBlobClient = storageAccount.createCloudBlobClient();
		} catch (URISyntaxException e) {
			log.error("Error from uploadData : URISyntaxException " + e.getMessage());
		} catch (InvalidKeyException e) {
			log.error("Error from uploadData : InvalidKeyException " + e.getMessage());
		}

	}

	/**
	 * Method to upload JSON data to Azure Cloud
	 */
	public void uploadData(String filePath, String blobNameReplace) {
		if (!activeProfile.equals(CommonConstants.LOCAL_PROFILE)) {
			Boolean isAzureOperationEnabled = azureProperties.getIsAzureFileUploadEnable();
			log.debug("Azure prop : " + isAzureOperationEnabled);
			if (isAzureOperationEnabled) {
				log.info("File Path to upload: " + filePath);
				getSingleFile(new File(filePath), blobNameReplace);
			} else {
				log.debug("Azure Operation not Enabled");
			}
		}
	}

	/**
	 * Method to get single file from directory and sub-directories
	 *
	 * @param sourceFile
	 *            source location
	 */
	private void getSingleFile(File sourceFile, String blobNameReplace) {
		log.debug("Source File: " + sourceFile);
		log.debug("Blob name Replace: " + blobNameReplace);
		log.debug("sourceFile.isFile(): " + sourceFile.isFile());

		if (sourceFile.exists() && sourceFile.isFile()) {
			try {
				upload(sourceFile, blobNameReplace);
			} catch (Exception e) {
				log.error("Error while uploading file: ", e);
			}
		} else if (!sourceFile.exists()) {
			log.info("Source file is uploaded before " + sourceFile);
		} else {
			// for loop to check if current file is directory or file; if its directory
			// follow same process
			for (File fileOrDir : sourceFile.listFiles()) {
				boolean isFile = fileOrDir.isFile();
				if (isFile) {
					try {
						upload(fileOrDir, blobNameReplace);
					} catch (Exception e) {
						log.error("Error while uploading file: ", e);
					}
				} else {
					getSingleFile(fileOrDir, blobNameReplace);
				}

			}
		}

	}

	/**
	 * Upload specific file (inside directory)
	 *
	 * @param file
	 *            file to upload (windows sample filePath :
	 *            "C:\\Users\\goswami_h\\Downloads\\rcm_test\\ecfr\\11-21-2021\\JSON\\ecfr-title7.json")
	 * @param blobNameReplace
	 *            from the file path part that need to be replaced or should not be
	 *            there on azure (eg. "C:\\Users\\goswami_h\\Downloads\\rcm_test\\)
	 */
	public void upload(File file, String blobNameReplace) {

		try {
			CloudBlobContainer container = cloudBlobClient.getContainerReference(azureProperties.getContainerName());
			log.debug("Container Name : " + container.getName());
			String filePath = file.getAbsolutePath().replace("\\", "/");
			blobNameReplace = blobNameReplace.replace("\\", "/");
			String blobName = filePath.replace(blobNameReplace, "");

			CloudBlockBlob blob = container.getBlockBlobReference(blobName);
			if (blob.exists()) {
				log.info("File [{}] Already exist in storage", blobName);
			}

			// Creating blob and uploading file to it
			log.debug("uploading file: " + blobName);
			blob.uploadFromFile(file.getAbsolutePath());
			// https://sadevrcmprapp1.blob.core.windows.net/sacontainerdevrcmprapp1/fr/2022-01-06/json/us_fr_notices_150cd1fd_cb7f_43fb_9e6c_84224318a43a_2017-06-27.json
			log.info("Uploaded file [{}] to container [{}]", blobName, container.getName());
		} catch (URISyntaxException | StorageException | IOException e) {
			log.error("Error while uploading file: ", e);
		} finally {
			if (file.exists() && deleteAfterUpload && !activeProfile.equals(CommonConstants.LOCAL_PROFILE)) {
				file.delete();
			}
		}

	}

	/**
	 * Method to download file from Azure
	 *
	 * @param filePath
	 *            local file path, where we need to download file (windows sample
	 *            filePath :
	 *            "C:\\Users\\goswami_h\\Downloads\\rcm_test\\ecfr\\11-21-2021\\JSON\\ecfr-title7.json")
	 * @param blobName
	 *            azure blob name from where we need to download file (sample :
	 *            "ecfr/11-21-2021/json/ecfr-title7.json")
	 */
	public void downloadData(String filePath, String blobName) {
		if (!activeProfile.equals(CommonConstants.LOCAL_PROFILE)) {
			try {
				CloudBlobContainer container = cloudBlobClient
						.getContainerReference(azureProperties.getContainerName());
				log.info("Container Name : " + container.getName());
				CloudBlockBlob blob = container.getBlockBlobReference(blobName);
				blob.downloadToFile(filePath);
			} catch (URISyntaxException | StorageException | FileNotFoundException e) {
				log.error("Error from downloadDataFromAzure :  " + e.getMessage());
				log.error("Exception occurred", e);
			} catch (IOException e) {
				log.error("Error from downloadDataFromAzure :  IOException " + e.getMessage());
			}
		} else {
			String localFilePath = blobName;
			Path src = Paths.get(localFilePath);
			Path dest = Paths.get(filePath);
			copyFile(src, dest);
		}
	}

	private static void copyFile(Path src, Path dest) {
		try {
			Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			log.error("Error in copying the file from blob location to temp location", e);
		}
	}

	/**
	 * Method to send message to service bus; message body as azure blob path of
	 * uploaded file
	 * 
	 * @param key
	 *            - pass blob name as key
	 */
	public void sendMessageToServiceBus(String key) {

		// ServiceBusProperties to get service bus connection details
		ServiceBusProperties serviceBusProperties = (ServiceBusProperties) RCMBeanFactory.getApplicationContext()
				.getBean("serviceBusProperties");
		// create a Service Bus Sender client for the queue
		try (ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
				.connectionString(serviceBusProperties.getConnectionString()).sender()
				.queueName(serviceBusProperties.getQueueName()).buildClient()) {
			log.info("sending message to service bus with key name [{}]", key);
			QueueInput queueInput = new QueueInput();
			queueInput.setContainer(azureProperties.getContainerName());
			queueInput.setKey(key);

			String input = new ObjectMapper().writeValueAsString(queueInput);

			ServiceBusMessage message = new ServiceBusMessage(input);
			message.setContentType("application/json");
			message.setMessageId(UUID.randomUUID().toString());
			message.setTimeToLive(Duration.ofHours(3));

			senderClient.sendMessage(message);

		} catch (Exception e) {
			log.error("sendMessageToServiceBus : ", e);
		}
	}

	/**
	 * Method to receive message from service bus
	 */
	public void receiveMessage() {

		ServiceBusProperties serviceBusProperties = (ServiceBusProperties) RCMBeanFactory.getApplicationContext()
				.getBean("serviceBusProperties");
		// create a Service Bus Sender client for the queue
		try (ServiceBusReceiverClient receiverClient = new ServiceBusClientBuilder()
				.connectionString(serviceBusProperties.getConnectionString()).receiver()
				.queueName(serviceBusProperties.getQueueName()).buildClient()) {
			IterableStream<ServiceBusReceivedMessage> serviceBusReceivedMessages = receiverClient.receiveMessages(1,
					Duration.ofSeconds(30));
			for (ServiceBusReceivedMessage message : serviceBusReceivedMessages) {
				log.info("received message : " + message.getBody());
			}
		} catch (Exception e) {
			log.error("AzureOperation.receiveMessage.Exception", e);
		}
	}

	// public void receiveMessages() {
	//
	// //ServiceBusProperties to get service bus connection details
	// ServiceBusProperties serviceBusProperties = (ServiceBusProperties)
	// RCMBeanFactory.getApplicationContext()
	// .getBean("serviceBusProperties");
	// CountDownLatch countdownLatch = new CountDownLatch(1);
	//
	// // Create an instance of the processor through the ServiceBusClientBuilder
	// ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
	// .connectionString(serviceBusProperties.getConnectionString())
	// .processor()
	// .processMessage(AzureOperations::processMessage)
	// .processError(context -> processError(context, countdownLatch))
	// .queueName(serviceBusProperties.getQueueName())
	// .buildProcessorClient();
	//
	// log.info("Starting the processor");
	// processorClient.start();
	//
	// try {
	// TimeUnit.SECONDS.sleep(10);
	// } catch (InterruptedException e) {
	// log.error("AzureOperation.receiveMessages.InterruptedException", e);
	// }
	// log.info("Stopping and closing the processor");
	// processorClient.close();
	// }

	private static void processMessage(ServiceBusReceivedMessageContext context) {
		ServiceBusReceivedMessage message = context.getMessage();
		log.info("Processing message. Session: %s, Sequence #: %s. Contents: %s%n" + message.getMessageId()
				+ message.getSequenceNumber() + message.getBody());
	}

	private static void processError(ServiceBusErrorContext context, CountDownLatch countdownLatch) {
		log.info("Error when receiving messages from namespace: '%s'. Entity: '%s'%n"
				+ context.getFullyQualifiedNamespace() + context.getEntityPath());

		if (!(context.getException() instanceof ServiceBusException)) {
			log.info("Non-ServiceBusException occurred: %s%n" + context.getException());
			return;
		}

		ServiceBusException exception = (ServiceBusException) context.getException();
		ServiceBusFailureReason reason = exception.getReason();

		if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
				|| reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
				|| reason == ServiceBusFailureReason.UNAUTHORIZED) {
			log.info("An unrecoverable error occurred. Stopping processing with reason %s: %s%n" + reason
					+ exception.getMessage());

			countdownLatch.countDown();
		} else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
			log.info("Message lock lost for message: %s%n" + context.getException());
		} else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
			try {
				// Choosing an arbitrary amount of time to wait until trying again.
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				log.error("Unable to sleep for period of time");
			}
		} else {
			log.error("Error source %s, reason %s, message: %s%n" + context.getErrorSource() + reason
					+ context.getException());
		}
	}
}
