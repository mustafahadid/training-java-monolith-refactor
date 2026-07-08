package com.sourcegraph.demo.bigbadmonolith.dao;

import com.sourcegraph.demo.bigbadmonolith.entity.Customer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock implementation of ICustomerDAO for testing purposes.
 * This in-memory implementation allows testing service layer without database.
 * 
 * Usage example in service tests:
 * <pre>
 * ICustomerDAO mockDAO = new MockCustomerDAO();
 * BillingService service = new BillingService(mockDAO, ...);
 * </pre>
 */
public class MockCustomerDAO implements ICustomerDAO {
    
    private final Map<Long, Customer> customers = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Customer save(Customer customer) throws SQLException {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        
        // Generate ID if not set
        if (customer.getId() == null) {
            customer.setId(idGenerator.getAndIncrement());
        }
        
        // Store customer
        customers.put(customer.getId(), cloneCustomer(customer));
        
        return customer;
    }
    
    @Override
    public Customer findById(Long id) throws SQLException {
        Customer customer = customers.get(id);
        return customer != null ? cloneCustomer(customer) : null;
    }
    
    @Override
    public List<Customer> findAll() throws SQLException {
        List<Customer> result = new ArrayList<>();
        for (Customer customer : customers.values()) {
            result.add(cloneCustomer(customer));
        }
        return result;
    }
    
    @Override
    public boolean update(Customer customer) throws SQLException {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (customer.getId() == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        
        if (!customers.containsKey(customer.getId())) {
            return false;
        }
        
        customers.put(customer.getId(), cloneCustomer(customer));
        return true;
    }
    
    @Override
    public boolean delete(Long id) throws SQLException {
        return customers.remove(id) != null;
    }
    
    /**
     * Clear all stored customers (useful for test setup/teardown).
     */
    public void clear() {
        customers.clear();
        idGenerator.set(1);
    }
    
    /**
     * Get count of stored customers (useful for test assertions).
     */
    public int size() {
        return customers.size();
    }
    
    /**
     * Clone a customer to prevent external modifications to internal storage.
     */
    private Customer cloneCustomer(Customer original) {
        Customer clone = new Customer();
        clone.setId(original.getId());
        clone.setName(original.getName());
        clone.setEmail(original.getEmail());
        clone.setAddress(original.getAddress());
        clone.setCreatedAt(original.getCreatedAt());
        return clone;
    }
}
