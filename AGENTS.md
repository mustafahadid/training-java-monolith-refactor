# AI Agent Instructions: Java Monolith Refactoring Project

## Project Overview

This is a **legacy Jakarta EE billing platform** designed for training exercises in modernizing monolithic Java applications. The codebase intentionally demonstrates anti-patterns and technical debt typical of legacy enterprise Java systems.

**Core Documentation:**
- [README.md](README.md) - Features, domain model, architecture overview
- [SETUP.md](SETUP.md) - Development environment setup, prerequisites

## Build & Run Commands

```bash
# Build project
./gradlew build

# Development mode (auto-reload)
./liberty-dev.sh        # Linux/macOS
liberty-dev.bat         # Windows

# Production deployment
./gradlew libertyStart
./gradlew libertyStop

# Access application
# http://localhost:9080/big-bad-monolith/
```

## Architecture Summary

**3-Tier Monolith:**
- **Presentation:** JSP pages with scriptlets (`src/main/webapp/*.jsp`)
- **Business Logic:** Service layer (`service/`) - thin, incomplete
- **Data Access:** DAO pattern with raw JDBC (`dao/`)

**Technology Stack:**
- Java 21, Jakarta EE 10, WebSphere Liberty
- Derby embedded database (`./data/bigbadmonolith/`)
- Joda-Time for date/time (legacy)
- No dependency injection framework
- No ORM/JPA - raw JDBC only

**Key Domain Entities:**
- `User` - Employees who log hours
- `Customer` - Billed clients
- `BillingCategory` - Work types with hourly rates
- `BillableHour` - Time entries linking users, customers, categories

## Code Conventions

### Naming Patterns
- **Entities:** `Customer`, `User`, `BillableHour` (plain nouns)
- **DAOs:** `{Entity}DAO` (e.g., `CustomerDAO`)
- **Services:** `{Purpose}Service` (e.g., `BillingService`)
- **Methods:** CRUD verbs: `save()`, `findById()`, `findAll()`, `update()`, `delete()`
- **Variables:** camelCase throughout

### Package Structure
```
com.sourcegraph.demo.bigbadmonolith/
├── dao/          # Data access layer - JDBC operations
├── entity/       # POJOs (no JPA annotations)
├── service/      # Business logic (incomplete)
└── util/         # Helper utilities
```

### JDBC Resource Management Pattern
Always use try-with-resources for connections:
```java
try (Connection conn = LibertyConnectionManager.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    // Execute query
    // Auto-closes in reverse order: stmt, then conn
}
```

**Never cache connections** - get fresh per method. Liberty's DataSource handles pooling.

### Date/Time Handling
Uses Joda-Time (legacy library):
```java
// In entities
private LocalDate dateLogged;           // Joda-Time
private DateTime createdAt;             // Joda-Time

// DAO conversion patterns
stmt.setDate(n, new Date(localDate.toDateTimeAtStartOfDay().getMillis()));
LocalDate.fromDateFields(rs.getDate("date_logged"));
```

**Migration Opportunity:** Replace Joda-Time with `java.time.*` (Java 8+ API).

## Critical Technical Patterns

### 1. Dual Database Connection Mode

**Two connection managers coexist:**

**LibertyConnectionManager** (preferred in production):
- JNDI DataSource: `jdbc/DefaultDataSource`
- Managed by Liberty server
- Configured in [src/main/liberty/config/server.xml](src/main/liberty/config/server.xml)

**ConnectionManager** (fallback for development):
- Embedded Derby: `jdbc:derby:./data/bigbadmonolith;create=true`
- Static initializer creates schema on class load
- Direct JDBC connections

**Connection Acquisition Pattern:**
```java
LibertyConnectionManager.getConnection()  // Auto-detects mode
```

**Important:** [StartupListener.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/StartupListener.java) determines mode at startup. Don't call `ConnectionManager.shutdown()` in Liberty mode.

### 2. Transaction Management

**⚠️ NO TRANSACTION BOUNDARIES** - All operations use auto-commit mode.

Each DAO method commits immediately:
```java
// ANTI-PATTERN: Not atomic
public Map<String, Object> generateCustomerBill(Long customerId) {
    Customer customer = customerDAO.findById(customerId);        // Commit 1
    List<BillableHour> hours = billableHourDAO.findByCustomerId(customerId);  // Commit 2
    // If crash occurs here, previous queries already committed
}
```

**When Refactoring:** Wrap service layer methods in programmatic transactions or use declarative transactions.

### 3. Error Handling Inconsistency

**Three different patterns exist:**

1. **RuntimeException wrapping** (UserDAO):
   ```java
   catch (SQLException e) {
       throw new RuntimeException("Failed to save user", e);
   }
   ```

2. **Checked exceptions** (CustomerDAO):
   ```java
   public Customer save(Customer customer) throws SQLException
   ```

3. **String error messages** (BillingService):
   ```java
   String errors = validateBillableHour(billableHour);
   if (!errors.isEmpty()) return errors;
   ```

**Recommendation:** Standardize on unchecked exceptions or use Result/Either pattern.

### 4. Static Initializers Gotcha

`ConnectionManager` has static block that executes on first class reference:
```java
static {
    Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
    initializeDatabase();  // Creates tables
}
```

**Impact:** Schema creation happens automatically but can fail silently if permissions missing.

## Anti-Patterns & Technical Debt

### JSP Scriptlet Hell
All JSP files (`*.jsp`) mix presentation with business logic:

**Anti-patterns present:**
- DAO instantiation directly in JSPs
- Raw JDBC queries in presentation layer
- Business logic calculations in scriptlets
- No input validation or sanitization
- Exception messages exposed to users
- No JSTL/EL usage - pure scriptlets

**Example from hours.jsp:**
```jsp
<%
    CustomerDAO customerDAO = new CustomerDAO();  // Direct instantiation
    String action = request.getParameter("action");
    if ("log".equals(action)) {
        Long customerId = Long.parseLong(request.getParameter("customerId"));
        billableHourDAO.save(billableHour);  // Business logic in JSP
    }
%>
```

### DAO Code Duplication
Each DAO reimplements CRUD methods with near-identical patterns. No base DAO abstraction.

### No Dependency Injection
All dependencies created via `new`:
```java
CustomerDAO customerDAO = new CustomerDAO();
BillingService billingService = new BillingService();
```

Makes testing impossible without refactoring.

### Map-Driven APIs
Services return `Map<String, Object>` instead of typed DTOs:
```java
// Anti-pattern
Map<String, Object> bill = billingService.generateCustomerBill(customerId);
BigDecimal total = (BigDecimal) bill.get("totalAmount");  // Runtime type casting
```

## Refactoring Strategy Guide

### Phase 1: Enable Testing
1. **Extract DAO interfaces** - Enable mock injection
2. **Create base DAO class** - Eliminate duplication
3. **Add constructor injection** - Replace `new` with dependency injection
4. **Setup test infrastructure** - Create `src/test/java` with JUnit 5 tests

### Phase 2: Transaction Management
1. **Add transaction service** - Programmatic transaction boundaries
2. **Wrap service methods** - Ensure atomicity
3. **Handle rollback scenarios** - Proper error recovery

### Phase 3: Service Layer Completion
1. **Move JSP logic to services** - Extract business logic from presentation
2. **Create DTOs** - Replace `Map<String, Object>` returns
3. **Add validation layer** - Input validation at service boundary
4. **Standardize error handling** - Consistent exception strategy

### Phase 4: Modernize Presentation
1. **Replace JSPs with REST API** - JAX-RS endpoints
2. **Add modern frontend** - React/Vue/Angular SPA
3. **Implement security** - JWT authentication
4. **API documentation** - OpenAPI/Swagger

### Phase 5: Data Access Modernization
1. **Migrate to JPA** - Replace raw JDBC with JPA entities
2. **Add Spring Data repositories** - Eliminate DAO boilerplate
3. **Update to java.time** - Replace Joda-Time
4. **Add database migrations** - Flyway or Liquibase

### Phase 6: Microservices Decomposition (Optional)
1. **Identify bounded contexts** - User management, billing, reporting
2. **Extract services** - Separate deployable units
3. **Add service communication** - REST or messaging
4. **Implement distributed transactions** - Saga pattern

## Testing Infrastructure

**Current State:** No tests exist. JUnit 5 configured but unused.

**Setup Required:**
```java
// Example test structure needed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BillingServiceTest {
    private BillingService billingService;
    private CustomerDAO mockCustomerDAO;
    
    @BeforeAll
    void setup() {
        // Initialize in-memory Derby database
        // Create test fixtures
    }
}
```

**Test Database Pattern:**
```java
// Use in-memory Derby for tests
jdbc:derby:memory:testdb;create=true
```

## Common Development Tasks

### Adding a New Entity
1. Create POJO in `entity/` package
2. Create DAO class in `dao/` package following existing pattern
3. Add table creation DDL in `ConnectionManager.initializeDatabase()`
4. Add sample data in `DataInitializationService` if needed
5. Create service methods in appropriate service class
6. Update JSPs or add REST endpoints for UI access

### Adding Business Logic
1. **DON'T** add to JSPs - create/update service class
2. Validate inputs at service layer
3. Coordinate multiple DAO calls if needed
4. Return DTOs or domain entities, not Maps
5. Handle exceptions consistently

### Database Schema Changes
1. Update DDL in `ConnectionManager.createCustomersTable()` pattern
2. Consider data migration for existing databases
3. Test with both embedded Derby and Liberty DataSource modes
4. Update entity classes to match schema

## Known Issues & Gotchas

### Foreign Key Constraints
Schema has FK constraints but DAOs don't check dependencies:
```sql
FOREIGN KEY (customer_id) REFERENCES customers(id)
```
Deleting a customer with billable hours **fails** with integrity violation.

### Database Location Sensitivity
Embedded Derby creates `./data/bigbadmonolith/` relative to working directory. Running from different directories creates multiple databases.

### Time Zone Handling
Joda-Time uses system default timezone implicitly. Can cause issues in containers with different TZ settings.

### Thread Safety
`DateTimeUtils.formatForDisplay()` uses synchronized block because `SimpleDateFormat` is not thread-safe. Performance bottleneck under load.

### Resource Management
**Always** use try-with-resources for JDBC resources. Manual close() calls are error-prone and leak connections.

## Dependencies & Version Management

**Key Dependencies (build.gradle):**
- Jakarta EE 10 APIs (CDI, Servlets)
- Derby 10.17.1.0 (embedded database)
- Commons DBCP2 2.11.0 (connection pooling)
- Joda-Time 2.12.5 (date/time)
- JUnit Jupiter 5.11.0 (testing framework)

**Liberty Gradle Plugin:** 3.8.2 - manages WebSphere Liberty server lifecycle

## Best Practices for AI Agents

### When Generating Code
- **Follow existing patterns** in similar files (especially DAOs)
- **Use try-with-resources** for all JDBC resources
- **Prefer interfaces** over concrete types for new abstractions
- **Add validation** before database operations
- **Document assumptions** about transaction boundaries

### When Refactoring
- **Start small** - refactor one layer at a time
- **Maintain backward compatibility** during incremental changes
- **Add tests first** - characterization tests before refactoring
- **Preserve behavior** - don't fix bugs while refactoring
- **Commit frequently** - small, atomic changes

### When Adding Features
- **Service layer first** - don't add logic to JSPs
- **Consider transactions** - multi-step operations need atomicity
- **Validate inputs** - never trust user input
- **Return DTOs** - avoid Map<String, Object> returns
- **Handle errors consistently** - follow chosen exception strategy

### Quick Wins for Immediate Impact
1. Extract DAO interfaces → enables testing
2. Create base DAO class → eliminates duplication
3. Add validation service → consistent input checking
4. Create DTOs for service returns → type safety
5. Move JSP logic to services → separation of concerns

## Related Resources

**Liberty Documentation:**
- [Open Liberty Docs](https://openliberty.io/docs/)
- [Jakarta EE 10 Specs](https://jakarta.ee/specifications/)

**Refactoring Patterns:**
- Martin Fowler's Refactoring Catalog
- Working Effectively with Legacy Code (Michael Feathers)

**Modernization Guides:**
- Spring Boot migration from Jakarta EE
- Microservices decomposition patterns
- Strangler Fig pattern for incremental rewrites

---

💡 **For iterative improvement of these instructions:** Use `/chronicle improve` to analyze friction patterns from past refactoring sessions and update this guide based on real experience.
