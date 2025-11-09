package com.daniel.chat_practice.domain.configuration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

@Configuration
public class R2dbcDatasourceConfiguration {
    @Bean
    public ConnectionFactory connectionFactory(@Value("${spring.r2dbc.url}") String url) {
        return ConnectionFactories.get(url);
    }
    @Bean
    @DependsOn("connectionFactory")
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        return new R2dbcEntityTemplate(connectionFactory);
    }
}
