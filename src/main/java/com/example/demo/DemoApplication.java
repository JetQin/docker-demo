package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SpringBootApplication
@RestController
public class DemoApplication {

	@GetMapping("/greeting")
	public String greeeting(@RequestParam(name="name", required=false, defaultValue="World")String name){
		return "Hello " + name;
	}


	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
