package qslv.transaction.fulfillment;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.ResourceAccessException;
import qslv.common.TimedResponse;
import qslv.common.TraceableRequest;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CancelReservationRequest;
import qslv.transaction.response.CancelReservationResponse;

@Repository
public class TransactionDao {
	private static final Logger log = LoggerFactory.getLogger(TransactionDao.class);
	private static ParameterizedTypeReference<TimedResponse<CancelReservationResponse>> cancelResponseType =
			new ParameterizedTypeReference<TimedResponse<CancelReservationResponse>>() {};

	@Autowired
	private ConfigProperties config;
	
	@Autowired
	private RestTemplateProxy restTemplateProxy;
	@Autowired
	private RetryTemplate retryTemplate;

	public void setConfig(ConfigProperties config) {
		this.config = config;
	}
	public void setRestTemplateProxy(RestTemplateProxy restTemplateProxy) {
		this.restTemplateProxy = restTemplateProxy;
	}
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}
	
	public CancelReservationResponse cancelReservation(final TraceableMessage<?> message, final CancelReservationRequest request) {
		log.warn("cancelReservation ENTRY");

		HttpHeaders headers = buildHeaders(message);
		headers.add(TraceableRequest.ACCEPT_VERSION, CancelReservationRequest.VERSION_1_0);
		CancelReservationResponse response = callService(headers, message, config.getCancelReservationUrl(), request, cancelResponseType);
		int status = response.getStatus();
		if (status != CancelReservationResponse.SUCCESS) {
			String msg = String.format("Unexpected return from %s Service. %s", config.getCancelReservationUrl(), response.toString());
			log.error(msg);
			throw new NonTransientDataAccessResourceException(msg);
		}

		log.warn("cancelReservation EXIT");
		return response;
	}

	private <M,R> R callService(final HttpHeaders headers, final TraceableMessage<?> message, 
			String url, M request, ParameterizedTypeReference<TimedResponse<R>> typereference) {
		log.trace("callService ENTRY");

		ResponseEntity<TimedResponse<R>> response = null;
		try {
			response = retryTemplate.execute(new RetryCallback<ResponseEntity<TimedResponse<R>>, ResourceAccessException>() {
				public ResponseEntity<TimedResponse<R>> doWithRetry( RetryContext context) throws ResourceAccessException {
					return restTemplateProxy.exchange(url, HttpMethod.POST,
							new HttpEntity<M>(request, headers), typereference);
			}});
		} catch (ResourceAccessException ex) {
			String msg = String.format("Exhausted %d retries for POST %s.", config.getRestAttempts(), url);
			log.warn(msg);
			throw new TransientDataAccessResourceException(msg, ex);
		} catch (Exception ex) {
			log.error(ex.getLocalizedMessage());
			throw (ex);
		}
		if (!response.hasBody() || !response.getStatusCode().equals(HttpStatus.CREATED) ) {
			String msg = String.format("Unexpected return from %s Service. %s", url, response.toString());
			log.error(msg);
			throw new NonTransientDataAccessResourceException(msg);
		}
		log.trace("callService ENTRY");
		return response.getBody().getPayload();
	}
	
	private HttpHeaders buildHeaders(final TraceableMessage<?> message) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON) );
		headers.add(TraceableRequest.AIT_ID, config.getAitid());
		headers.add(TraceableRequest.BUSINESS_TAXONOMY_ID, message.getBusinessTaxonomyId());
		headers.add(TraceableRequest.CORRELATION_ID, message.getCorrelationId());
		return headers;
	}
}