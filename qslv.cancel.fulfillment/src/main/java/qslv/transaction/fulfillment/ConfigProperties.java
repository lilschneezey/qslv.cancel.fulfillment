package qslv.transaction.fulfillment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import qslv.util.EnableQuickSilver;

@Configuration
@ConfigurationProperties(prefix = "qslv")
@EnableQuickSilver
public class ConfigProperties {

	private String aitid = "27834";
	private String cancelReservationUrl;
	private String commitReservationUrl;
	private String postReservationUrl;
	private String postTransactionUrl;
	private String transferAndTransactUrl;
	private int restConnectionRequestTimeout = 1000;
	private int restConnectTimeout = 1000;
	private int restTimeout = 1000;
	private int restAttempts = 3;
	private int restBackoffDelay = 100;
	private int restBackoffDelayMax = 500; 
	private String kafkaTransactionRequestQueue;
	private String kafkaTransactionReplyQueue;
	private String kafkaCancelRequestQueue;
	private String kafkaCancelReplyQueue;
	private String kafkaCommitRequestQueue;
	private String kafkaCommitReplyQueue;
	private int kafkaTimeout;
	private String kafkaProducerPropertiesPath;
	private String kafkaConsumerPropertiesPath;

	public String getAitid() {
		return aitid;
	}

	public void setAitid(String aitid) {
		this.aitid = aitid;
	}

	public String getPostTransactionUrl() {
		return postTransactionUrl;
	}

	public void setPostTransactionUrl(String postTransactionUrl) {
		this.postTransactionUrl = postTransactionUrl;
	}

	public String getTransferAndTransactUrl() {
		return transferAndTransactUrl;
	}

	public void setTransferAndTransactUrl(String transferAndTransactUrl) {
		this.transferAndTransactUrl = transferAndTransactUrl;
	}

	public String getCommitReservationUrl() {
		return commitReservationUrl;
	}

	public void setCommitReservationUrl(String commitReservationUrl) {
		this.commitReservationUrl = commitReservationUrl;
	}

	public int getRestConnectionRequestTimeout() {
		return restConnectionRequestTimeout;
	}

	public void setRestConnectionRequestTimeout(int restConnectionRequestTimeout) {
		this.restConnectionRequestTimeout = restConnectionRequestTimeout;
	}

	public int getRestConnectTimeout() {
		return restConnectTimeout;
	}

	public void setRestConnectTimeout(int restConnectTimeout) {
		this.restConnectTimeout = restConnectTimeout;
	}

	public int getRestTimeout() {
		return restTimeout;
	}

	public void setRestTimeout(int restTimeout) {
		this.restTimeout = restTimeout;
	}

	public int getRestAttempts() {
		return restAttempts;
	}

	public void setRestAttempts(int restAttempts) {
		this.restAttempts = restAttempts;
	}

	public int getRestBackoffDelay() {
		return restBackoffDelay;
	}

	public void setRestBackoffDelay(int restBackoffDelay) {
		this.restBackoffDelay = restBackoffDelay;
	}

	public int getRestBackoffDelayMax() {
		return restBackoffDelayMax;
	}

	public void setRestBackoffDelayMax(int restBackoffDelayMax) {
		this.restBackoffDelayMax = restBackoffDelayMax;
	}

	public int getKafkaTimeout() {
		return kafkaTimeout;
	}

	public void setKafkaTimeout(int kafkaTimeout) {
		this.kafkaTimeout = kafkaTimeout;
	}

	public String getCancelReservationUrl() {
		return cancelReservationUrl;
	}

	public void setCancelReservationUrl(String cancelReservationUrl) {
		this.cancelReservationUrl = cancelReservationUrl;
	}

	public String getPostReservationUrl() {
		return postReservationUrl;
	}

	public void setPostReservationUrl(String postReservationUrl) {
		this.postReservationUrl = postReservationUrl;
	}

	public String getKafkaTransactionRequestQueue() {
		return kafkaTransactionRequestQueue;
	}

	public void setKafkaTransactionRequestQueue(String kafkaTransactionRequestQueue) {
		this.kafkaTransactionRequestQueue = kafkaTransactionRequestQueue;
	}

	public String getKafkaTransactionReplyQueue() {
		return kafkaTransactionReplyQueue;
	}

	public void setKafkaTransactionReplyQueue(String kafkaTransactionReplyQueue) {
		this.kafkaTransactionReplyQueue = kafkaTransactionReplyQueue;
	}

	public String getKafkaCancelRequestQueue() {
		return kafkaCancelRequestQueue;
	}

	public void setKafkaCancelRequestQueue(String kafkaCancelRequestQueue) {
		this.kafkaCancelRequestQueue = kafkaCancelRequestQueue;
	}

	public String getKafkaCancelReplyQueue() {
		return kafkaCancelReplyQueue;
	}

	public void setKafkaCancelReplyQueue(String kafkaCancelReplyQueue) {
		this.kafkaCancelReplyQueue = kafkaCancelReplyQueue;
	}

	public String getKafkaCommitRequestQueue() {
		return kafkaCommitRequestQueue;
	}

	public void setKafkaCommitRequestQueue(String kafkaCommitRequestQueue) {
		this.kafkaCommitRequestQueue = kafkaCommitRequestQueue;
	}

	public String getKafkaCommitReplyQueue() {
		return kafkaCommitReplyQueue;
	}

	public void setKafkaCommitReplyQueue(String kafkaCommitReplyQueue) {
		this.kafkaCommitReplyQueue = kafkaCommitReplyQueue;
	}

	public String getKafkaProducerPropertiesPath() {
		return kafkaProducerPropertiesPath;
	}

	public void setKafkaProducerPropertiesPath(String kafkaProducerPropertiesPath) {
		this.kafkaProducerPropertiesPath = kafkaProducerPropertiesPath;
	}

	public String getKafkaConsumerPropertiesPath() {
		return kafkaConsumerPropertiesPath;
	}

	public void setKafkaConsumerPropertiesPath(String kafkaConsumerPropertiesPath) {
		this.kafkaConsumerPropertiesPath = kafkaConsumerPropertiesPath;
	}

}
