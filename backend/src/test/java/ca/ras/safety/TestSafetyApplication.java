package ca.ras.safety;

import org.springframework.boot.SpringApplication;

public class TestSafetyApplication {

	public static void main(String[] args) {
		SpringApplication.from(SafetyApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
