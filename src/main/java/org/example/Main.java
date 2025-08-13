package org.example;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static EntityManager entityManager;

    public static void init() {
        try {
            Map<String, String> properties = new HashMap<>();
            properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            properties.put("jakarta.persistence.jdbc.user", "sa");
            properties.put("jakarta.persistence.jdbc.password", "");
            properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.show_sql", "true");
            properties.put("hibernate.format_sql", "true");
            
            EntityManagerFactory entityManagerFactory = 
                Persistence.createEntityManagerFactory("myPU", properties);
            
            entityManager = entityManagerFactory.createEntityManager();
            System.out.println("Successfully connected to the database!");
        } catch (Exception e) {
            System.err.println("Error initializing database connection: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize JPA", e);
        }
    }

    public static void main(String[] args) {
        // Initialize EntityManager
        init();

        if (entityManager == null) {
            System.err.println("Failed to initialize EntityManager. Exiting...");
            return;
        }

        try {
            // Create a new user
            User user = new User();
            user.setName("Anna");
            user.setEmail("anna@example.com");

            // Begin transaction
            entityManager.getTransaction().begin();
            entityManager.persist(user);
            entityManager.getTransaction().commit();

            System.out.println("User saved successfully!");

            User user2 = entityManager.find(User.class, 1L);
            System.out.println(user2.getName());

        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            System.err.println("Error saving user: " + e.getMessage());
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
