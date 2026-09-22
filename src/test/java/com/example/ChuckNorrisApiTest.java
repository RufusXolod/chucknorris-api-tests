package com.example;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

@DisplayName("Chuck Norris API")
class ChuckNorrisApiTest {

    private static RequestSpecification requestSpec;
    private static ResponseSpecification okJsonSpec;
    private static final String BASE_URI = "https://api.chucknorris.io";

    @BeforeAll
    static void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(BASE_URI)
                .setBasePath("/jokes")
                .setAccept(ContentType.JSON)
                .build();

        okJsonSpec = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }

    static Stream<String> allCategories() {
        return RestAssured.given()
                .baseUri(BASE_URI)
                .when()
                .get("/jokes/categories")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$", String.class)
                .stream();
    }

    @Nested
    @DisplayName("GET /jokes/random")
    class RandomJoke {

        @Test
        @DisplayName("Возвращает статус код 200 и непустой value")
        void returnsJokeWithText() {
            given().spec(requestSpec)
                    .when()
                    .get("/random")
                    .then()
                    .spec(okJsonSpec)
                    .body("value", not(emptyOrNullString()));
        }

        @ParameterizedTest(name = "Шутка из категории \"{0}\"")
        @MethodSource("com.example.ChuckNorrisApiTest#allCategories")
        @DisplayName("С параметром category возвращает шутку из этой категории")
        void returnsJokeFromCategory(String category) {
            given().spec(requestSpec)
                    .queryParam("category", category)
                    .when()
                    .get("/random")
                    .then()
                    .spec(okJsonSpec)
                    .body("categories", hasItem(category))
                    .body("value", not(emptyOrNullString()));
        }

        @Test
        @DisplayName("Несуществующая категория даёт 404")
        void unknownCategory_returns404() {
            given().spec(requestSpec)
                    .queryParam("category", "wasd")
                    .when()
                    .get("/random")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Ответ соответствует JSON-схеме шутки")
        void matchesJokeSchema() {
            given().spec(requestSpec)
                    .when()
                    .get("/random")
                    .then()
                    .spec(okJsonSpec)
                    .body(matchesJsonSchemaInClasspath("schemas/joke.json"));
        }
    }

    @Nested
    @DisplayName("GET /jokes/categories")
    class Categories {

        @Test
        @DisplayName("Возвращает непустой список с известными категориями")
        void returnsKnownCategories() {
            given().spec(requestSpec)
                    .when()
                    .get("/categories")
                    .then()
                    .spec(okJsonSpec)
                    .body("$", not(empty()))
                    .body("$", hasItems("dev", "movie", "food"));
        }

        @Test
        @DisplayName("Ответ соответствует JSON-схеме списка категорий")
        void matchesCategoriesSchema() {
            given().spec(requestSpec)
                    .when()
                    .get("/categories")
                    .then()
                    .spec(okJsonSpec)
                    .body(matchesJsonSchemaInClasspath("schemas/categories.json"));
        }
    }

    @Nested
    @DisplayName("GET /jokes/search")
    class Search {

        @Test
        @DisplayName("Возвращает total и непустой result")
        void returnsTotalAndResult() {
            given().spec(requestSpec)
                    .queryParam("query", "computer")
                    .when()
                    .get("/search")
                    .then()
                    .spec(okJsonSpec)
                    .body("total", greaterThan(0))
                    .body("result", hasSize(greaterThan(0)));
        }

        @Test
        @DisplayName("Найденные шутки содержат искомое слово")
        void resultsContainSearchedWord() {
            given().spec(requestSpec)
                    .queryParam("query", "computer")
                    .when()
                    .get("/search")
                    .then()
                    .spec(okJsonSpec)
                    .body("result.value", everyItem(containsStringIgnoringCase("computer")));
        }

        @Test
        @DisplayName("Запрос без совпадений даёт total = 0 и пустой result")
        void noMatches_returnsEmptyResult() {
            given().spec(requestSpec)
                    .queryParam("query", "wasd")
                    .when()
                    .get("/search")
                    .then()
                    .spec(okJsonSpec)
                    .body("total", equalTo(0))
                    .body("result", empty());
        }

        @Test
        @DisplayName("Слишком короткий запрос даёт 400")
        void tooShortQuery_returnsError() {
            given().spec(requestSpec)
                    .queryParam("query", "wa")
                    .when()
                    .get("/search")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Ответ соответствует JSON-схеме поиска")
        void matchesSearchSchema() {
            given().spec(requestSpec)
                    .queryParam("query", "computer")
                    .when()
                    .get("/search")
                    .then()
                    .spec(okJsonSpec)
                    .body(matchesJsonSchemaInClasspath("schemas/search.json"));
        }
    }

    @Nested
    @DisplayName("GET /jokes/{id}")
    class JokeById {

        @Test
        @DisplayName("Возвращает ту же шутку, что была получена из /random")
        void returnsSameJokeAsRandom() {
            JsonPath random = given().spec(requestSpec)
                    .when()
                    .get("/random")
                    .then()
                    .spec(okJsonSpec)
                    .extract().jsonPath();

            String id = random.getString("id");
            String value = random.getString("value");

            given().spec(requestSpec)
                    .when()
                    .get("/{id}", id)
                    .then()
                    .spec(okJsonSpec)
                    .body("id", equalTo(id))
                    .body("value", equalTo(value));
        }

        @Test
        @DisplayName("Несуществующий id даёт 404")
        void unknownId_returns404() {
            given().spec(requestSpec)
                    .when()
                    .get("/{id}", "no_such_id_123")
                    .then()
                    .statusCode(404);
        }
    }
}