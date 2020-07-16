package qslv.transaction.fulfillment;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;
import qslv.common.kafka.ResponseMessage;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CancelReservationRequest;
import qslv.transaction.response.CancelReservationResponse;

@Repository
public class KafkaDao {
	private static final Logger log = LoggerFactory.getLogger(KafkaDao.class);

	@Autowired
	private ConfigProperties config;

	@Autowired
	private KafkaTemplate<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> cancelKafkaTemplate;

	public void setConfig(ConfigProperties config) {
		this.config = config;
	}
	public void setCancelKafkaTemplate(
			KafkaTemplate<String, TraceableMessage<ResponseMessage<CancelReservationRequest, CancelReservationResponse>>> cancelKafkaTemplate) {
		this.cancelKafkaTemplate = cancelKafkaTemplate;
	}

	public void produceCancel(TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>> message) throws DataAccessException {
		log.trace("ENTRY produceCancel");
		try {
			String key = message.getPayload().getRequest().getAccountNumber();
			cancelKafkaTemplate.send(config.getKafkaCancelReplyQueue(), key, message).get();
			log.debug("Kakfa Produce {}", message);
		} catch ( ExecutionException ex ) {
			log.debug(ex.getLocalizedMessage());
			throw new TransientDataAccessResourceException("Kafka Producer failure", ex);
		} catch ( InterruptedException  ex) {
			log.debug(ex.getLocalizedMessage());
			throw new TransientDataAccessResourceException("Kafka Producer failure", ex);
		}
		// TODO: log time it took
		log.trace("EXIT produceCancel");
	}
}
