package com.sourcegraph.demo.bigbadmonolith.dao;

import com.sourcegraph.demo.bigbadmonolith.entity.Customer;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface for Customer data access operations.
 * Defines the contract for CRUD operations on Customer entities.
 */
public interface ICustomerDAO {
    
    /**
     * Save a new customer to the database.
     * 
     * @param customer the customer to save
     * @return the saved customer with generated ID
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if customer is null or validation fails
     */
    Customer save(Customer customer) throws SQLException;
    
    /**
     * Find a customer by ID.
     * 
     * @param id the customer ID
     * @return the customer if found, null otherwise
     * @throws SQLException if a database access error occurs
     */
    Customer findById(Long id) throws SQLException;
    
    /**
     * Retrieve all customers.
     * 
     * @return list of all customers, ordered by creation date descending
     * @throws SQLException if a database access error occurs
     */
    List<Customer> findAll() throws SQLException;
    
    /**
     * Update an existing customer.
     * 
     * @param customer the customer with updated values
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if customer or ID is null or validation fails
     */
    boolean update(Customer customer) throws SQLException;
    
    /**
     * Delete a customer by ID.
     * 
     * @param id the customer ID
     * @return true if the deletion was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean delete(Long id) throws SQLException;
}
