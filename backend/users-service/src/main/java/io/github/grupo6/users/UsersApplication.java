package io.github.grupo6.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UsersApplication {

    /**
     * Inicia la ejecución del microservicio de usuarios y autenticación Spring Boot.
     */
    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--bootstrap-admin")) {
            if (args.length != 1) {
                System.err.println("El bootstrap acepta configuración exclusivamente mediante variables de entorno");
                System.exit(1);
            }
            System.exit(io.github.grupo6.users.bootstrap.AdminBootstrap.execute(System.getenv()));
        }
        SpringApplication.run(UsersApplication.class, args);
    }
}
