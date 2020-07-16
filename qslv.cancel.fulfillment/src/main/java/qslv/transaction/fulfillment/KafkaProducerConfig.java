package qslv.transaction.fulfillment;

import java.util.Map;
import javax.annotation.Resource;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import com.fasterxml.jackson.databind.JavaType;

import qslv.common.kafka.JacksonAvroSerializer;
import qslv.common.kafka.ResponseMessage;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CancelReservationRequest;
import qslv.transaction.response.CancelReservationResponse;

@Configuration
public class KafkaProducerConfig {

	@Resource(name="producerConfig")
	Map<String,Object> producerConfig;

	@Bean
	public ProducerFactory<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> cancelProducerFactory() throws Exception {
		
    	JacksonAvroSerializer<TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> jas = new JacksonAvroSerializer<>();
		JavaType type = jas.getTypeFactory().constructParametricType(TraceableMessage.class, 
				jas.getTypeFactory().constructParametricType(ResponseMessage.class, CancelReservationRequest.class, CancelReservationResponse.class));
    	jas.configure(producerConfig, false, type);
	
		return new DefaultKafkaProducerFactory<>(producerConfig, new StringSerializer(), jas);
	}

	@Bean
	public KafkaTemplate<String, TraceableMessage<ResponseMessage<CancelReservationRequest,CancelReservationResponse>>> cancelKafkaTemplate() throws Exception {
		return new KafkaTemplate<>(cancelProducerFactory(), true); // auto-flush true, to force each message to broker.
	}

}
