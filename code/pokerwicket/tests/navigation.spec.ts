import { test, expect } from '@playwright/test';

test.describe('Navigation UI Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('logo should be 64px in size', async ({ page }) => {
    const logo = page.locator('.logo-icon');
    await expect(logo).toBeVisible();

    const boundingBox = await logo.boundingBox();
    expect(boundingBox?.width).toBe(64);
    expect(boundingBox?.height).toBe(64);
  });

  test('header should be 80px tall', async ({ page }) => {
    const headerTop = page.locator('.header-top');
    await expect(headerTop).toBeVisible();

    const boundingBox = await headerTop.boundingBox();
    expect(boundingBox?.height).toBeGreaterThanOrEqual(80);
  });

  test('main navigation items should have reduced spacing', async ({ page }) => {
    const mainNav = page.locator('.main-nav');
    await expect(mainNav).toBeVisible();

    // Check that gap is applied (visual check via computed style)
    const gapValue = await mainNav.evaluate((el) => {
      return window.getComputedStyle(el).gap;
    });

    // gap: 1rem should be 16px in most browsers
    expect(gapValue).toBe('16px');
  });

  test('secondary navigation should not overlap header', async ({ page }) => {
    // Navigate to About page which has secondary navigation
    await page.goto('/about');

    const header = page.locator('.header');
    const secondaryNav = page.locator('.secondary-nav');

    await expect(header).toBeVisible();
    await expect(secondaryNav).toBeVisible();

    const headerBox = await header.boundingBox();
    const secondaryBox = await secondaryNav.boundingBox();

    // Secondary nav should be within or at the bottom of the header
    if (headerBox && secondaryBox) {
      expect(secondaryBox.y).toBeGreaterThanOrEqual(headerBox.y);
      expect(secondaryBox.y + secondaryBox.height).toBeLessThanOrEqual(headerBox.y + headerBox.height + 5); // 5px tolerance
    }
  });

  test('secondary navigation has proper spacing from top', async ({ page }) => {
    await page.goto('/about');

    const secondaryNav = page.locator('.secondary-nav');
    await expect(secondaryNav).toBeVisible();

    const marginTop = await secondaryNav.evaluate((el) => {
      return window.getComputedStyle(el).marginTop;
    });

    // margin-top: 0.25rem should be 4px
    expect(marginTop).toBe('4px');
  });

  test('desktop navigation layout on wide screen', async ({ page, viewport }) => {
    // Set desktop viewport
    await page.setViewportSize({ width: 1280, height: 720 });

    const mainNav = page.locator('.main-nav');
    const mobileToggle = page.locator('.mobile-menu-toggle');

    // Main nav should be visible as flex
    await expect(mainNav).toBeVisible();
    const display = await mainNav.evaluate((el) => window.getComputedStyle(el).display);
    expect(display).toBe('flex');

    // Mobile toggle should not be visible
    const toggleDisplay = await mobileToggle.evaluate((el) => window.getComputedStyle(el).display);
    expect(toggleDisplay).toBe('none');
  });

  test('mobile navigation layout on narrow screen', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });

    const mainNav = page.locator('.main-nav');
    const mobileToggle = page.locator('.mobile-menu-toggle');

    // Mobile toggle should be visible
    await expect(mobileToggle).toBeVisible();

    // Main nav should be hidden initially
    const display = await mainNav.evaluate((el) => window.getComputedStyle(el).display);
    expect(display).toBe('none');
  });

  test('secondary navigation items are readable', async ({ page }) => {
    await page.goto('/about');

    const secondaryLinks = page.locator('.secondary-nav-link');
    const count = await secondaryLinks.count();

    // Should have at least one secondary nav item
    expect(count).toBeGreaterThan(0);

    // Check first item is visible and has text
    const firstLink = secondaryLinks.first();
    await expect(firstLink).toBeVisible();
    const text = await firstLink.textContent();
    expect(text).toBeTruthy();
  });

  test('header stays sticky on scroll', async ({ page }) => {
    await page.goto('/about');

    const header = page.locator('.header');
    const position = await header.evaluate((el) => window.getComputedStyle(el).position);
    expect(position).toBe('sticky');

    const top = await header.evaluate((el) => window.getComputedStyle(el).top);
    expect(top).toBe('0px');

    // Scroll down and verify header is still visible
    await page.evaluate(() => window.scrollBy(0, 500));
    await expect(header).toBeVisible();
  });

  test('visual regression - header appears correctly', async ({ page }) => {
    await page.goto('/');

    const header = page.locator('.header');
    await expect(header).toBeVisible();

    // Take screenshot of header for manual verification if needed
    await expect(header).toHaveScreenshot('header-desktop.png', {
      maxDiffPixels: 100,
    });
  });

  test('visual regression - about page secondary nav', async ({ page }) => {
    await page.goto('/about');

    const header = page.locator('.header');
    await expect(header).toBeVisible();

    // Take screenshot of header with secondary nav
    await expect(header).toHaveScreenshot('header-with-secondary-nav.png', {
      maxDiffPixels: 100,
    });
  });
});
