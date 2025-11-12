package com.xynnity.watermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.xynnity.watermanagement.config.MqttProperties;

@SpringBootApplication
@EnableConfigurationProperties(MqttProperties.class)
public class WatermanagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(WatermanagementApplication.class, args);
	}

}
