package com.gianmdp03.cuando_llega_pro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CuandoLlegaProApplication {

	public static void main(String[] args) {
		SpringApplication.run(CuandoLlegaProApplication.class, args);
	}

}
