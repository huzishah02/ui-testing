package playwrightLLM;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

public class BookstoreLLMTest {
    @Test
    void aiGeneratedEarbudsTest() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setRecordVideoDir(Paths.get("videos/"))
                    .setRecordVideoSize(1280, 720));

            Page page = context.newPage();
            page.navigate("https://depaul.bncollege.com/");

            // Locate the search box without AriaRole
            Locator searchBox = page.locator("input[aria-label='Search'], input[placeholder*='Search']");
            searchBox.first().click();
            searchBox.first().fill("earbuds");
            page.keyboard().press("Enter");

            page.waitForTimeout(4000);

            // Verify search results contain "earbuds"
            Assertions.assertTrue(page.content().toLowerCase().contains("earbuds"),
                    "Search results should mention 'earbuds'");

            context.close();
            browser.close();
        }
    }
}