package com.example.client;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class JokesClient {

    private final RequestSpecification spec = new RequestSpecBuilder()
            .setBaseUri("https://api.chucknorris.io")
            .setBasePath("/jokes")
            .setAccept(ContentType.JSON)
            .build();

    public Response getRandom() {
        return given().spec(spec).get("/random");
    }

    public Response getRandom(String category) {
        return given().spec(spec).queryParam("category", category).get("/random");
    }

    public Response getCategories() {
        return given().spec(spec).get("/categories");
    }

    public Response search(String query) {
        return given().spec(spec).queryParam("query", query).get("/search");
    }

    public Response getById(String id) {
        return given().spec(spec).get("/{id}", id);
    }
}