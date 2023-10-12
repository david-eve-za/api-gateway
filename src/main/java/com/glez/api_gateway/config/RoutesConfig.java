package com.glez.api_gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
@Slf4j
public class RoutesConfig {

    private final String API_GATEWAY = "api-gateway";


    public Function<PredicateSpec, Buildable<Route>> getEurekaServerRoute() {
        return p -> p.path("/eureka/web")
                .filters(f -> f.setPath("/"))
                .uri("http://localhost:8761");
    }

    public Function<PredicateSpec, Buildable<Route>> getEurekaServerStaticRoute() {
        return p -> p.path("/eureka/**")
                .uri("http://localhost:8761");
    }

    public Function<PredicateSpec, Buildable<Route>> buildRoute(String service) {
        return p -> p.path("/api/" + service.split("-")[0] + "/**")
                .filters(f -> {
                    f.saveSession().tokenRelay();
                    f.stripPrefix(2);
                    return f;
                })
                .uri("lb://" + service);
    }

    public Function<PredicateSpec, Buildable<Route>> buildActuatorRoute(String service) {
        return p -> p.path("/actuator/" + service.split("-")[0] + "/**")
                .filters(f -> f.saveSession().tokenRelay())
                .uri("lb://" + service + "/actuator/" + service.split("-")[0] + "/**");
    }


    @Bean
    public RouteLocator myRoutes(ReactiveDiscoveryClient discoveryClient, RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        discoveryClient.getServices().toStream().forEach(service -> {
            if (!service.equals(API_GATEWAY)){
                log.info("Service discovered: {}", service);
                routes.route(buildRoute(service));
                routes.route(buildActuatorRoute(service));
            }
        });
        routes.route(getEurekaServerRoute());
        routes.route(getEurekaServerStaticRoute());


        return routes.build();
    }
}
