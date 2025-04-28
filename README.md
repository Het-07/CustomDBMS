# Custom Multiuser Database Management System

A lightweight, Java-based DBMS implementation with support for SQL-like queries, transaction management, and concurrency control.

## Project Overview

This project implements a custom Database Management System from scratch using Java. It features a console-based interface that accepts SQL-like queries and supports multiple users with secure authentication. The system follows SOLID principles and implements core database functionalities including persistent storage, query processing, transaction management, and concurrency control.

## Features

### Authentication & Security
- Two-factor authentication (User ID, Password, and Captcha)
- Password hashing using SHA-256
- Password recovery system with security questions
- Comprehensive audit logging for security events

### Database Operations
- Database creation and selection
- Table creation with schema definition
- Data insertion and retrieval
- SQL-like query syntax (CREATE, USE, SHOW, DESCRIBE, INSERT, SELECT)

### Advanced Features
- Transaction management (BEGIN TRANSACTION, COMMIT, ROLLBACK)
- ACID compliance for data integrity
- Concurrency control with read/write locks
- In-memory indexing for optimized queries

## System Architecture

The system is built with a modular architecture following SOLID principles:

- **Authentication Module**: Handles user authentication, password management, and security
- **Storage Module**: Manages persistent storage in JSON format with custom delimiters
- **Query Handler**: Processes SQL-like queries and executes appropriate operations
- **Transaction Manager**: Ensures ACID properties for transactions
- **Concurrency Control**: Implements read/write locks for safe multi-user access
- **Index Manager**: Maintains in-memory indexes for efficient data retrieval

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- Any standard Java IDE (IntelliJ IDEA, Eclipse, or NetBeans)

### Running the Application
1. Clone this repository
2. Open the project in your preferred Java IDE
3. Run the `Main.java` file in the `src/main` package
4. Follow the console prompts to register or login
5. Use SQL-like commands to interact with the database

### Default Credentials
- Username: admin
- Password: password

## Technical Implementation Details

- **Persistent Storage**: Custom JSON-based storage system with efficient read/write operations
- **Indexing**: Tree-based in-memory indexing for optimized query performance
- **Concurrency**: Read/write lock mechanism to prevent data inconsistencies
- **Security**: SHA-256 hashing for passwords and security answers
- **Transactions**: Intermediate data structures to store changes before commit

## By - Het Patel