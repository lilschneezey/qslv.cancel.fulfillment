package qslv.transaction.fulfillment;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CancelReservationRequest;

@Component
public class KafkaCancelListener {
	private static final Logger log = LoggerFactory.getLogger(KafkaCancelListener.class);

	@Autowired
	private FulfillmentService fulfillmentController;

	public void setFulfillmentController(FulfillmentService fulfillmentController) {
		this.fulfillmentController = fulfillmentController;
	}

	@KafkaListener(topics = "#{ @configProperties.kafkaCancelRequestQueue }")
	void onCancelMessage(final ConsumerRecord<String, TraceableMessage<CancelReservationRequest>> data, Acknowledgment acknowledgment) {
		log.trace("onMessage ENTRY");

		fulfillmentController.fulfillCancel(data.value(), acknowledgment);
		log.error("========================={} {}", data.key(), data.value());

		log.trace("onMessage EXIT");
	}

}
