# DD Poker Wicket UI Tests

This directory contains Playwright end-to-end tests for the DD Poker website UI.

## Setup

1. Install Node.js dependencies:
```bash
cd code/pokerwicket
npm install
```

2. Install Playwright browsers:
```bash
npx playwright install
```

## Running the Server

Before running tests, start the Wicket server:

```bash
# From the project root
mvn -pl code/pokerwicket jetty:run
```

The server will start on http://localhost:8080

## Running Tests

### Run all tests
```bash
npm test
```

### Run tests with UI mode (interactive)
```bash
npm run test:ui
```

### Run tests in headed mode (see browser)
```bash
npm run test:headed
```

### Run specific test file
```bash
npx playwright test tests/navigation.spec.ts
```

### Run specific test
```bash
npx playwright test -g "logo should be 64px"
```

## Test Coverage

### Navigation Tests (`navigation.spec.ts`)

Tests covering the navigation UI updates:
- Logo size (64px)
- Header height (80px)
- Main navigation spacing (1rem gap)
- Secondary navigation positioning (no overlap)
- Responsive behavior (desktop vs mobile)
- Sticky header functionality
- Visual regression snapshots

## Debugging Failed Tests

View the HTML report:
```bash
npx playwright show-report
```

Run tests in debug mode:
```bash
npx playwright test --debug
```

## Visual Regression

The tests include visual regression checks that create baseline screenshots.

First run will create the baseline images in `tests/navigation.spec.ts-snapshots/`.

Subsequent runs compare against these baselines.

To update baselines:
```bash
npx playwright test --update-snapshots
```

## CI/CD Integration

These tests can be integrated into CI/CD pipelines. The configuration in `playwright.config.ts` includes CI-specific settings for retries and parallel execution.
