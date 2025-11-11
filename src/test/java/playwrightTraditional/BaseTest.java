package playwrightTraditional;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {
    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    void beforeAll() {
        playwright = Playwright.create();
        BrowserType.LaunchOptions launch = new BrowserType.LaunchOptions()
                .setHeadless(true); // set true on CI if needed
        browser = playwright.chromium().launch(launch);
    }

    @BeforeEach
    void beforeEach() {
        // New context per test => clears cache/cookies so “TBD” taxes and cart state are correct
        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(Paths.get("videos"))
                .setRecordVideoSize(1280, 720)
        );
        page = context.newPage();
    }

    @AfterEach
    void afterEach() {
        if (context != null) context.close(); // closing context finalizes the video file
    }

    @AfterAll
    void afterAll() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
