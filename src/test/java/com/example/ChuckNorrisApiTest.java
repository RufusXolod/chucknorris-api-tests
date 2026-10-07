package com.example;
import java.util.List;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import static org.hamcrest.Matchers.*;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import com.example.client.JokesClient;
import com.example.model.Joke;
import com.example.model.SearchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Chuck Norris API")
class ChuckNorrisApiTest {

    private static final JokesClient client = new JokesClient();
    private static ResponseSpecification okJsonSpec;


    @BeforeAll
    static void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        okJsonSpec = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }

    static Stream<String> allCategories() {
        return client.getCategories()
                .then().statusCode(200)
                .extract().jsonPath().getList("$", String.class)
                .stream();
    }

    @Nested
    @DisplayName("GET /jokes/random")
    class RandomJoke {

        @Test
        @DisplayName("Возвращает статус код 200 и непустой value")
        void returnsJokeWithText() {
            client.getRandom().then().spec(okJsonSpec).body("value", not(emptyOrNullString()));
        }

        @ParameterizedTest(name = "Шутка из категории \"{0}\"")
        @MethodSource("com.example.ChuckNorrisApiTest#allCategories")
        @DisplayName("С параметром category возвращает шутку из этой категории")
        void returnsJokeFromCategory(String category) {
            Joke joke = client.getRandom(category).then().spec(okJsonSpec).extract().as(Joke.class);

            assertTrue(joke.categories().contains(category));
            assertFalse(joke.value().isBlank());
        }

        @Test
        @DisplayName("Несуществующая категория даёт 404")
        void unknownCategory_returns404() {
            client.getRandom("no_such_category_123").then().statusCode(404);
        }

        @Test
        @DisplayName("Ответ соответствует JSON-схеме шутки")
        void matchesJokeSchema() {
            client.getRandom().then().spec(okJsonSpec).body(matchesJsonSchemaInClasspath("schemas/joke.json"));
        }
    }

    @Nested
    @DisplayName("GET /jokes/categories")
    class Categories {

        @Test
        @DisplayName("Возвращает непустой список с известными категориями")
        void returnsKnownCategories() {
            List<String> categories = client.getCategories()
                    .then().spec(okJsonSpec)
                    .extract().jsonPath().getList("$", String.class);

            assertFalse(categories.isEmpty());
            assertTrue(categories.containsAll(List.of("dev", "movie", "food")));
        }

        @Test
        @DisplayName("Ответ соответствует JSON-схеме списка категорий")
        void matchesCategoriesSchema() {
            client.getCategories().then().spec(okJsonSpec).body(matchesJsonSchemaInClasspath("schemas/categories.json"));
        }
    }

    @Nested
    @DisplayName("GET /jokes/search")
    class Search {

        @Test
        @DisplayName("Возвращает total, непустой result и что они равны")
        void returnsTotalAndResult() {
            SearchResult found = client.search("computer").then().spec(okJsonSpec).extract().as(SearchResult.class);

            assertTrue(found.total() > 0);
            assertFalse(found.result().isEmpty());
            assertEquals(found.total(), found.result().size());
        }

        @Test
        @DisplayName("Найденные шутки содержат искомое слово")
        void resultsContainSearchedWord() {
            SearchResult found = client.search("computer").then().spec(okJsonSpec).extract().as(SearchResult.class);

            assertFalse(found.result().isEmpty());
            assertTrue(found.result().stream().allMatch(j -> j.value().toLowerCase().contains("computer")));
        }

        @Test
        @DisplayName("Запрос без совпадений даёт total = 0 и пустой result")
        void noMatches_returnsEmptyResult() {
            SearchResult found = client.search("wasd").then().spec(okJsonSpec).extract().as(SearchResult.class);

            assertEquals(0, found.total());
            assertTrue(found.result().isEmpty());
        }

        @Test
        @DisplayName("Слишком короткий запрос даёт 400")
        void tooShortQuery_returnsError() {
            client.search("wa").then().statusCode(400);
        }

        @Test
        @DisplayName("Ответ соответствует JSON-схеме поиска")
        void matchesSearchSchema() {
            client.search("computer").then().spec(okJsonSpec).body(matchesJsonSchemaInClasspath("schemas/search.json"));
        }
    }

    @Nested
    @DisplayName("GET /jokes/{id}")
    class JokeById {

        @Test
        @DisplayName("Возвращает ту же шутку, что была получена из /random")
        void returnsSameJokeAsRandom() {
            Joke random = client.getRandom()
                    .then().spec(okJsonSpec)
                    .extract().as(Joke.class);

            Joke byId = client.getById(random.id())
                    .then().spec(okJsonSpec)
                    .extract().as(Joke.class);

            assertEquals(random, byId);
        }

        @Test
        @DisplayName("Несуществующий id даёт 404")
        void unknownId_returns404() {
            client.getById("wasd_123").then().statusCode(404);
        }
    }
}