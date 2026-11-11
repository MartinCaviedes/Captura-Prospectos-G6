package io.github.grupo6.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UsersApplication {

    /**
     * Inicia la ejecución del microservicio de usuarios y autenticación Spring Boot.
     */
    public static void main(String[] args) {
        SpringApplication.run(UsersApplication.class, args);
    }
}
