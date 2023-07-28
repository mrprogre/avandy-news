import com.avandy.news.database.JdbcQueries;
import com.avandy.news.database.SQLite;
import com.avandy.news.utils.Login;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JdbcQueriesTest {
    private final JdbcQueries jdbcQueries = new JdbcQueries();

    @BeforeAll
    static void beforeAll() {
        new SQLite().openConnection();
        Login.userId = 0;
        Login.username = "test";
    }

    @AfterAll
    static void afterAll() {
        new SQLite().closeConnection();
    }

    @Test
    public void checkSources() {
        assertEquals(7, jdbcQueries.getSources("all").size(),
                "Неправильное общее количество источников");
        assertEquals(6, jdbcQueries.getSources("active").size(),
                "Неправильное количество активных источников");
    }

    @Test
    public void checkKeywords() {
        assertEquals(4, jdbcQueries.getKeywords(2).size(),
                "Неправильное общее количество ключевых слов");
        assertEquals(3, jdbcQueries.getKeywords(1).size(),
                "Неправильное количество активных ключевых слов");
    }

    @Test
    public void shouldGetSetting() {
        assertNotNull(jdbcQueries.getSetting("interval"),
                "Получение настроек недоступно");
    }


}
