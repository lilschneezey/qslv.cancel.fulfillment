package qslv.transaction.fulfillment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JavaType;

import qslv.common.TimedResponse;
import qslv.common.kafka.JacksonAvroDeserializer;
import qslv.common.kafka.JacksonAvroSerializer;
import qslv.common.kafka.ResponseMessage;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CancelReservationRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.CancelReservationResponse;

@SpringBootTest
@Import(value = { TestConfig.class })
@DirtiesContext
@EmbeddedKafka(partitions = 1,topics = { "request.queue", "reply.queue" })
@ActiveProfiles("test")
public class Unit_EmbeddedKakfa {
	private static String request_topic = "request.queue";
	private static String reply_topic = "reply.queue";
	@Autowired EmbeddedKafkaBroker embeddedKafka;
	@Autowired KafkaCancelListener kafkaCancelListener;
	@Autowired KafkaListenerConfig kafkaListenerConfig;	
	@Autowired TransactionDao transactionDao;
	@Autowired ConfigProperties configProperties;
	
	@Mock
	RestTemplateProxy restTemplateProxy;
	
	@BeforeEach
	public void init() {
		transactionDao.setRestTemplateProxy(restTemplateProxy);	
		configProperties.setKafkaCancelRequestQueue(request_topic);
		configProperties.setKafkaCancelReplyQueue(reply_topic);
	}
	
	//----------------
	// Kafka Reconfigure
	//----------------
	private Map<String,Object> embeddedConfig() {
		HashMap<String, Object> props = new HashMap<>();
		props.put("bootstrap.servers", embeddedKafka.getBrokersAsString());
		props.put("group.id", "whatever");
		props.put("schema.registry.url", "http://localhost:8081");
		return props;
	}
	
	@Test
	void test_cancelReservation_success() {

		// -Setup input output----------------
		Producer<String, TraceableMessage<CancelReservationRequest>> producer = buildProducer();
		Consumer<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> consumer = buildConsumer();
		ResponseEntity<TimedResponse<CancelReservationResponse>> response = setup_responseEntity();
		
		// -Prepare----------------------------
		doReturn(response).when(restTemplateProxy).exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TraceableMessage<CancelReservationRequest>>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CancelReservationResponse>>>any());
		//doNothing().when(fulfillmentController).fulfillCancel(any(), any());
		
		TraceableMessage<CancelReservationRequest> message = setup_traceable();
		
		// -Execute----------------
		producer.send(new ProducerRecord<>(request_topic, message.getPayload().getAccountNumber(), message));
		producer.flush();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, reply_topic);
		ConsumerRecord<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> record 
			= KafkaTestUtils.getSingleRecord(consumer, reply_topic, 2000L);

		// -Verify----------------
		verify(restTemplateProxy).exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TraceableMessage<CancelReservationRequest>>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CancelReservationResponse>>>any());
		assertNotNull(record);
		assertEquals(message.getPayload().getAccountNumber(), record.key());
		assertEquals(message.getBusinessTaxonomyId(), record.value().getBusinessTaxonomyId());
		assertEquals(message.getPayload().getAccountNumber(), record.value().getPayload().getRequest().getAccountNumber());
	}

	//------------------------------------------------------
	// Mock producer and consumer to test end-to-end flow
	//------------------------------------------------------
	private Producer<String, TraceableMessage<CancelReservationRequest>> buildProducer() {
		Map<String, Object> configs = embeddedConfig();
		
    	JacksonAvroSerializer<TraceableMessage<CancelReservationRequest>> jas = new JacksonAvroSerializer<>();
		JavaType type = jas.getTypeFactory().constructParametricType(TraceableMessage.class, CancelReservationRequest.class);
    	jas.configure(configs, false, type);
		
		return new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), jas ).createProducer();
	}
	private Consumer<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> buildConsumer() {
		Map<String, Object> configs = embeddedConfig();
		configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		configs.put("group.id", "somevalue");
		
    	JacksonAvroDeserializer<TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> jad = new JacksonAvroDeserializer<>();
    	jad.configure(configs);

		return new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), jad).createConsumer();
	}
	
	TraceableMessage<CancelReservationRequest> setup_traceable() {
		TraceableMessage<CancelReservationRequest> message = new TraceableMessage<>();
		message.setPayload(setup_request());
		message.setBusinessTaxonomyId("234234234234");
		message.setCorrelationId("328942834234j23k4");
		message.setMessageCreationTime(LocalDateTime.now());
		message.setProducerAit("27834");
		return message;
	}
	CancelReservationRequest setup_request() {
		CancelReservationRequest request = new CancelReservationRequest();
		request.setAccountNumber("12345634579");
		request.setRequestUuid(UUID.randomUUID());
		request.setReservationUuid(UUID.randomUUID());
		request.setTransactionMetaDataJson("{}");
		return request;
	}
	
	ResponseEntity<TimedResponse<CancelReservationResponse>> setup_responseEntity() {
		return new ResponseEntity<TimedResponse<CancelReservationResponse>>(new TimedResponse<>(123456L, setup_response()), HttpStatus.CREATED);
	}
	ResponseEntity<TimedResponse<CancelReservationResponse>> setup_failedResponseEntity() {
		return new ResponseEntity<TimedResponse<CancelReservationResponse>>(new TimedResponse<>(234567L, setup_response()), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	CancelReservationResponse setup_response() {
		CancelReservationResponse resourceResponse = new CancelReservationResponse(CancelReservationResponse.SUCCESS, new TransactionResource());
		resourceResponse.getResource().setAccountNumber("12345679");
		resourceResponse.getResource().setDebitCardNumber("7823478239467");
		return resourceResponse;
	}
}
