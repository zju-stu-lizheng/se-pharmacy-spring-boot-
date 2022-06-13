package zju.se.pharmacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PharmacyApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyJDBC.class, args);
	}

}
