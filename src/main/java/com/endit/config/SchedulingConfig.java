package com.endit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @Scheduled}를 붙인 빈이 실제로 등록되려면 이 설정이 있어야 한다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {

}
