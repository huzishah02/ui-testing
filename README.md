# SE333 – Assignment 5: UI Testing

## Overview

This project explores two different approaches to automating user interface (UI) testing using **Playwright** and **JUnit** in Java.

- **Manual Testing (playwrightTraditional):** Written completely by hand using Playwright’s API and JUnit.
- **AI-Assisted Testing (playwrightLLM):** Generated through natural language prompts to an AI agent (Playwright MCP-style process simulated through ChatGPT).

Both test suites focus on the same website:
**[DePaul University Bookstore](https://depaul.bncollege.com)**

---

## Manual UI Testing (playwrightTraditional)

For the manual approach, I scripted the entire bookstore purchase process step by step.  
The test automates searching for **“earbuds”**, filtering by brand, verifying product details, adding an item to the cart, and proceeding through checkout while checking that totals and tax values appear correctly.

This process took more time and required several iterations to get right. I had to:

- Carefully write accurate selectors, assertions, and wait conditions.  
- Manage asynchronous events, page loads, and dynamic elements manually.  
- Debug and rerun the test multiple times until every step passed successfully.

Even though it was time-consuming, this method was **highly reliable**. Once it worked, it ran smoothly from start to finish and gave consistent, correct results.  

---

## AI-Assisted UI Testing (playwrightLLM)

The AI-assisted approach was generated using a simple text prompt describing the same workflow in natural language (for example:  
> “Test search for earbuds, filter by color, add to cart, and verify the cart shows 1 item.”)

The AI then produced the `BookstoreLLMTest.java` file automatically.  
This version was **much shorter** and required almost no manual setup — the AI handled most of the structure.  
However, when I ran it, the test **failed** because of incorrect imports and missing roles in the locators.  
It still compiled and executed, but the assertions didn’t fully pass.

This showed me that while AI can quickly generate runnable test code, it **still needs human oversight** to refine selectors, fix small issues, and ensure full functionality.

---

## Reflection and Comparison

Working with both methods made it clear that manual testing and AI-assisted testing each have their strengths:

| Aspect | Manual Testing (playwrightTraditional) | AI-Assisted Testing (playwrightLLM) |
|--------|----------------------------------------|------------------------------------|
| **Ease of Setup** | Slower, requires step-by-step coding | Extremely fast, just needs a prompt |
| **Accuracy** | Very precise and dependable | Incomplete, sometimes misses key elements |
| **Reliability** | Fully passed all assertions | Compiled but failed during execution |
| **Maintenance** | Requires manual updates | Easier to regenerate but less stable |
| **Overall Usefulness** | Best for production-level, reliable testing | Great for quick prototypes or ideas |

Overall, I found that the manual approach gave me much more **control and consistency**, while the AI-assisted approach was a faster way to **prototype and learn**.  
AI can definitely help speed up testing in the future, but for now, human validation is still essential to make sure everything actually works the way it should.

---

## Repository and Automation

**GitHub Repository:**  
[https://github.com/huzishah02/ui-testing](https://github.com/huzishah02/ui-testing)

Both test suites (`playwrightTraditional` and `playwrightLLM`) are included.  
GitHub Actions runs the tests automatically whenever new code is pushed.  

The manual test passes completely, while the AI-generated test compiles but fails at runtime — illustrating both the potential and current limitations of AI-driven UI test generation.