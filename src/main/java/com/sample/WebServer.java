package com.sample;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;


public class WebServer extends AbstractVerticle {

    private RedisClient redis;
    int nbr = 0;


    public static void main(String[] args) {
        WebServer ws = new WebServer();

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(ws);
    }

    @Override
    public void start() throws Exception {

        JsonObject config = new JsonObject()
                .put("host", "127.0.0.1")
                .put("port", 6379);

        redis = RedisClient.create(Vertx.vertx(), config);

        HttpServer server = vertx.createHttpServer();

        server.requestHandler(res -> {

            System.out.println(nbr++);

            //res.response().setStatusCode(200).end();

            getInBase("nonexistantkey", res);

        }).listen(8000);
    }

    public void getInBase(String key, HttpServerRequest request) {
        redis.get(key, res -> {

            HttpServerResponse response = request.response();

            if (res.succeeded()) {
                response.setStatusCode(200).end();
            } else {
                response.setStatusCode(201).end();
            }
        });
    }
}
