package qslv.transaction.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FulfillCancelApplication {
	
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(FulfillCancelApplication.class);
        application.run(args);
	}

}
