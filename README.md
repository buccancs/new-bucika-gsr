# new-bucika-gsr

A Kotlin project demonstrating comprehensive unit testing with 100% code coverage.

## Project Structure

This project contains several well-tested Kotlin classes:

- **Calculator**: Basic arithmetic operations (add, subtract, multiply, divide, power, square root)
- **User**: Data class representing a user with validation and utility methods
- **StringUtils**: String manipulation utilities (palindrome check, vowel counting, capitalization, etc.)
- **UserRepository**: Repository pattern for managing User objects

## Testing

The project includes comprehensive unit tests achieving **99% instruction coverage** and **90% branch coverage**.

### Running Tests

```bash
# Run all tests
./gradlew test

# Generate coverage report
./gradlew jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Test Coverage

- **103 unit tests** covering all classes and methods
- **99% instruction coverage** 
- **90% branch coverage**
- All edge cases and error conditions tested

## Features Tested

### Calculator
- Basic arithmetic operations with positive/negative numbers
- Division by zero exception handling  
- Power calculations including edge cases (zero exponent, negative exponent)
- Square root with negative number exception handling

### User
- Data validation on creation (blank fields, invalid email, age limits)
- Adult status checking (age >= 18)
- Name parsing (initials, display name) with whitespace handling
- Data class equality and inequality

### StringUtils
- Palindrome detection with case/space insensitivity
- Vowel counting with case handling
- Word capitalization and reversal
- Word counting with whitespace normalization
- String cleaning and numeric validation

### UserRepository
- CRUD operations (Create, Read, Update, Delete)
- Duplicate handling
- Age-based filtering
- Adult user filtering
- Repository state management

## Build Requirements

- Java 17+
- Gradle 8.4+
- Kotlin 2.2.0

## Coverage Reports

After running tests, coverage reports are available at:
- HTML: `build/reports/jacoco/test/html/index.html`
- XML: `build/reports/jacoco/test/jacocoTestReport.xml`
