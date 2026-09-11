package com.endit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class ApplicationTest {

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
