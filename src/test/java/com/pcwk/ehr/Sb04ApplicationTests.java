package com.pcwk.ehr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SpringBootTest
class Sb04ApplicationTests {

	final Logger log = LoggerFactory.getLogger(getClass());

	
	@Test
	void contextLoads() {
		log.trace("TRACE");
		log.debug("DEBUG");
		log.info("INFO : {}","good");
		log.warn("WARN");
		log.error("ERROR");
	}

}
