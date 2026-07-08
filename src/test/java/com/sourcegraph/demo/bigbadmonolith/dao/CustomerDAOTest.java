package com.sourcegraph.demo.bigbadmonolith.dao;

import com.sourcegraph.demo.bigbadmonolith.entity.Customer;
import org.joda.time.DateTime;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CustomerDAO.
 * Uses in-memory Derby database for isolated testing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerDAOTest {
    
    private static final String TEST_DB_URL = "jdbc:derby:memory:testdb;create=true";
    private Connection testConnection;
    private ICustomerDAO customerDAO;
    
    @BeforeAll
    void setupDatabase() throws Exception {
        // Load Derby driver
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        
        // Create test database connection
        testConnection = DriverManager.getConnection(TEST_DB_URL);
        
        // Create customers table
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute(
                "CREATE TABLE customers (" +
                "    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                "    name VARCHAR(255) NOT NULL," +
                "    email VARCHAR(255) NOT NULL," +
                "    address VARCHAR(500)," +
                "    created_at TIMESTAMP NOT NULL" +
                ")"
            );
        }
        
        // Initialize DAO (using concrete class for now - will refactor to use mock later)
        customerDAO = new CustomerDAO();
    }
    
    @AfterAll
    void teardownDatabase() throws Exception {
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.close();
        }
        
        // Shutdown Derby database
        try {
            DriverManager.getConnection("jdbc:derby:memory:testdb;drop=true");
        } catch (SQLException e) {
            // Expected exception when dropping database
            if (!e.getSQLState().equals("08006")) {
                throw e;
            }
        }
    }
    
    @BeforeEach
    void cleanDatabase() throws SQLException {
        // Clean all data before each test
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("DELETE FROM customers");
        }
    }
    
    @Test
    @DisplayName("Should save new customer and generate ID")
    void testSaveCustomer() throws SQLException {
        // Arrange
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setEmail("test@example.com");
        customer.setAddress("123 Test Street");
        customer.setCreatedAt(DateTime.now());
        
        // Act
        Customer savedCustomer = customerDAO.save(customer);
        
        // Assert
        assertNotNull(savedCustomer);
        assertNotNull(savedCustomer.getId());
        assertEquals("Test Customer", savedCustomer.getName());
        assertEquals("test@example.com", savedCustomer.getEmail());
        assertEquals("123 Test Street", savedCustomer.getAddress());
    }
    
    @Test
    @DisplayName("Should find customer by ID")
    void testFindById() throws SQLException {
        // Arrange - save a customer first
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setAddress("456 Main St");
        Customer savedCustomer = customerDAO.save(customer);
        
        // Act
        Customer foundCustomer = customerDAO.findById(savedCustomer.getId());
        
        // Assert
        assertNotNull(foundCustomer);
        assertEquals(savedCustomer.getId(), foundCustomer.getId());
        assertEquals("John Doe", foundCustomer.getName());
        assertEquals("john@example.com", foundCustomer.getEmail());
    }
    
    @Test
    @DisplayName("Should return null when customer not found")
    void testFindByIdNotFound() throws SQLException {
        // Act
        Customer customer = customerDAO.findById(999L);
        
        // Assert
        assertNull(customer);
    }
    
    @Test
    @DisplayName("Should retrieve all customers")
    void testFindAll() throws SQLException {
        // Arrange - save multiple customers
        Customer customer1 = new Customer();
        customer1.setName("Customer 1");
        customer1.setEmail("customer1@example.com");
        customerDAO.save(customer1);
        
        Customer customer2 = new Customer();
        customer2.setName("Customer 2");
        customer2.setEmail("customer2@example.com");
        customerDAO.save(customer2);
        
        // Act
        List<Customer> customers = customerDAO.findAll();
        
        // Assert
        assertNotNull(customers);
        assertEquals(2, customers.size());
    }
    
    @Test
    @DisplayName("Should update existing customer")
    void testUpdateCustomer() throws SQLException {
        // Arrange - save a customer first
        Customer customer = new Customer();
        customer.setName("Original Name");
        customer.setEmail("original@example.com");
        customer.setAddress("Original Address");
        Customer savedCustomer = customerDAO.save(customer);
        
        // Act - update customer
        savedCustomer.setName("Updated Name");
        savedCustomer.setEmail("updated@example.com");
        savedCustomer.setAddress("Updated Address");
        boolean updated = customerDAO.update(savedCustomer);
        
        // Assert
        assertTrue(updated);
        
        // Verify changes persisted
        Customer foundCustomer = customerDAO.findById(savedCustomer.getId());
        assertEquals("Updated Name", foundCustomer.getName());
        assertEquals("updated@example.com", foundCustomer.getEmail());
        assertEquals("Updated Address", foundCustomer.getAddress());
    }
    
    @Test
    @DisplayName("Should delete customer by ID")
    void testDeleteCustomer() throws SQLException {
        // Arrange - save a customer first
        Customer customer = new Customer();
        customer.setName("To Delete");
        customer.setEmail("delete@example.com");
        Customer savedCustomer = customerDAO.save(customer);
        
        // Act
        boolean deleted = customerDAO.delete(savedCustomer.getId());
        
        // Assert
        assertTrue(deleted);
        
        // Verify customer no longer exists
        Customer foundCustomer = customerDAO.findById(savedCustomer.getId());
        assertNull(foundCustomer);
    }
    
    @Test
    @DisplayName("Should throw exception when saving null customer")
    void testSaveNullCustomer() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            customerDAO.save(null);
        });
    }
    
    @Test
    @DisplayName("Should throw exception when saving customer with null name")
    void testSaveCustomerWithNullName() {
        // Arrange
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            customerDAO.save(customer);
        });
    }
    
    @Test
    @DisplayName("Should throw exception when updating customer with null ID")
    void testUpdateCustomerWithNullId() {
        // Arrange
        Customer customer = new Customer();
        customer.setName("Test");
        customer.setEmail("test@example.com");
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            customerDAO.update(customer);
        });
    }
}
