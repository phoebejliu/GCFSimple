package org.example;

import jakarta.persistence.*;
import java.util.List;

public class Main {
    // ===== Database Connection Fields =====
    private static EntityManager entityManager;
    private static EntityManagerFactory emf;

    // ===== Main Methods =====
    public static void main(String[] args) {
        init();
    }

    public static void init() {
        try {
            initializeDatabaseConnection();
            runDemo();
        } catch (Exception e) {
            handleError("Error in initialization", e);
        } finally {
            closeResources();
        }
    }

    // ===== Demo Execution =====
    private static void runDemo() {
        System.out.println("\n=== Starting Book Management System Demo ===\n");
        
        // Create sample data
        Author[] authors = createSampleAuthors();
        Category[] categories = createSampleCategories();
        createSampleBooks(authors, categories);
        
        // Demonstrate operations using the last author (Haruki Murakami)
        demonstrateBookOperations(authors[2]);
        
        // Demonstrate ManyToMany relationship operations
        demonstrateCategoryOperations(categories[0]); // Fiction category
    }

    // ===== Database Operations =====
    private static void initializeDatabaseConnection() {
        emf = Persistence.createEntityManagerFactory("myPU");
        entityManager = emf.createEntityManager();
        System.out.println("Successfully connected to the database!");
    }

    private static void closeResources() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    private static void executeInTransaction(Runnable operation) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            operation.run();
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        }
    }

    private static <T> T executeInTransactionWithResult(TransactionOperation<T> operation) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T result = operation.execute();
            transaction.commit();
            return result;
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        }
    }

    // ===== Sample Data Creation =====
    private static Author[] createSampleAuthors() {
        System.out.println("Creating authors...");
        return new Author[] {
            createAuthor("J.K. Rowling", "United Kingdom"),
            createAuthor("George Orwell", "United Kingdom"),
            createAuthor("Haruki Murakami", "Japan")
        };
    }

    private static Category[] createSampleCategories() {
        System.out.println("\nCreating categories...");
        return new Category[] {
            createCategory("Fiction"),
            createCategory("Fantasy"),
            createCategory("Dystopian"),
            createCategory("Magical Realism"),
            createCategory("Classic")
        };
    }

    private static Category createCategory(String name) {
        return executeInTransactionWithResult(() -> {
            Category category = new Category(name);
            entityManager.persist(category);
            System.out.println("Created category: " + category);
            return category;
        });
    }

    private static void createSampleBooks(Author[] authors, Category[] categories) {
        System.out.println("\nAdding books...");
        Book book1 = createBook("Harry Potter and the Philosopher's Stone", authors[0], 1997);
        book1.addCategory(categories[0]); // Fiction
        book1.addCategory(categories[1]); // Fantasy

        Book book2 = createBook("Harry Potter and the Chamber of Secrets", authors[0], 1998);
        book2.addCategory(categories[0]); // Fiction
        book2.addCategory(categories[1]); // Fantasy

        Book book3 = createBook("1984", authors[1], 1949);
        book3.addCategory(categories[0]); // Fiction
        book3.addCategory(categories[2]); // Dystopian
        book3.addCategory(categories[4]); // Classic

        Book book4 = createBook("Norwegian Wood", authors[2], 1987);
        book4.addCategory(categories[0]); // Fiction

        Book book5 = createBook("Kafka on the Shore", authors[2], 2002);
        book5.addCategory(categories[0]); // Fiction
        book5.addCategory(categories[3]); // Magical Realism
    }

    // ===== CRUD Operations =====
    private static Author createAuthor(String name, String country) {
        return executeInTransactionWithResult(() -> {
            Author author = new Author(name, country);
            entityManager.persist(author);
            System.out.println("Created author: " + author);
            return author;
        });
    }

    private static Book createBook(String title, Author author, int publicationYear) {
        return executeInTransactionWithResult(() -> {
            Book book = new Book(title, author.getName(), publicationYear);
            book.setAuthor(author);
            entityManager.persist(book);
            author.addBook(book);
            System.out.println("Created book: " + book);
            return book;
        });
    }

    private static Book findBookById(Long id) {
        return entityManager.find(Book.class, id);
    }

    private static void updateBook(Long id, String newTitle, boolean isAvailable) {
        executeInTransaction(() -> {
            Book book = entityManager.find(Book.class, id);
            if (book != null) {
                book.setTitle(newTitle);
                book.setAvailable(isAvailable);
                entityManager.merge(book);
                System.out.println("Updated book: " + book);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Book> findAvailableBooks() {
        return entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.isAvailable = true", Book.class)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    private static List<Book> findBooksByAuthor(Long authorId) {
        return entityManager.createQuery(
            "SELECT b FROM Book b JOIN b.author a WHERE a.id = :authorId", Book.class)
            .setParameter("authorId", authorId)
            .getResultList();
    }

    private static void deleteBook(Long id) {
        executeInTransaction(() -> {
            Book book = entityManager.find(Book.class, id);
            if (book != null) {
                // Remove the book from the author's collection
                Author author = book.getAuthor();
                if (author != null) {
                    author.getBooks().remove(book);
                }
                entityManager.remove(book);
                System.out.println("Deleted book with ID: " + id);
            }
        });
    }

    // ===== Helper Methods =====
    private static void demonstrateBookOperations(Author author) {
        // Find book by ID
        System.out.println("\nFinding book by ID (ID: 1):");
        Book foundBook = findBookById(1L);
        System.out.println(foundBook);

        // Update book
        System.out.println("\nUpdating book (ID: 1) title and availability...");
        updateBook(1L, "Harry Potter and the Sorcerer's Stone", false);

        // Find available books
        System.out.println("\nAll available books:");
        findAvailableBooks().forEach(System.out::println);

        // Find books by author
        System.out.println("\nBooks by " + author.getName() + ":");
        findBooksByAuthor(author.getId()).forEach(System.out::println);

        // Delete book
        System.out.println("\nDeleting book (ID: 5)...");
        deleteBook(5L);
        System.out.println("Book with ID 5 deleted. Trying to find it again:");
        Book deletedBook = findBookById(5L);
        System.out.println(deletedBook != null ? deletedBook : "Book not found (as expected)");

        System.out.println("\n=== Demo Completed Successfully ===");
    }

    private static void demonstrateCategoryOperations(Category category) {
        System.out.println("\n=== Demonstrating Category Operations ===");
        
        // 1. Show all books in a category
        System.out.println("\nBooks in category '" + category.getName() + "':");
        List<Book> booksInCategory = findBooksByCategory(category.getId());
        booksInCategory.forEach(book -> System.out.println("- " + book.getTitle() + " by " + book.getAuthorName()));
        
        // 2. Add a new category to a book
        System.out.println("\nAdding 'Science Fiction' category to '1984'...");
        executeInTransaction(() -> {
            Book book1984 = entityManager.createQuery(
                "SELECT b FROM Book b WHERE b.title = :title", Book.class)
                .setParameter("title", "1984")
                .getSingleResult();
            
            Category scifiCategory = new Category("Science Fiction");
            entityManager.persist(scifiCategory);
            book1984.addCategory(scifiCategory);
            System.out.println("Added category 'Science Fiction' to '1984'");
        });
        
        // 3. Show updated categories for a book
        System.out.println("\nUpdated categories for '1984':");
        Book updatedBook = entityManager.createQuery(
            "SELECT b FROM Book b JOIN FETCH b.categories WHERE b.title = :title", Book.class)
            .setParameter("title", "1984")
            .getSingleResult();
        updatedBook.getCategories().forEach(c -> System.out.println("- " + c.getName()));
        
        // 4. Find all categories
        System.out.println("\nAll categories and their book counts:");
        List<Category> allCategories = entityManager.createQuery(
            "SELECT c FROM Category c LEFT JOIN FETCH c.books", Category.class)
            .getResultList();
            
        allCategories.forEach(c -> 
            System.out.println("- " + c.getName() + ": " + c.getBooks().size() + " books")
        );
    }
    
    @SuppressWarnings("unchecked")
    private static List<Book> findBooksByCategory(Long categoryId) {
        return entityManager.createQuery(
            "SELECT b FROM Book b JOIN b.categories c WHERE c.id = :categoryId", Book.class)
            .setParameter("categoryId", categoryId)
            .getResultList();
    }

    private static void handleError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }

    @FunctionalInterface
    private interface TransactionOperation<T> {
        T execute();
    }
}
