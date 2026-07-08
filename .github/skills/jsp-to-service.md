---
description: Move business logic from JSP scriptlets to service layer classes. Eliminates JSP anti-patterns and implements proper separation of concerns.
tools:
  allow: [read_file, grep_search, semantic_search, create_file, replace_string_in_file, multi_replace_string_in_file]
---

# JSP to Service Migration Skill

Extracts business logic from JSP scriptlets into proper service layer methods, following **Phase 3: Service Layer Completion** from the refactoring strategy.

## Anti-Pattern: JSP Scriptlet Hell

Current JSPs contain:
- DAO instantiation: `CustomerDAO dao = new CustomerDAO()`
- Form processing: `String name = request.getParameter("name")`
- Validation logic: `if (name == null || name.isEmpty()) ...`
- Business calculations: Revenue calculations, aggregations
- Database queries: Direct JDBC in JSPs
- Exception handling: Try-catch blocks exposing technical errors

## Target Architecture

**JSP** → Forwards to **Servlet/Controller** → Calls **Service** → Uses **DAO** → Returns **DTO** → JSP displays

## Usage

When user asks to "extract JSP logic to service" or "refactor [page].jsp":

## Workflow

### 1. Analyze JSP File

Read the target JSP and identify:
- **Form actions** (add, update, delete operations)
- **DAO instantiations** (should be injected)
- **Business logic** (validation, calculations)
- **Query operations** (data retrieval)

Example from `customers.jsp`:
```jsp
<%
    CustomerDAO customerDAO = new CustomerDAO();
    String action = request.getParameter("action");
    
    if ("add".equals(action)) {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String address = request.getParameter("address");
        
        if (name != null && !name.trim().isEmpty()) {
            Customer customer = new Customer(name, email, address);
            customerDAO.save(customer);
            message = "Customer added successfully!";
        } else {
            message = "Name and email are required!";
        }
    }
%>
```

### 2. Create Service Method

Extract to service class (create if doesn't exist):

```java
package com.sourcegraph.demo.bigbadmonolith.service;

public class CustomerService {
    private final ICustomerDAO customerDAO;
    
    public CustomerService() {
        this.customerDAO = new CustomerDAO();  // Later: inject via DI
    }
    
    public ServiceResult<Customer> addCustomer(String name, String email, String address) {
        // Validation
        if (name == null || name.trim().isEmpty()) {
            return ServiceResult.error("Name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            return ServiceResult.error("Email is required");
        }
        
        try {
            Customer customer = new Customer(name, email, address);
            Customer saved = customerDAO.save(customer);
            return ServiceResult.success(saved, "Customer added successfully");
        } catch (SQLException e) {
            return ServiceResult.error("Failed to add customer: " + e.getMessage());
        }
    }
}
```

### 3. Create ServiceResult DTO

If doesn't exist, create result wrapper:

```java
package com.sourcegraph.demo.bigbadmonolith.dto;

public class ServiceResult<T> {
    private final boolean success;
    private final T data;
    private final String message;
    
    private ServiceResult(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }
    
    public static <T> ServiceResult<T> success(T data, String message) {
        return new ServiceResult<>(true, data, message);
    }
    
    public static <T> ServiceResult<T> error(String message) {
        return new ServiceResult<>(false, null, message);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }
}
```

### 4. Update JSP to Use Service

Simplify JSP to only call service:

```jsp
<%
    CustomerService customerService = new CustomerService();
    String action = request.getParameter("action");
    String message = "";
    
    if ("add".equals(action)) {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String address = request.getParameter("address");
        
        ServiceResult<Customer> result = customerService.addCustomer(name, email, address);
        message = result.getMessage();
    }
    
    List<Customer> customers = customerService.getAllCustomers();
%>
```

### 5. Iterate for All Actions

Repeat for each action in JSP:
- `action=add` → `addCustomer()`
- `action=update` → `updateCustomer()`
- `action=delete` → `deleteCustomer()`

### 6. Extract Query Methods

Move data retrieval to service:

```java
// In CustomerService
public List<Customer> getAllCustomers() {
    return customerDAO.findAll();
}

public Optional<Customer> getCustomerById(Long id) {
    Customer customer = customerDAO.findById(id);
    return Optional.ofNullable(customer);
}
```

## Complex Example: Reports

For `reports.jsp` with aggregations:

**Before (JSP):**
```jsp
<%
    double totalRevenue = 0.0;
    for (BillableHour hour : billableHours) {
        BillingCategory category = categoryDAO.findById(hour.getCategoryId());
        totalRevenue += hour.getHours().doubleValue() * category.getHourlyRate().doubleValue();
    }
%>
```

**After (Service):**
```java
public CustomerBillDTO generateCustomerReport(Long customerId) {
    List<BillableHour> hours = billableHourDAO.findByCustomerId(customerId);
    Map<Long, BillingCategory> categoryCache = buildCategoryCache();
    
    BigDecimal totalRevenue = BigDecimal.ZERO;
    List<BillLineItemDTO> lineItems = new ArrayList<>();
    
    for (BillableHour hour : hours) {
        BillingCategory category = categoryCache.get(hour.getCategoryId());
        BigDecimal lineTotal = hour.getHours().multiply(category.getHourlyRate());
        totalRevenue = totalRevenue.add(lineTotal);
        
        lineItems.add(new BillLineItemDTO(
            hour.getDateLogged(),
            category.getName(),
            hour.getHours(),
            category.getHourlyRate(),
            lineTotal
        ));
    }
    
    return new CustomerBillDTO(customerId, lineItems, totalRevenue);
}
```

## File Organization

Create service classes following existing structure:

```
src/main/java/com/sourcegraph/demo/bigbadmonolith/
├── service/
│   ├── BillingService.java        # Exists - enhance
│   ├── CustomerService.java       # Create
│   ├── UserService.java           # Create
│   └── BillableHourService.java   # Create
└── dto/                            # Create new package
    ├── ServiceResult.java
    ├── CustomerBillDTO.java
    ├── BillLineItemDTO.java
    └── RevenueReportDTO.java
```

## Testing Benefits

Once logic is in services, easily unit testable:

```java
@Test
void addCustomer_withValidData_succeeds() {
    CustomerService service = new CustomerService();
    
    ServiceResult<Customer> result = service.addCustomer(
        "Acme Corp", 
        "contact@acme.com", 
        "123 Main St"
    );
    
    assertTrue(result.isSuccess());
    assertEquals("Customer added successfully", result.getMessage());
}

@Test
void addCustomer_withEmptyName_fails() {
    CustomerService service = new CustomerService();
    
    ServiceResult<Customer> result = service.addCustomer("", "test@test.com", "");
    
    assertFalse(result.isSuccess());
    assertEquals("Name is required", result.getMessage());
}
```

## Migration Checklist

For each JSP:

- [ ] Identify all `action` parameters and their logic
- [ ] Create service class if doesn't exist
- [ ] Extract validation logic to service methods
- [ ] Extract business calculations to service methods
- [ ] Create DTO classes for complex returns (replace `Map<String, Object>`)
- [ ] Update JSP to call service methods only
- [ ] Remove DAO instantiations from JSP
- [ ] Test all form actions still work
- [ ] Add unit tests for service methods

## Integration with Refactoring Strategy

Implements **Phase 3** from [AGENTS.md](AGENTS.md#phase-3-service-layer-completion):
1. ✅ Move JSP logic to services
2. ✅ Create DTOs (replace Map returns)
3. ✅ Add validation layer
4. Next: Standardize error handling

## Next Steps After Migration

Once JSPs are thin:
1. Replace JSPs with Thymeleaf or modern template engine
2. Create REST controllers for API access
3. Add frontend SPA (React/Vue/Angular)
4. Implement proper MVC pattern
