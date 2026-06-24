# MenteeConnect — Backend

<div align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-3.0+-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/MongoDB-6.0+-47A248?style=for-the-badge&logo=mongodb&logoColor=white" alt="MongoDB" />
  <img src="https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/WebRTC-Video_Call-FF6B6B?style=for-the-badge&logo=webrtc&logoColor=white" alt="WebRTC" />
  <img src="https://img.shields.io/badge/WebSocket-Real_Time-FFCA28?style=for-the-badge&logo=websocket&logoColor=black" alt="WebSocket" />
  <img src="https://img.shields.io/badge/Docker-Container-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <a href="https://menteeconnect.vercel.app/" target="_blank">
    <img src="https://img.shields.io/badge/Live-menteeconnect.vercel.app-000000?style=for-the-badge&logo=vercel&logoColor=white" alt="Live Version" />
  </a>
</div>

<div align="center">
  <h3>Backend for Role-Based Mentor-Student Management System</h3>
  <p><em>Secure, scalable Spring Boot backend powering an academic mentorship platform with real-time video calling, JWT authentication, and role-based access control.</em></p>
  <br/>
  <strong><a href="https://menteeconnect.vercel.app/">menteeconnect.vercel.app</a></strong>
</div>

---

## Architecture Overview

MenteeConnect utilizes a decoupled, high-performance architecture combining a robust, secure Spring Boot backend with a dynamic React frontend. 

```mermaid
flowchart TB
    subgraph External_Clients ["External Clients"]
        BrowserA["User Browser A <br/> Admin / Mentor / Student"]
        BrowserB["User Browser B <br/> Mentor / Student"]
    end

    subgraph Docker_Network ["Docker Network: app-network"]
        
        subgraph Frontend_Container ["React Frontend Container port 80"]
            direction TB
            Router["React Router DOM"]
            Context["Context API: Auth & Socket"]
            Views["Role Dashboards: Admin / Mentor / Student"]
            Services["Axios Interceptors & API Services"]
            WebRTC["WebRTC Client & Media Devices"]
            
            Router --> Views
            Views --> Context
            Views --> Services
            Views --> WebRTC
        end

        subgraph Backend_Container ["Spring Boot Backend Container port 8080"]
            direction TB
            SecFilter["Spring Security JWT Filter"]
            RESTCtrl["REST API Controllers"]
            WSEndpoint["WebSocket STOMP Broker /ws"]
            
            AuthBiz["Auth & User Service"]
            AcadBiz["Batch, Task & Attendance Svc"]
            VideoBiz["Video Call Signaling Svc"]
            Analytics["Analytics & Notification Svc"]
            
            SecFilter --> RESTCtrl
            RESTCtrl --> AuthBiz
            RESTCtrl --> AcadBiz
            RESTCtrl --> Analytics
            WSEndpoint --> VideoBiz
            WSEndpoint --> Analytics
        end

        subgraph Persistence_Layer ["Persistence & Storage Layer"]
            DB[("MongoDB Database")]
            FileSystem["Local Certificate/Upload Storage"]
        end
        
    end

    %% Client to Frontend
    BrowserA -- "HTTP/React" --> Frontend_Container
    BrowserB -- "HTTP/React" --> Frontend_Container
    
    %% Direct P2P
    BrowserA <==>|"Direct P2P WebRTC Video/Audio"| BrowserB

    %% Frontend to Backend
    Services -- "HTTP/REST APIs (JSON + JWT)" --> SecFilter
    Services -- "Multipart Form-Data" --> SecFilter
    Context -- "WebSocket / SockJS Connection" --> WSEndpoint
    WebRTC -- "ICE Candidates & SDP Offers/Answers" --> WSEndpoint

    %% Backend to Persistence
    AuthBiz --> DB
    AcadBiz --> DB
    VideoBiz --> DB
    Analytics --> DB
    AcadBiz --> FileSystem
```

## Features

- **Role-Based JWT Authentication:** Secure login and session management for Admin, Mentor, and Student roles.
- **WebRTC Video Call Signaling:** Backend signaling server enabling low-latency peer-to-peer video calls between mentors and mentees.
- **Real-Time Notifications:** WebSocket-based incoming call alerts and announcements delivered instantly to users.
- **Meeting History APIs:** Comprehensive storage and retrieval of video call records, enabling mentors to access their own history and admins to oversee platform-wide records.
- **Comprehensive Management APIs:** Full CRUD functionality for user, batch, mentor, and student management.
- **Goal & Certificate Tracking:** Robust tracking capabilities for student assignments, goals, and achievements.
- **File Upload Support:** Secure handling of multipart form data for profile photos and educational resources.
- **Password Security:** BCrypt hashing for all user credentials to maintain high security standards.

---

## Technologies Used

| Category      | Tech Stack                              |
|---------------|------------------------------------------|
| Language      | Java 17+                                 |
| Framework     | Spring Boot 3+                           |
| Database      | MongoDB 6+ (NoSQL)                       |
| Security      | Spring Security + JWT Authentication     |
| Real-Time     | WebSocket (Spring Messaging + STOMP)     |
| Video Calls   | WebRTC Signaling via WebSocket           |
| Build Tool    | Maven                                    |
| Logging       | SLF4J + Logback                          |
| Deployment    | Docker                                   |

---

## Video Call Integration Architecture

The backend serves as a critical WebRTC signaling server, coordinating the initial handshake and peer connection setup.

```mermaid
sequenceDiagram
    participant Mentor
    participant Backend as Signaling Server
    participant Mentee

    Mentor->>Backend: WebSocket: Initiate Call Request
    Backend->>Mentee: WebSocket: Incoming Call Alert
    Mentee->>Backend: WebSocket: Accept Call
    Backend->>Mentor: WebSocket: Call Accepted
    Mentor->>Backend: Send WebRTC Offer
    Backend->>Mentee: Relay WebRTC Offer
    Mentee->>Backend: Send WebRTC Answer
    Backend->>Mentor: Relay WebRTC Answer
    Mentor->>Mentee: ICE Candidate Exchange (via Backend)
    Note over Mentor,Mentee: Direct P2P Video Stream Established
```

**Access Control Details:**
- Mentors are strictly restricted to initiating calls with their allocated mentees.
- Call history retrieval endpoints (`/api/meetings/mentor`) are scoped to the authenticated mentor.
- System-wide meeting records are restricted to the Admin dashboard via `/api/meetings/all`.

---

## Project Structure

```text
backend/
└── src/
    └── main/
        ├── java/com/menteeconnect/
        │   ├── controllers/       # REST Controllers
        │   ├── services/          # Business Logic Layer
        │   ├── models/            # MongoDB Document Models
        │   ├── repositories/      # MongoDB Repositories
        │   ├── security/          # JWT Filters, Auth Config
        │   └── config/            # CORS, WebSocket, WebRTC Config
        └── resources/
            ├── application.yml    # App Configuration
            └── static/            # Static assets
```

---

## Configuration & Setup

### `application.yml` Example

```yaml
server:
  port: 8080

spring:
  data:
    mongodb:
      uri: ${MONGO_URI:mongodb://localhost:27017/menteeconnect}
      database: menteeconnect
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000
```

*Note: Ensure all sensitive configurations (`JWT_SECRET`, `MONGO_URI`) are injected via environment variables.*

### Local Development

**Prerequisites:**
- Java 17+
- Maven 3.6+
- MongoDB 6.0+

```bash
# Navigate to the project directory
cd Mentee_Connect

# Clean and build the application
mvn clean install

# Launch the Spring Boot server
mvn spring-boot:run
```

---

## Security Implementation

- **Token Validation:** All protected API endpoints enforce JWT validation filters.
- **RBAC (Role-Based Access Control):** Granular separation of concerns for Admin, Mentor, and Student endpoints.
- **Data Protection:** BCrypt password encoding ensures data integrity at rest.
- **CORS Policies:** Configured origin validation for frontend integration safety.

---

## Deployment Configuration

**Docker Profile:**

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/menteeconnect.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build and Run Commands:**

```bash
docker build -t menteeconnect-backend .
docker run -p 8080:8080 \
  -e JWT_SECRET=your_secret \
  -e MONGO_URI=mongodb://host.docker.internal:27017/menteeconnect \
  menteeconnect-backend
```

---

## Developer

**Mahesh Shinde**

- **Email:** [maheshshinde9100@gmail.com](mailto:maheshshinde9100@gmail.com)
- **Portfolio:** [maheshshinde-dev.vercel.app](https://maheshshinde-dev.vercel.app)
- **LinkedIn:** [linkedin.com/in/maheshshinde9100](https://www.linkedin.com/in/maheshshinde9100)
- **GitHub:** [github.com/maheshshinde9100](https://github.com/maheshshinde9100)

*MenteeConnect is part of an academic mentorship platform. All rights reserved.*
