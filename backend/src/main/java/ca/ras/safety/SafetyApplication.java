package ca.ras.safety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
public class SafetyApplication {
	@Bean Clock clock() { return Clock.system(ZoneId.of("America/Vancouver")); }

	public static void main(String[] args) {
		SpringApplication.run(SafetyApplication.class, args);
	}

}
