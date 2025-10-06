package mvrpl.br.reverse_proxy_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@ComponentScan({"mvrpl.br.controller", "mvrpl.br.service"})
@EnableAutoConfiguration
@SpringBootApplication
public class ReverseProxyDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReverseProxyDemoApplication.class, args);
	}

}
