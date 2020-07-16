package qslv.transaction.fulfillment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import qslv.common.kafka.ResponseMessage;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CancelReservationRequest;
import qslv.transaction.response.CancelReservationResponse;

@ExtendWith(MockitoExtension.class)
public class Unit_KafkaDao_cancelReservation {
	KafkaDao kafkaDao = new KafkaDao();
	ConfigProperties config = new ConfigProperties();
	
	@Mock
	KafkaTemplate<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> cancelKafkaTemplate;
	@Mock
	ListenableFuture<SendResult<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>>> future;
	
	{
		config.setKafkaCancelReplyQueue("CancelURL");
		kafkaDao.setConfig(config);
	}
	
	@BeforeEach
	public void setup() {
		kafkaDao.setCancelKafkaTemplate(cancelKafkaTemplate);
	}
	
	TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>> setup_message() {
		TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>> message = 
				new TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>> ();
		
		message.setPayload(new ResponseMessage<CancelReservationRequest,CancelReservationResponse>());
		message.getPayload().setRequest(new CancelReservationRequest());
		message.getPayload().getRequest().setAccountNumber("2839420384902");
		
		return message;
	}
	
	@Test
	public void test_produceCancel_success() throws InterruptedException, ExecutionException {
		
		//-Setup---------------
		TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>> setup_message = setup_message();
		
		//-Prepare---------------
		ProducerRecord<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> producerRecord 
			= new ProducerRecord<>("mockTopicName", setup_message);
		
		SendResult<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> sendResult 
			= new SendResult<>(producerRecord, new RecordMetadata(new TopicPartition("mockTopic", 1), 1, 1, 1, 1L, 1, 1));
		
		doReturn(sendResult).when(future).get();
		doReturn(future).when(cancelKafkaTemplate).send(anyString(), anyString(), any());
	
		//-Execute----------------------------		
		kafkaDao.produceCancel(setup_message);
		
		//-Verify----------------------------		
		verify(future).get();
		ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);
		verify(cancelKafkaTemplate).send(anyString(), arg.capture(), any());
		assertEquals( arg.getValue(), setup_message.getPayload().getRequest().getAccountNumber());
	}
	
	@Test
	public void test_produceTransferMessage_throwsTransient() throws InterruptedException, ExecutionException, TimeoutException {
		
		//-Prepare---------------
		TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>> setup_message = setup_message();

		//-Prepare---------------
		doThrow(new InterruptedException()).when(future).get();
		doReturn(future).when(cancelKafkaTemplate).send(anyString(), anyString(), any());
		
		//--Execute--------------	
		assertThrows(TransientDataAccessResourceException.class, () -> {
			kafkaDao.produceCancel(setup_message);
		});
	}

}
