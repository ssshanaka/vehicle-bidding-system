# 🚗 Vehicle Bidding System (VBS)

[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-svg&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5%20(RC)-brightgreen?style=flat-svg&logo=springboot)](https://spring.io/projects/spring-boot)
[![MS SQL Server](https://img.shields.io/badge/Database-MS%20SQL%20Server-red?style=flat-svg&logo=microsoft-sql-server)](https://www.microsoft.com/en-us/sql-server)
[![WebSockets](https://img.shields.io/badge/Real--Time-WebSockets-blue?style=flat-svg)](https://spring.io/guides/gs/messaging-stomp-websocket/)
[![JWT Auth](https://img.shields.io/badge/Security-JWT%20Token-blueviolet?style=flat-svg&logo=json-web-tokens)](https://jwt.io/)

A modern, high-performance, real-time online vehicle auction and bidding platform. Built using **Spring Boot**, **MS SQL Server**, and **WebSockets**, it provides a seamless, secure, and interactive marketplace for buyers and sellers to list, inspect, and bid on second-hand and reconditioned vehicles.

---

## 🌟 Key Features

* **🔐 User & Session Management**
  * Role-based access control (RBAC) with dedicated roles: **Buyer/Seller**, **Admin Officer**, **Sales Manager**, **Customer Service Executive**, **Vehicle Inspector**, and **IT Consultant**.
  * Secure, stateless authentication powered by **JWT tokens**.
  * Built-in password recovery/reset flows and rate-limiting.

* **🚘 Vehicle Listing Management**
  * Detailed listing workflow with validated attributes (make, model, year, engine capacity, mileage, and condition).
  * Multiple image uploads and asset management.
  * Inspection flow for inspectors to verify vehicle details for maximum platform integrity.

* **⚡ Real-Time Bidding**
  * Live interactive bidding experience.
  * Real-time bid updates, timer countdowns, and instant winner notifications powered by **WebSockets**.
  * Anti-snipe protection (automatic bidding period extension for late-second bids).

* **🛡️ Admin & Moderation Dashboard**
  * Listing approval, rejection, and moderation queue.
  * User account management, banning (temporary/permanent), and security logs.
  * Comprehensive transaction audit trails.

* **📊 Reports & Notifications**
  * Automated email notifications for bidding activity, outbid warnings, and winner confirmations.
  * Dynamic report generation for auction performance and platform statistics.

---

## 🛠️ Tech Stack

* **Backend Framework:** Java Spring Boot (v3.5+)
* **Database:** Microsoft SQL Server (MS SQL)
* **Frontend Engine:** Thymeleaf, HTML5, Vanilla CSS3, Javascript (ES6), Bootstrap 5
* **Real-time Engine:** Spring WebSocket & SockJS (STOMP Protocol)
* **Security Layer:** Spring Security, JWT (Json Web Token)
* **Mailing Service:** JavaMail Sender (SMTP integration)

---

## 📂 Project Structure

```
vehicle-bidding-system
├── .mvn/                         # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/sliit/vehiclebiddingsystem/
│   │   │   ├── config/           # Application & Security configurations
│   │   │   ├── controller/       # Web and REST Controllers
│   │   │   ├── converter/        # Entity attribute converters
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA Database Entities
│   │   │   ├── exception/        # Global Exception Handlers
│   │   │   ├── repository/       # Database Repositories (Spring Data JPA)
│   │   │   ├── security/         # JWT Filters & Access Handlers
│   │   │   └── service/          # Core Business Logic & Decorators
│   │   └── resources/
│   │       ├── static/           # CSS, JS, and UI Images
│   │       ├── templates/        # Thymeleaf HTML Templates
│   │       └── application.properties # Server and Database Config
│   └── test/                     # Unit & Integration Tests
├── pom.xml                       # Maven dependency tree configuration
└── README.md                     # Project Documentation
```

---

## 🚀 Getting Started

### 📋 Prerequisites
Ensure you have the following installed on your system:
- **Java JDK 17** or higher
- **Microsoft SQL Server** (running locally or remotely)
- **Maven** (optional, wrapper script `./mvnw` is included)

### ⚙️ Database Setup
1. Create a database named `vehicle-bidding-system`.
2. Configure a database user with read/write and schema modification privileges.
3. Update `src/main/resources/application.properties` with your credentials:
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=vehicle-bidding-system;encrypt=true;trustServerCertificate=true
   spring.datasource.username=YOUR_USERNAME
   spring.datasource.password=YOUR_PASSWORD
   ```

### 🏃 Running the Application
To run the server locally on port `8010`:

```bash
# Windows
./mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Access the app via your browser at [http://localhost:8010](http://localhost:8010).

### 🧪 Running Tests
To run unit and integration tests (including Spring Application Context loads):

```bash
# Windows
./mvnw.cmd test

# Linux / macOS
./mvnw test
```