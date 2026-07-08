---
description: Validates code changes against monolith anti-patterns. Triggers on file edits to prevent regression during refactoring.
events:
  - before:edit
applyTo:
  - "**/*.java"
  - "**/*.jsp"
tools:
  allow: [read_file, grep_search]
---

# Monolith Anti-Pattern Detection Hook

Automatically checks code changes for anti-patterns common in this legacy codebase. Helps prevent regression during refactoring efforts.

## Trigger

Runs automatically before editing `.java` or `.jsp` files.

## Checks Performed

### 1. JSP Anti-Patterns

**❌ Scriptlet Detection:**
```jsp
<% 
    CustomerDAO dao = new CustomerDAO();  // Direct DAO instantiation
%>
```
**✅ Recommendation:** Move logic to service layer. Use controller servlet to prepare data.

**❌ JDBC in JSPs:**
```jsp
<%
    Connection conn = DriverManager.getConnection(...);
    PreparedStatement stmt = conn.prepareStatement(...);
%>
```
**✅ Recommendation:** Database access must be in DAO layer only.

### 2. Java Anti-Patterns

**❌ Direct DAO Instantiation:**
```java
CustomerDAO customerDAO = new CustomerDAO();  // Tight coupling
```
**✅ Recommendation:** Use constructor injection:
```java
private final ICustomerDAO customerDAO;

public CustomerService(ICustomerDAO customerDAO) {
    this.customerDAO = customerDAO;
}
```

**❌ Map Return Types:**
```java
public Map<String, Object> generateReport() { ... }  // Stringly-typed
```
**✅ Recommendation:** Create DTO:
```java
public CustomerReportDTO generateReport() { ... }
```

**❌ String Error Messages:**
```java
String errors = "";
errors += "Name is required. ";
return errors;
```
**✅ Recommendation:** Use Result/Either pattern or throw typed exceptions:
```java
if (name == null) {
    throw new ValidationException("Name is required");
}
```

**❌ No Transaction Management:**
```java
// Service method with multiple DAO calls
public void transferBilling(Long from, Long to) {
    billableHourDAO.update(hour1);  // Auto-commit
    billableHourDAO.update(hour2);  // Auto-commit - not atomic!
}
```
**✅ Recommendation:** Add transaction boundary:
```java
@Transactional
public void transferBilling(Long from, Long to) {
    // Both updates in same transaction
}
```

**❌ SQLException Wrapping Inconsistency:**
```java
// Don't mix patterns
catch (SQLException e) {
    throw new RuntimeException(e);  // One DAO uses this
}

public void save() throws SQLException {  // Another DAO uses this
}
```
**✅ Recommendation:** Standardize on one approach (prefer unchecked exceptions).

**❌ Missing Try-With-Resources:**
```java
Connection conn = getConnection();
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.executeUpdate();
conn.close();  // Manual close - leak risk
```
**✅ Recommendation:** Always use try-with-resources:
```java
try (Connection conn = getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.executeUpdate();
}
```

**❌ Joda-Time Usage (Legacy):**
```java
import org.joda.time.LocalDate;
private LocalDate dateLogged;
```
**✅ Recommendation:** Migrate to java.time:
```java
import java.time.LocalDate;
private LocalDate dateLogged;
```

**❌ Static Method Dependencies:**
```java
Connection conn = ConnectionManager.getConnection();  // Hard to test
```
**✅ Recommendation:** Inject connection source:
```java
private final DataSource dataSource;
Connection conn = dataSource.getConnection();
```

## Hook Behavior

When anti-pattern detected:

1. **List violations** with line references
2. **Explain why** it's an anti-pattern
3. **Suggest refactoring** approach
4. **Ask user**: "Proceed with change anyway, or refactor first?"

## Example Output

```
⚠️ Anti-Pattern Detection: customers.jsp

Line 15: Direct DAO instantiation in JSP
  ❌ CustomerDAO dao = new CustomerDAO();
  ✅ Suggestion: Move to service layer or inject via request attribute

Line 23: Business logic in JSP scriptlet
  ❌ Complex validation and calculation in <% %> block
  ✅ Suggestion: Extract to CustomerService.addCustomer() method

Line 45: Exception message exposed to user
  ❌ catch (Exception e) { out.println(e.getMessage()); }
  ✅ Suggestion: Log technical details, show user-friendly message

Proceed with edit (not recommended) or refactor first? [refactor/proceed]
```

## Configuration

Adjust sensitivity in hook YAML frontmatter:

```yaml
# Strict mode - block all anti-patterns
checkLevel: strict

# Warning mode - warn but allow (default)
checkLevel: warning

# Disabled - skip checks
checkLevel: disabled
```

## Integration with Refactoring Phases

This hook supports the overall modernization strategy by:

- **Phase 1:** Enforces interface usage for DAOs
- **Phase 2:** Warns about missing transactions
- **Phase 3:** Detects business logic in JSPs
- **Phase 4:** Recommends REST over JSP modifications
- **Phase 5:** Suggests JPA migration opportunities

## Disable for Specific Cases

Add comment to bypass:
```java
// @refactor-hook: ignore-next-line - legacy code, will refactor in TICKET-123
CustomerDAO dao = new CustomerDAO();
```

## Related Files

- [AGENTS.md](AGENTS.md) - Overall refactoring strategy
- [extract-dao-interface.md](skills/extract-dao-interface.md) - Fix DAO instantiation
- [jsp-to-service.md](skills/jsp-to-service.md) - Fix JSP anti-patterns
