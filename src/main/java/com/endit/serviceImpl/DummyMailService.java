/**
 * <pre>
 * Class Name : DummyMailService
 * Description :
 *
 * Modification Information
 * 수정일        수정자     수정내용
 * ----------  --------  ---------------------------
 * 2026. 8. 4.  user   최초 생성
 * </pre>
 *
 * @author user
 * @since 2026. 8. 4.
 */
package com.endit.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

public class DummyMailService implements MailSender {

	final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public void send(SimpleMailMessage simpleMessage) throws MailException {
		log.debug("---------------------");
		log.debug("DummyMailService()   ");
		log.debug("개발에서는 메일 전송 않됨!  ");
		log.debug("---------------------");

	}

	@Override
	public void send(SimpleMailMessage... simpleMessages) throws MailException {
		log.debug("---------------------");
		log.debug("DummyMailService()   ");
		log.debug("개발에서는 메일 전송 않됨!  ");
		log.debug("---------------------");

	}

}
