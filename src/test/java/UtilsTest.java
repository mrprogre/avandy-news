import com.avandy.news.utils.Common;
import com.avandy.news.utils.JaroWinklerDistance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {
    private final JaroWinklerDistance jwd = new JaroWinklerDistance();

    @Test
    public void checkJaroWinklerDistanceCompare() {
        String s1 = "CRATE", s2 = "TRACE", s3 = "DwAyNE", s4 = "DuANE";
        assertEquals(73, jwd.compare(s1, s2));
        assertEquals(82, jwd.compare(s3, s4));
    }

    @Test
    public void checkJaroWinklerCompare() {
        String[] words = {"атаке", "атаку", "атаки", "атак"};
        for (String word : words) {
            for (String s : words) {
                int value = jwd.compare(word, s);
                if (value != 100)
                    assertTrue(value > 80);
            }
        }
    }

    @Test
    public void checkRssTest() {
        assertTrue(Common.checkRss("https://www.agroinvestor.ru/feed/public-agronews.xml"));
        assertFalse(Common.checkRss("wrong.xml"));
    }

}
