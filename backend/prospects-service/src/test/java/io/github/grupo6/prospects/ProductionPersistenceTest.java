package io.github.grupo6.prospects;

import io.github.grupo6.prospects.model.Prospect;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;

class ProductionPersistenceTest {
    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Prospect.class)
    static class Entities {}

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class))
            .withUserConfiguration(Entities.class)
            .withPropertyValues("spring.profiles.active=prod");
    }

    @Test void missingProductionDatabaseCannotUseDevelopmentDefault() {
        runner().run(context -> assertThat(context).hasFailed());
    }

    @Test void productionValidatesSchemaWithoutCreatingTables() {
        String database = "jdbc:h2:mem:prod_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        runner().withPropertyValues("PROSPECTS_JDBC_URL=" + database,
            "PROSPECTS_DB_USER=sa", "PROSPECTS_DB_PASSWORD=test-only")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasStackTraceContaining("Schema-validation: missing table");
            });
        // Crea únicamente el esquema ficticio de esta prueba en memoria.
        runner().withPropertyValues("PROSPECTS_JDBC_URL=" + database,
            "PROSPECTS_DB_USER=sa", "PROSPECTS_DB_PASSWORD=test-only", "spring.jpa.hibernate.ddl-auto=create")
            .run(context -> assertThat(context).hasNotFailed());
        runner().withPropertyValues("PROSPECTS_JDBC_URL=" + database,
            "PROSPECTS_DB_USER=sa", "PROSPECTS_DB_PASSWORD=test-only")
            .run(context -> assertThat(context).hasNotFailed());
    }
}
