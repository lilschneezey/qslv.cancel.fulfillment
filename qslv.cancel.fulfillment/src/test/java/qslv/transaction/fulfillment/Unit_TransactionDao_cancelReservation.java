package qslv.transaction.fulfillment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;
import qslv.common.TimedResponse;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CancelReservationRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.CancelReservationResponse;

@ExtendWith(MockitoExtension.class)

class Unit_TransactionDao_cancelReservation {

	@Mock
	RestTemplateProxy restTemplateProxy;
	
	ConfigProperties config = new ConfigProperties();
	TransactionDao transactionDao = new TransactionDao();
	RetryTemplate retryTemplate = new RetryTemplate() ;
	
	{
		SimpleRetryPolicy srp = new SimpleRetryPolicy();
		srp.setMaxAttempts(3);
		retryTemplate.setThrowLastExceptionOnExhausted(true);
		retryTemplate.setRetryPolicy(srp);
		transactionDao.setRetryTemplate(retryTemplate);
		config.setAitid("723842");
		config.setCancelReservationUrl("http://localhost:9091/CancelTransaction");
		transactionDao.setConfig(config);
	}
	
	@BeforeEach
	public void init() {
		transactionDao.setRestTemplateProxy(restTemplateProxy);			
	}
	
	@Test
	void test_cancelReservation_success() {
		
		//-Setup -----------
		TraceableMessage<CancelReservationRequest> message = setup_traceable_message();
		ResponseEntity<TimedResponse<CancelReservationResponse>> response = setup_responseEntity();
		
		//-Prepare----------------
		doReturn(response).when(restTemplateProxy).exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TraceableMessage<CancelReservationRequest>>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CancelReservationResponse>>>any());
		
		//-Execute----------------
		CancelReservationResponse callresult = transactionDao.cancelReservation(message, message.getPayload());

		//-Verify----------------
		assertSame(response.getBody().getPayload(), callresult);
	}

	TraceableMessage<CancelReservationRequest> setup_traceable_message() {
		TraceableMessage<CancelReservationRequest> message = new TraceableMessage<CancelReservationRequest>();
		message.setBusinessTaxonomyId("jskdfjsdjfls");
		message.setCorrelationId("sdjfsjdlfjslkdfj");
		message.setMessageCreationTime(LocalDateTime.now());
		message.setProducerAit("234234");
		message.setPayload(setup_request());
		return message;
	}
	CancelReservationRequest setup_request() {
		CancelReservationRequest request = new CancelReservationRequest();
		request.setReservationUuid(UUID.randomUUID());
		request.setRequestUuid(UUID.randomUUID());
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
	
	@Test
	void test_cancelReservation_failsOnce() {

		//-Setup -----------
		TraceableMessage<CancelReservationRequest> message = setup_traceable_message();
		ResponseEntity<TimedResponse<CancelReservationResponse>> response = setup_responseEntity();
		
		//-Prepare----------------
		doThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
		.doReturn(response)
		.when(restTemplateProxy).exchange(anyString(), eq(HttpMethod.POST), 
			ArgumentMatchers.<HttpEntity<TraceableMessage<CancelReservationRequest>>>any(), 
			ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CancelReservationResponse>>>any());
		
		//-Execute----------------
		CancelReservationResponse callresult = transactionDao.cancelReservation(message, message.getPayload());

		//-Verify----------------
		assertSame(response.getBody().getPayload(), callresult);
	}
	
	@Test
	void test_cancelReservation_failsTwice() {

		//-Setup -----------
		TraceableMessage<CancelReservationRequest> message = setup_traceable_message();
		ResponseEntity<TimedResponse<CancelReservationResponse>> response = setup_responseEntity();
		
		//-Prepare----------------
		doThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
		.doThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
		.doReturn(response)
		.when(restTemplateProxy).exchange(anyString(), eq(HttpMethod.POST), 
			ArgumentMatchers.<HttpEntity<TraceableMessage<CancelReservationRequest>>>any(), 
			ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CancelReservationResponse>>>any());
		
		//-Execute----------------
		CancelReservationResponse callresult = transactionDao.cancelReservation(message, message.getPayload());

		//-Verify----------------
		assertSame(response.getBody().getPayload(), callresult);
	}

	@Test
	void test_cancelReservation_failsThrice() {
		//-Setup -----------
		TraceableMessage<CancelReservationRequest> message = setup_traceable_message();
		
		//-Prepare----------------
		doThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
		.doThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
		.doThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
		.when(restTemplateProxy).exchange(anyString(), eq(HttpMethod.POST), 
			ArgumentMatchers.<HttpEntity<TraceableMessage<CancelReservationRequest>>>any(), 
			ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CancelReservationResponse>>>any());
		
		//-Execute----------------
		assertThrows(TransientDataAccessResourceException.class, () -> {
			transactionDao.cancelReservation(message, message.getPayload());
		});

	}
	
	@Test
	void test_recordReservation_throwsNonTransient() {
		
		//-Setup -----------
		TraceableMessage<CancelReservationRequest> message = setup_traceable_message();
		ResponseEntity<TimedResponse<CancelReservationResponse>> response = setup_failedResponseEntity();
		
		//-Prepare----------------
		doReturn(response).when(restTemplateProxy).exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TraceableMessage<CancelReservationRequest>>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CancelReservationResponse>>>any());

		//-Execute----------------
		assertThrows(NonTransientDataAccessResourceException.class, () -> {
			transactionDao.cancelReservation(message, message.getPayload());
		});

	}
}
