# How to Run Visad API

## Prerequisites
1.  **Java 17**: This project requires Java 17.
2.  **Maven**: Used for building the project.
3.  **MySQL Database**: The application connects to a MySQL database.

## 1. Database Setup
The application is configured to connect to a MySQL database on `localhost:3306` by default.
You need to ensure a MySQL server is running and a database named `visad_test` exists.

**Configuration:**
The database credentials can be configured in `src/main/resources/application.yml` or via environment variables.
Default settings:
-   **Database Name**: `visad_test`
-   **Username**: `root`
-   **Password**: `elnitish@11`

If your local database has different credentials, update `src/main/resources/application.yml` or set the environment variables:
```bash
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

## 2. Build the Application
I have already initialized the dependencies. To rebuild the project, run:
```bash
mvn clean install
```

## 3. Run the Application
You can run the application using Maven:
```bash
mvn spring-boot:run
```

Or by running the built JAR file:
```bash
java -jar target/visad-api-1.0.0.jar
```

The application will start on port **8089**.
API Base URL: `http://localhost:8089/api`
