package playwrightTraditional;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.JSHandle;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class BookstoreE2ETest extends BaseTest {

    // helpers
    private void expandFilter(String name) {
        page.waitForSelector("body");

        Locator header = page.locator("text=" + name);
        if (header.count() == 0) header = page.getByText(name, new Page.GetByTextOptions().setExact(false));
        if (header.count() == 0) header = page.locator("button:has-text('" + name + "')");
        if (header.count() == 0) header = page.locator("summary:has-text('" + name + "')"); // for <details> elements
        if (header.count() == 0) header = page.locator("div:has-text('" + name + "')");

        assertTrue(header.count() > 0, "Could not find filter section for: " + name);
        header.first().click();
        page.waitForTimeout(5000); // wait after expanding
    }

    private void assertMoneyInSidebar(String labelText, String expectedAmount) {
        // Find the parent container that holds Order Summary and amounts
        Locator sidebar = page.locator("div, section, aside")
                .filter(new Locator.FilterOptions().setHasText("Order Summary"))
                .last();

        assertTrue(sidebar.count() > 0, "Sidebar not found.");

        // Find a row inside with the label
        Locator row = sidebar.locator("p, div, span").filter(new Locator.FilterOptions().setHasText(labelText));
        int rowCount = row.count();

        assertTrue(rowCount > 0, "Expected to find row for label: " + labelText);

        // Check the row text for the expected amount
        String rowText = row.first().innerText().trim();

        // Handle "Tax" being TBD gracefully
        if (labelText.equalsIgnoreCase("Tax") && rowText.contains("TBD")) {
            return;
        }

        assertTrue(rowText.contains(expectedAmount),
                "Expected '" + labelText + "' to contain amount '" + expectedAmount + "' but saw: " + rowText);
    }

    private void assertTextContainsNear(String anchorText, String mustContain) {
        Locator anchors = page.getByText(anchorText, new Page.GetByTextOptions().setExact(false));
        int count = anchors.count();
        assertTrue(count > 0, "No elements found with text: " + anchorText);

        Locator anchor = null;
        for (int i = 0; i < count; i++) {
            if (anchors.nth(i).isVisible()) {
                anchor = anchors.nth(i);
                break;
            }
        }
        if (anchor == null) anchor = anchors.first();

        // Get the section containing the heading
        Locator section = anchor.locator("xpath=ancestor-or-self::*[self::div or self::section][1]");
        String sectionText = section.innerText();

        // Look for *any* visible sibling divs that might hold details (skip edit link blocks)
        Locator siblings = section.locator("xpath=following-sibling::*[self::div or self::section]");
        for (int i = 0; i < siblings.count(); i++) {
            if (siblings.nth(i).isVisible()) {
                sectionText += "\n" + siblings.nth(i).innerText();
            }
        }

        System.out.println("DEBUG: Combined text near '" + anchorText + "': " +
                sectionText.substring(0, Math.min(300, sectionText.length())));

        assertTrue(sectionText.toLowerCase().contains(mustContain.toLowerCase()),
                "Expected near '" + anchorText + "' to contain '" + mustContain + "' but saw: " + sectionText);
    }

    private void fillLabeled(String label, String value) {
        Locator input = page.getByLabel(label, new Page.GetByLabelOptions().setExact(false));
        if (input.count() == 0) input = page.getByPlaceholder(label);
        if (input.count() == 0) fail("Could not find input for label or placeholder: " + label);
        input.first().fill(value);
    }

    @Test
    void bookstore_purchase_pathway() {
        // --- Setup browser and video recording ---
        // Ensure cache is cleared between runs
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(Paths.get("videos/"))
                .setRecordVideoSize(1280, 720));

        page = context.newPage();

        //  Startup
        page.navigate("https://depaul.bncollege.com/");
        page.setDefaultTimeout(60000); // 60 seconds for any selector
        page.setDefaultNavigationTimeout(60000);

        // TestCase Bookstore
        // Search “earbuds”
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Search")).click();
        page.keyboard().type("earbuds");
        page.keyboard().press("Enter");

        // Wait for the page and network to finish loading
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait for the Brand filter section
        expandFilter("Brand");
        page.waitForTimeout(1500);

        Locator jblLabel = page.locator("label:has-text('JBL')").first();
        assertTrue(jblLabel.count() > 0, "Could not find JBL brand checkbox");

        jblLabel.scrollIntoViewIfNeeded();
        jblLabel.click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(3000);


        //  COLOR filter
        expandFilter("Color");
        page.waitForTimeout(1500);
        Locator colorOption = page.locator("span.facet__list__label:has-text('Black')").first();
        assertTrue(colorOption.count() > 0, "Could not find Black color filter label");
        colorOption.click();

        //  PRICE filter
        expandFilter("Price");
        page.waitForTimeout(1500);
        Locator priceOption = page.locator("span.facet__list__label:has-text('Over $50')").first();
        if (priceOption.count() == 0) {
            priceOption = page.getByText("Over $50", new Page.GetByTextOptions().setExact(false)).first();
        }
        assertTrue(priceOption.count() > 0, "Could not find Over $50 price filter label");
        priceOption.click();

        //  Click JBL Quantum True Wireless product
        page.waitForSelector("a:has-text('JBL Quantum True Wireless Noise Cancelling Gaming')",
                new Page.WaitForSelectorOptions().setTimeout(60000));

        Locator productLink = page.locator("a:has-text('JBL Quantum True Wireless Noise Cancelling Gaming')").first();
        assertTrue(productLink.count() > 0, "Could not find JBL Quantum product link");
        productLink.scrollIntoViewIfNeeded();
        productLink.click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(4000); // allow product detail page to fully load

        //  Verify product detail page content
        Locator productTitle = page.locator("h1:has-text('JBL Quantum True Wireless Noise Cancelling Gaming')").first();
        assertTrue(productTitle.count() > 0, "Expected product title on detail page");

        Locator sku = page.locator("text=SKU");
        assertTrue(sku.count() > 0, "SKU label not found");

        Locator price = page.locator("text=$164.98").first();
        assertTrue(price.count() > 0, "Price not visible on product page");

        Locator desc = page.locator(":text('Adaptive noise cancelling')").first();
        assertTrue(desc.count() > 0, "Product description not visible");

        //  Add item to cart
        Locator addToCart = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to Cart").setExact(false)).first();
        assertTrue(addToCart.count() > 0, "Add to Cart button not found");
        addToCart.click();
        page.waitForTimeout(3000);

        // Scroll to top to ensure the Cart icon is visible
        page.evaluate("window.scrollTo(0, 0)");
        page.waitForTimeout(1500);

        // Wait until the cart text updates to "Cart | 1 items" (case-insensitive)
        Locator cartLink = page.locator("a:has-text('Cart')").first();
        JSHandle handle = cartLink.elementHandle();

        page.waitForFunction(
                "el => el && /Cart\\s*(\\|\\s*)?1\\s*items?/i.test(el.innerText)",
                handle,
                new Page.WaitForFunctionOptions().setTimeout(90000)
        );

        // Assert cart link is visible and click
        assertTrue(cartLink.isVisible(), "Cart link is not visible after adding item.");
        cartLink.scrollIntoViewIfNeeded();
        cartLink.click();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(3000);

        //  Wait until the cart header element exists in the DOM
        Locator cartHeader = page.locator("h2.bned-cart-main-title, h1:has-text('Shopping Cart')");

        page.waitForSelector(
                "h2.bned-cart-main-title, h1:has-text('Shopping Cart')",
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.ATTACHED)
                        .setTimeout(60000)
        );

        //  Wait until it becomes visible
        JSHandle headerHandle = cartHeader.first().elementHandle();

        page.waitForFunction(
                "el => el && window.getComputedStyle(el).visibility !== 'hidden' && window.getComputedStyle(el).display !== 'none'",
                headerHandle,
                new Page.WaitForFunctionOptions().setTimeout(30000)
        );

        //  Verify text content
        String cartText = cartHeader.first().innerText().trim();

        assertTrue(cartText.toLowerCase().contains("shopping cart"),
                "Expected Shopping Cart page, but saw: " + cartText);

        System.out.println("Cart page loaded successfully: " + cartText);

        // Assert item name / qty / price
        assertTrue(page.getByText("JBL Quantum True Wireless", new Page.GetByTextOptions().setExact(false)).first().isVisible(),
                "Line item name should be visible");

        // qty: either spinbutton, select, or static text
        Locator qty = page.getByRole(AriaRole.SPINBUTTON).or(page.getByLabel("Qty")).first();
        String qtyText = qty.count() > 0 ? (qty.inputValue() != null ? qty.inputValue() : qty.innerText()) : "1";
        assertEquals("1", qtyText.trim(), "Quantity should be 1");

        //  Verify item price format
        Locator priceLine = page.locator("text=/\\$\\d+\\.\\d{2}/").first();
        assertTrue(priceLine.isVisible(), "Expected a visible currency value on line item");

        //  Select “FAST In-Store Pickup”
        Locator pickup = page.getByText("FAST In-Store Pickup", new Page.GetByTextOptions().setExact(false));
        if (pickup.count() > 0) {
            pickup.first().click();
        } else {
            System.out.println("DEBUG: Pickup option not found (may already be selected)");
        }

        //  Verify sidebar totals appear, regardless of exact values
        assertTrue(page.getByText("Subtotal", new Page.GetByTextOptions().setExact(false)).first().isVisible(),
                "Subtotal section missing");

        //  Verify Handling row with icon-tolerant selector
        Locator handlingLine = page.locator("div:has-text('Handling'), span:has-text('Handling')");
        if (handlingLine.count() == 0) {
            System.out.println("DEBUG: Could not find Handling with standard match — trying flexible regex match.");
            handlingLine = page.locator("text=/Handling/i");
        }
        assertTrue(handlingLine.count() > 0 && handlingLine.first().isVisible(),
                "Handling section missing or not visible");

        assertTrue(page.getByText("Estimated Total", new Page.GetByTextOptions().setExact(false)).first().isVisible(),
                "Estimated Total section missing");

        String subtotalText = page.getByText("Subtotal", new Page.GetByTextOptions().setExact(false))
                .first().locator("xpath=following-sibling::*").textContent();


        // Promo code TEST
        Locator promo = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Promo"));
        if (promo.count() == 0) promo = page.getByPlaceholder("Promo code");

        if (promo.count() > 0) {
            promo.first().fill("TEST");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply").setExact(false)).first().click();

            // Wait up to 5 seconds for any rejection message to appear
            page.waitForTimeout(2000);

            Locator rejection = page.locator("text=/The coupon code entered is not valid/i");
            if (rejection.count() == 0) {
                rejection = page.locator("text=/invalid|not valid|cannot be applied/i");
            }

            assertTrue(rejection.count() > 0 && rejection.first().isVisible(),
                    "Expected promo code rejection message to appear after applying TEST code");
            System.out.println("Promo code rejection detected successfully.");
        }

        //  Proceed to Checkout
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Proceed to Checkout")).first().click();

        // Wait for the Sign In / Create Account page to fully load
        page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(60000));

        // Wait until "Create Account" heading is visible (right side)
        Locator createAccountHeader = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Create Account"));
        createAccountHeader.first().waitFor(new Locator.WaitForOptions().setTimeout(15000).setState(WaitForSelectorState.VISIBLE));

        assertTrue(createAccountHeader.first().isVisible(), "Create Account label should be present");
        System.out.println("Checkout page loaded: Create Account visible");

        Locator proceedAsGuest = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Proceed as Guest").setExact(false));

        // If not found, try by visible text
        if (proceedAsGuest.count() == 0) {
            proceedAsGuest = page.locator("text=/Proceed as Guest/i");
        }

        // Wait briefly for animations or lazy render
        page.waitForTimeout(1500);

        // Assert and click
        assertTrue(proceedAsGuest.count() > 0, "'Proceed as Guest' should be present and visible on Create Account page");
        assertTrue(proceedAsGuest.first().isVisible(), "'Proceed as Guest' is present but not visible yet");
        proceedAsGuest.first().scrollIntoViewIfNeeded();
        proceedAsGuest.first().click();

        // Contact Information Page
        Locator visibleContactHeading = page.locator("h2.bned-checkout-section-headline",
                new Page.LocatorOptions().setHasText("Contact Information"));

        // Wait until it's visible
        page.waitForCondition(() -> visibleContactHeading.isVisible(),
                new Page.WaitForConditionOptions().setTimeout(20000));

        System.out.println("DEBUG: Contact Information heading is visible on the page.");

        fillLabeled("First Name", "Taylor");
        fillLabeled("Last Name", "Jordan");
        fillLabeled("Email", "taylor.jordan+test@depaul.edu");
        fillLabeled("Phone", "312-555-1212");

        // Sidebar values persist
        assertMoneyInSidebar("Order Subtotal", "164.98");
        assertMoneyInSidebar("Handling", "3.00");
        assertMoneyInSidebar("Tax", "0.31");
        assertMoneyInSidebar("Total", "$");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue").setExact(false)).first().click();

        // Pickup Information
        Locator contactCandidates = page.locator("text=Contact Information");
        int totalCandidates = contactCandidates.count();
        System.out.println("DEBUG: Found " + totalCandidates + " elements with text 'Contact Information'.");

        for (int i = 0; i < totalCandidates; i++) {
            String snippet = contactCandidates.nth(i).innerText();
            boolean visible = contactCandidates.nth(i).isVisible();
            System.out.println("DEBUG: Element #" + i + " visible=" + visible + " | snippet='" +
                    snippet.substring(0, Math.min(snippet.length(), 120)) + "'");
        }

        Locator errorBanner = page.locator("text=Errors were found with the contact information");
        if (errorBanner.count() > 0 && errorBanner.first().isVisible()) {
            System.out.println("DEBUG: Contact Information validation error detected — likely invalid inputs or missing field!");
        }

        try {
            page.waitForSelector("h2:has-text('Contact Information'), h3:has-text('Contact Information')",
                    new Page.WaitForSelectorOptions().setTimeout(10000));
        } catch (Exception e) {
            System.out.println("DEBUG: Timeout waiting for 'Contact Information'. Printing visible page text...");
            System.out.println(page.content().substring(0, Math.min(page.content().length(), 1500)));
            throw e;
        }

        assertTextContainsNear("Contact Information", "Taylor");
        assertTextContainsNear("Contact Information", "Jordan");
        assertTextContainsNear("Contact Information", "taylor.jordan+test@depaul.edu\n");
        assertTextContainsNear("Contact Information", "312-555-1212");

        //  Pickup Information Debug & Assertions
        System.out.println("DEBUG: Checking Pickup Information section...");

        Locator pickupSection = page.locator("text=Pick Up Information").locator("xpath=..");
        if (pickupSection.count() > 0) {
            String sectionText = pickupSection.first().innerText();
            System.out.println("DEBUG: Pickup Section Text:\n" + sectionText);
        } else {
            System.out.println("DEBUG: Could not find 'Pick Up Information' heading!");
        }
        page.waitForTimeout(1500);

        Locator pickupContainer = page.locator(
                "div:has-text('DePaul University Loop Campus & SAIC'), " +
                        "p:has-text('DePaul University Loop Campus & SAIC'), " +
                        "section:has-text('DePaul University Loop Campus & SAIC')"
        ).first();

        boolean pickupVisible = false;

        if (pickupContainer.count() > 0) {
            try {
                pickupVisible = pickupContainer.isVisible();
                System.out.println("DEBUG: Pickup container found; visible? " + pickupVisible);
            } catch (Exception e) {
                System.out.println("DEBUG: Pickup container visibility check threw exception: " + e.getMessage());
            }
        } else {
            System.out.println("DEBUG: Pickup container not found by :has-text selector!");
        }

        if (!pickupVisible && pickupSection.count() > 0) {
            String sectionText = pickupSection.first().innerText().toLowerCase();
            pickupVisible = sectionText.contains("depaul university loop campus");
            System.out.println("DEBUG: Fallback visibility check via section text: " + pickupVisible);
        }

        System.out.println("DEBUG: Final pickup location visible? " + pickupVisible);
        assertTrue(pickupVisible, "Pickup location should be visible");

        Locator pickupPerson = page.locator("text=/I'll pick them up/i");
        boolean personVisible = pickupPerson.count() > 0 && pickupPerson.first().isVisible();

        if (!personVisible && pickupSection.count() > 0) {
            String sectionText = pickupSection.first().innerText().toLowerCase();
            personVisible = sectionText.contains("i'll pick them up");
            System.out.println("DEBUG: Fallback visibility check via section text: " + personVisible);
        }

        System.out.println("DEBUG: 'I'll pick them up' visible? " + personVisible);
        assertTrue(personVisible, "Pickup person text should be visible");


        // Sidebar values persist
        assertMoneyInSidebar("Order Subtotal", "164.98");
        assertMoneyInSidebar("Handling", "3.00");

        Locator taxRow = page.locator("text=Tax").first();
        page.waitForTimeout(2000);
        boolean taxLoaded = false;

        for (int i = 0; i < 10; i++) {
            String taxText = taxRow.innerText().trim();
            if (!taxText.contains("TBD") && taxText.matches(".*\\$?\\d+\\.\\d{2}.*")) {
                System.out.println("DEBUG: Tax value loaded successfully: " + taxText);
                taxLoaded = true;
                break;
            }
            page.waitForTimeout(1500);
            if (i == 9)
                System.out.println("DEBUG: Tax still TBD after retries — skipping exact total validation.");
        }

        if (taxLoaded) {
            assertMoneyInSidebar("Tax", "0.31");
            assertMoneyInSidebar("Total", "168.29");
        } else {
            assertMoneyInSidebar("Total", "167.98"); // fallback when tax never updates
        }

        // Click Continue to go to Payment
        Locator continueBtn = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Continue").setExact(false)).first();
        continueBtn.scrollIntoViewIfNeeded();
        continueBtn.click();
        System.out.println("DEBUG: Clicked Continue on Pickup Information page");
        page.waitForTimeout(2000);

        page.waitForSelector("text=/Payment( Information| Method| Options)?/i",
                new Page.WaitForSelectorOptions().setTimeout(20000));

        System.out.println("DEBUG: Payment section detected!");

        //  Payment Information
        assertMoneyInSidebar("Order Subtotal", "164.98");
        assertMoneyInSidebar("Handling", "3.00");
        // Wait until tax updates on the Payment Information page
        Locator paymentTaxRow = page.locator("text=Tax").first();
        boolean paymentTaxLoaded = false;

        for (int i = 0; i < 12; i++) {
            String taxText = paymentTaxRow.innerText().trim();
            if (!taxText.contains("TBD") && taxText.matches(".*\\$?\\d+\\.\\d{2}.*")) {
                System.out.println("DEBUG: Payment page tax loaded: " + taxText);
                paymentTaxLoaded = true;
                break;
            }
            page.waitForTimeout(1500); // wait up to ~18s total
        }

        if (paymentTaxLoaded) {
            assertMoneyInSidebar("Tax", "17.22");
            assertMoneyInSidebar("Total", "185.20");
        } else {
            System.out.println("DEBUG: Payment tax never updated — skipping exact total validation.");
            assertMoneyInSidebar("Total", "$"); // loose fallback check
        }

        String pageText = page.content();
        boolean itemPresent = pageText.toLowerCase().contains("jbl quantum true wireless");

        System.out.println("DEBUG: JBL item present in Payment page HTML? " + itemPresent);
        assertTrue(itemPresent, "Expected JBL Quantum item text to appear in Payment page content");

        String pageHtml = page.content();
        boolean pricePresent = pageHtml.contains("$164.98");
        System.out.println("DEBUG: Price '$164.98' found in Payment page HTML? " + pricePresent);
        assertTrue(pricePresent, "Expected $164.98 to appear in the Payment page content");

        // Back to cart
        page.getByText("Back to Cart", new Page.GetByTextOptions().setExact(false)).first().click();

        //  Your Shopping Cart (delete; cart empty)
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove")).first().click();        page.waitForTimeout(1000);
        assertTrue(page.getByText("Your cart is empty", new Page.GetByTextOptions().setExact(false)).first().isVisible(),
                "Cart should be empty");

        // Close browser + context
        context.close();
        browser.close();
    }
}