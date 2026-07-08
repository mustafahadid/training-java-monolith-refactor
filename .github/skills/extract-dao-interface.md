---
description: Extract interfaces from DAO classes to enable dependency injection and testing. This is Phase 1 of the monolith modernization strategy.
tools:
  allow: [read_file, grep_search, semantic_search, create_file, replace_string_in_file, multi_replace_string_in_file]
---

# Extract DAO Interface Skill

This skill helps extract interfaces from concrete DAO implementations to enable:
- Dependency injection (constructor/setter injection)
- Mock implementations for unit testing
- Flexible implementation swapping (JDBC → JPA)
- Interface-based programming

## Usage

When the user asks to "extract DAO interface" or "create DAO interface for [Entity]":

## Workflow

1. **Identify the DAO class** to extract from (e.g., `CustomerDAO`)

2. **Analyze public methods** - Read the DAO class and identify all public methods:
   ```java
   public Customer save(Customer customer) throws SQLException
   public Customer findById(Long id)
   public List<Customer> findAll()
   public void update(Customer customer)
   public void delete(Long id)
   ```

3. **Create interface** in same package:
   ```java
   package com.sourcegraph.demo.bigbadmonolith.dao;
   
   import java.util.List;
   import java.sql.SQLException;
   
   public interface ICustomerDAO {
       Customer save(Customer customer) throws SQLException;
       Customer findById(Long id);
       List<Customer> findAll();
       void update(Customer customer) throws SQLException;
       void delete(Long id) throws SQLException;
   }
   ```

4. **Update DAO class** to implement interface:
   ```java
   public class CustomerDAO implements ICustomerDAO {
       // Existing implementation
   }
   ```

5. **Find usages** of the DAO and offer to update to interface:
   ```java
   // Before
   CustomerDAO customerDAO = new CustomerDAO();
   
   // After (prepare for DI)
   ICustomerDAO customerDAO = new CustomerDAO();
   ```

6. **Verify compilation** - Check for errors after extraction

## Naming Convention

- Interface: `I{Entity}DAO` (e.g., `ICustomerDAO`, `IBillableHourDAO`)
- Implementation: Keep existing name (e.g., `CustomerDAO`)
- Alternative: Use plain interface names like `CustomerRepository` if transitioning away from DAO pattern

## Key Considerations

- **Preserve exception signatures** - Keep `throws SQLException` if present
- **Include all public methods** - Don't skip utility or helper methods
- **Generic types** - Preserve generic parameters like `List<Customer>`
- **Check for static methods** - Static methods don't go in interface (create separate utility class)
- **Package-private methods** - Exclude package-private and private methods from interface

## Example Interaction

**User:** "Extract interface for CustomerDAO"

**Agent Actions:**
1. Read `CustomerDAO.java`
2. Identify public methods: `save()`, `findById()`, `findAll()`, `update()`, `delete()`
3. Create `ICustomerDAO.java` with method signatures
4. Update `CustomerDAO` to implement `ICustomerDAO`
5. Run `get_errors` to verify compilation
6. Report completion and offer to update call sites

## Follow-up Suggestions

After extracting interface, suggest:
- "Would you like me to update call sites to use the interface type?"
- "Should I extract interfaces for other DAOs (User, BillableHour, BillingCategory)?"
- "Ready to create a base interface that all DAOs extend?"

## Testing Enablement

Once interfaces exist, explain how to create mock implementations:
```java
// Test with mock
class MockCustomerDAO implements ICustomerDAO {
    @Override
    public Customer save(Customer customer) {
        customer.setId(999L);  // Fake ID
        return customer;
    }
    // ... other methods return test data
}
```

## Integration with Phase 1 Strategy

This skill implements **Phase 1: Enable Testing** from [AGENTS.md](AGENTS.md#phase-1-enable-testing):
1. ✅ Extract DAO interfaces
2. Next: Create base DAO class
3. Next: Add constructor injection
4. Next: Setup test infrastructure
