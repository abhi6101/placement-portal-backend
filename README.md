# Placement Portal | Spring Boot & JavaScript

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/)
[![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)](https://www.javascript.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)

A full-stack web application built to modernize and simplify the job placement process within an educational institution. This platform provides a centralized hub for administrators to post job opportunities and for students to browse and apply for them seamlessly.

## ✨ Live Demo

You can view the deployed application here:
**[https://hack-2-hired.onrender.com/](https://hack-2-hired.onrender.com/)**

## 🚀 Key Features

-   🔐 **Secure JWT Authentication:** Stateless authentication using JSON Web Tokens ensures secure communication between the client and server.
-   👤 **Role-Based Access Control (RBAC):** A robust system with two distinct user roles:
    -   **Admin:** Full CRUD (Create, Read, Update, Delete) capabilities for managing job postings.
    -   **Student:** Can view all available job listings and submit applications directly through the portal.
-   ⚙️ **RESTful API Architecture:** A well-structured backend API built with Spring Boot enables a clean separation of concerns and scalable development.
-   📱 **Responsive Frontend:** The user interface is crafted with HTML, CSS, and vanilla JavaScript to be fully responsive, offering an optimal user experience on any device.

## 🛠️ Tech Stack & Tools

| Category         | Technology / Tool                                  |
| ---------------- | -------------------------------------------------- |
| **Backend**      | Spring Boot, Spring Security                       |
| **Frontend**     | Vanilla JavaScript, HTML5, CSS3                    |
| **Database**     | PostgreSQL                                         |
| **Authentication**| JSON Web Tokens (JWT)                              |
| **Build Tool**   | Apache Maven                                       |
| **Deployment**   | Render                                             |

## ⚙️ System Architecture

The application follows a classic client-server architecture:

1.  **Frontend (Client):** A responsive interface built with HTML, CSS, and JavaScript that handles user interaction and sends HTTP requests to the backend.
2.  **Backend (Server):** A Spring Boot application that exposes a RESTful API. It manages business logic, security with Spring Security & JWT, and data persistence.
3.  **Database:** A PostgreSQL database stores all application data, including user credentials, job postings, and student applications.


## (Local Setup)

To get a local copy up and running, follow these simple steps.

### Prerequisites

-   Java Development Kit (JDK 17 or later)
-   Apache Maven 3.x or later
-   PostgreSQL Server

### Installation

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/your-username/your-repo-name.git
    cd your-repo-name
    ```

2.  **Configure the database:**
    -   Start your PostgreSQL server.
    -   Create a new database for the project.
    -   Navigate to `src/main/resources/application.properties` and update the following properties with your PostgreSQL credentials:
        ```properties
        spring.datasource.url=jdbc:postgresql://localhost:5432/your_db_name
        spring.datasource.username=your_db_username
        spring.datasource.password=your_db_password
        ```

3.  **Install dependencies and run the backend:**
    ```sh
    mvn install
    mvn spring-boot:run
    ```

4.  **Access the application:**
    -   The backend server will start on `http://localhost:8080`.
    -   Open the `index.html` file in your browser to use the application.

### Credentials for Testing

-   **Admin:** `username: admin_user` / `password: admin_pass`
-   **User:** `username: student_user` / `password: student_pass`
    *(Note: Replace with your actual seed data credentials)*

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## 🙏 Acknowledgements

-   [Spring Boot](https://spring.io/projects/spring-boot)
-   [JSON Web Tokens](https://jwt.io/)
-   [Render](https://render.com/) for the deployment.