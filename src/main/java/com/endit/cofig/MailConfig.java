/**
 * <pre>
 * Class Name : MailConfig
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
package com.endit.cofig;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.endit.serviceImpl.DummyMailService;

@Configuration
public class MailConfig {
	final Logger log = LoggerFactory.getLogger(getClass());

	
	@Value("${spring.mail.host}")
	private String host;
	
	@Value("${spring.mail.port}")
	private int port;
	
	@Value("${spring.mail.username}")
	private String username;
	
	@Value("${spring.mail.password}")
	private String password;
	
	@Value("${spring.mail.default-encoding}")
	private String defaultEncoding;
	
	
	@Bean
	@Profile("run")
	public JavaMailSenderImpl mailSender() {
		JavaMailSenderImpl sender=new JavaMailSenderImpl();
		log.debug("==============================");
		log.debug("mailSender()");
		log.debug("==============================");
		
//		log.debug("host:{}",host);
//		log.debug("port:{}",port);
//		log.debug("username:{}",username);
//		log.debug("password:{}",password);
//		log.debug("defaultEncoding:{}",defaultEncoding);

		sender.setHost(host);
		sender.setPort(port);
		sender.setUsername(username);
		sender.setPassword(password);
		sender.setDefaultEncoding(defaultEncoding);
		
		
		Properties props= sender.getJavaMailProperties();
		
		props.put("mail.smtp.ssl.protocols", "TLSv1.2");
		props.put("mail.smtp.ssl.enable","true");
		props.put("mail.smtp.ssl.trust","smtp.naver.com");
		props.put("mail.debug","true");						
		
		
		return sender;
	}
	
	@Bean
	@Profile("dev")
	public DummyMailService dummyMailService() {
		return new DummyMailService();
	}
	
}
