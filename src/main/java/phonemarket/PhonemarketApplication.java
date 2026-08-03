package phonemarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PhonemarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhonemarketApplication.class, args);
	}

}
