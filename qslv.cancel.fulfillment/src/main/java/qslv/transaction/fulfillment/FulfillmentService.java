package qslv.transaction.fulfillment;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import qslv.common.kafka.ResponseMessage;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CancelReservationRequest;
import qslv.transaction.response.CancelReservationResponse;
import qslv.util.ServiceLevelIndicator;

@Service
public class FulfillmentService {
	private static final Logger log = LoggerFactory.getLogger(FulfillmentService.class);
	
	@Autowired
	private ConfigProperties config;
	@Autowired
	TransactionDao transactionDao;
	@Autowired
	private KafkaProducerDao kafkaDao;

	public void setKafkaDao(KafkaProducerDao kafkaDao) {
		this.kafkaDao = kafkaDao;
	}
	public void setConfig(ConfigProperties config) {
		this.config = config;
	}
	public void setTransactionDao(TransactionDao transactionDao) {
		this.transactionDao = transactionDao;
	}

	public void fulfillCancel(TraceableMessage<CancelReservationRequest> message, Acknowledgment acknowledgment) {
		log.warn("ENTRY FulfillmentControllerService::fulfillCancel");
		
		TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>> traceableResponse = 
				new TraceableMessage<>(message, new ResponseMessage<CancelReservationRequest,CancelReservationResponse>(message.getPayload()));

		try {
			validateMessage(message);
			validateCancelReservationRequest(message.getPayload());	

			CancelReservationResponse cancelResponse = transactionDao.cancelReservation(message, message.getPayload());

			traceableResponse.getPayload().setResponse( cancelResponse );
			traceableResponse.setMessageCompletionTime(LocalDateTime.now());

			kafkaDao.produceResponse(traceableResponse);
			ServiceLevelIndicator.logAsyncServiceElapsedTime(log, "TransferFulfillment::fulfillCancel", 
					config.getAitid(), message.getMessageCreationTime());
		} catch (TransientDataAccessException ex) {
			log.warn("Recoverable error. Return message to Kafka and sleep for {} ms.", config.getKafkaTimeout());
			acknowledgment.nack(10000L);
			return;	

		} catch (Exception ex) {
			log.error("Unrecoverable exception thrown. {}", ex.getLocalizedMessage());

			traceableResponse.getPayload().setErrorMessage(ex.getLocalizedMessage());
			if ( ex instanceof MalformedMessageException) {
				traceableResponse.getPayload().setStatus(ResponseMessage.MALFORMED_MESSAGE);				
			} else {
				traceableResponse.getPayload().setStatus(ResponseMessage.INTERNAL_ERROR);
			}
			
			try {
				kafkaDao.produceResponse(traceableResponse);
			} catch (Exception iex) {
				log.error("Additional unexpected exception caught while processing unexpected exception. Keep message on Kafka. {}", iex.getLocalizedMessage());
				acknowledgment.nack(10000L);
				return;	
			}
		}

		acknowledgment.acknowledge();
		log.warn("EXIT FulfillmentControllerService::fulfillCancel");
	}
	public class MalformedMessageException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public MalformedMessageException(String msg) {
			super(msg);
		}
	}
	private void validateCancelReservationRequest( CancelReservationRequest request) {
		if (request.getRequestUuid() == null) {
			throw new MalformedMessageException("Malformed message payload. Missing From Request UUID.");
		}
		if (request.getReservationUuid() == null) {
			throw new MalformedMessageException("Malformed message payload. Missing From Reservation UUID.");
		}
		if (request.getTransactionMetaDataJson() == null || request.getTransactionMetaDataJson().isEmpty()) {
			throw new MalformedMessageException("Malformed message payload. Missing Meta Data.");
		}
	}
	private void validateMessage(TraceableMessage<?> data) throws NonTransientDataAccessResourceException {
		if (null == data.getProducerAit()) {
			throw new MalformedMessageException("Malformed message. Missing Producer AIT Id.");
		}
		if (null == data.getCorrelationId()) {
			throw new MalformedMessageException("Malformed message. Missing Correlation Id.");
		}
		if (null == data.getBusinessTaxonomyId()) {
			throw new MalformedMessageException("Malformed message. Missing Business Taxonomy Id.");
		}
		if (null == data.getMessageCreationTime()) {
			throw new MalformedMessageException("Malformed message. Missing Message Creation Time.");
		}
		if (null == data.getPayload()) {
			throw new MalformedMessageException("Malformed message. Missing Fulfillment Message.");
		}
	}
}
