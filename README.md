# Placement Portal - Backend (Spring Boot)

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)

This repository contains the backend server for the **Placement Portal**, a full-stack web application built to modernize and simplify the job placement process. The server is built with Spring Boot and exposes a secure RESTful API to be consumed by the frontend client.

**➡️ Frontend Repository: [placement-portal-frontend](https://github.com/abhi6101/placement-portal-frontend)**

---

## ✨ Live Demo

The complete, deployed application can be viewed here:
**[https://hack-2-hired.onrender.com/](https://hack-2-hired.onrender.com/)**

## 🚀 Key Features

-   🔐 **Secure JWT Authentication:** Stateless authentication using JSON Web Tokens ensures secure communication.
-   👤 **Role-Based Access Control (RBAC):** A robust system with two distinct user roles (Admin and Student).
-   ⚙️ **RESTful API Architecture:** A well-structured API enables a clean separation of concerns and scalable development.
-   🗄️ **Data Persistence:** Uses PostgreSQL to manage all application data, including users, jobs, and applications.

## 🛠️ Tech Stack & Tools

| Category         | Technology / Tool                                  |
| ---------------- | -------------------------------------------------- |
| **Framework**    | Spring Boot, Spring Security                       |
| **Database**     | PostgreSQL                                         |
| **Authentication**| JSON Web Tokens (JWT)                              |
| **Build Tool**   | Apache Maven                                       |
| **Deployment**   | Render                                             |

## ⚙️ API Endpoints

*(Optional but recommended: Briefly list your main API endpoints here. For example:)*
-   `POST /auth/login` - Authenticate a user and receive a JWT.
-   `GET /api/jobs` - Fetch all job listings.
-   `POST /api/admin/jobs` - (Admin) Create a new job posting.
-   `POST /api/jobs/{jobId}/apply` - (Student) Apply for a job.
...and so on.

## Local Setup

To get the backend server running locally, follow these steps. **Note:** You will also need to set up the [frontend client](https://github.com/abhi6101/placement-portal-frontend) to use the application fully.

### Prerequisites

-   Java Development Kit (JDK 17 or later)
-   Apache Maven 3.x or later
-   PostgreSQL Server

### Installation

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/abhi6101/placement-portal-backend.git
    cd placement-portal-backend
    ```

2.  **Configure the database:**
    -   Start your PostgreSQL server and create a new database.
    -   Navigate to `src/main/resources/application.properties` and update the datasource properties with your PostgreSQL credentials:
        ```properties
        spring.datasource.url=jdbc:postgresql://localhost:5432/your_db_name
        spring.datasource.username=your_db_username
        spring.datasource.password=your_db_password
        ```

3.  **Install dependencies and run the server:**
    ```sh
    mvn install
    mvn spring-boot:run
    ```
    The server will start on `http://localhost:8080`.

## 🤝 Contributing

Contributions are greatly appreciated. Please fork the repo and create a pull request.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License

Distributed under the MIT License.