# 🚀 MenteeConnect — Backend

<div align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-3.0+-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/MongoDB-6.0+-47A248?style=for-the-badge&logo=mongodb&logoColor=white" />
  <img src="https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" />
  <img src="https://img.shields.io/badge/WebRTC-Video_Call-FF6B6B?style=for-the-badge&logo=webrtc&logoColor=white" />
  <img src="https://img.shields.io/badge/WebSocket-Real_Time-FFCA28?style=for-the-badge&logo=websocket&logoColor=black" />
  <img src="https://img.shields.io/badge/Docker-Container-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <a href="https://menteeconnect.vercel.app/" target="_blank">
    <img src="https://img.shields.io/badge/🚀 Live-menteeconnect.vercel.app-000000?style=for-the-badge&logo=vercel&logoColor=white" />
  </a>
</div>

<div align="center">
  <h3>🎯 Backend for Role-Based Mentor-Student Management System</h3>
  <p><em>Secure, scalable Spring Boot backend powering an academic mentorship platform with real-time video calling, JWT authentication, and role-based access control.</em></p>
  <br/>
  <strong>🌐 <a href="https://menteeconnect.vercel.app/">menteeconnect.vercel.app</a></strong>
</div>

---

## ✨ Features

- **Role-Based JWT Authentication** — Secure login for Admin, Mentor, and Student roles
- **WebRTC Video Call Signaling** — Backend signaling server enabling peer-to-peer video calls between mentors and mentees
- **Real-Time Notifications** — WebSocket-based incoming call alerts delivered instantly to mentees
- **Meeting History APIs** — Stores and exposes video call records; mentors access their own history, admins can view all records platform-wide
- **CRUD APIs** — Full user, batch, mentor, and student management
- **Goal & Certificate Tracking** — Assign goals and track student achievements
- **File Upload Support** — Profile photos and resource sharing
- **Password Security** — BCrypt hashing for all user credentials
- **RESTful Design** — Clean JSON-based API architecture

---

## 🛠️ Technologies Used

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
| Dev Tools     | MongoDB Compass, Postman                 |
| Deployment    | Docker                                   |

---

## Video Call — Backend Architecture

The backend acts as a **WebRTC signaling server**, coordinating the peer connection setup between mentor and mentee.

**Flow:**
1. Mentor sends a call initiation event through WebSocket
2. Backend routes the **incoming call notification** to the target mentee in real time
3. Upon acceptance, backend facilitates **SDP offer/answer and ICE candidate exchange** between peers
4. Once connected, video flows peer-to-peer (no media passes through the server)
5. Session metadata (participants, timestamps, duration) is **persisted to MongoDB** on call end

**Access Control:**
- Mentors can only call their **allocated mentees**
- Mentors can retrieve their **own meeting history** via `/api/meetings/mentor`
- Admins can retrieve **all meeting records** across the platform via `/api/meetings/all`

---

## 🧩 Backend Folder Structure

```
backend/
└── src/
    └── main/
        ├── java/com/menteeconnect/
        │   ├── controllers/       # REST Controllers
        │   ├── services/          # Business Logic Layer
        │   ├── models/            # MongoDB Document Models
        │   ├── repositories/      # MongoDB Repositories
        │   ├── security/          # JWT Filters, Auth Config
        │   └── config/            # CORS, WebSocket, WebRTC Signaling Config
        └── resources/
            ├── application.yml    # App Configuration
            └── static/            # Static assets (if any)
```

---

## ⚙️ Configuration

### `application.yml`

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
  secret: ${JWT_SECRET}       # Always load from environment variable
  expiration: 86400000        # 1 day in milliseconds
```

> 🔒 Store all secrets (`JWT_SECRET`, `MONGO_URI`) in environment variables — never hardcode them.

---

## 🚀 How to Run

### Prerequisites
- Java 17+
- Maven 3.6+
- MongoDB 6.0+ running locally

### Run Commands

```bash
# Navigate to the backend folder
cd backend

# Build the project
mvn clean install

# Start the server
mvn spring-boot:run
```

> MongoDB will auto-create the `menteeconnect` database on first run if it doesn't exist.

---

## 📡 API Modules

| Module              | Base Path               |
|---------------------|--------------------------|
| Authentication      | `/api/auth/`             |
| User Management     | `/api/users/`            |
| Mentor APIs         | `/api/mentors/`          |
| Student APIs        | `/api/students/`         |
| Batch Management    | `/api/batches/`          |
| File Uploads        | `/api/uploads/`          |
| Certificates        | `/api/certificates/`     |
| Goals / Progress    | `/api/goals/`            |
| Notifications       | `/api/notifications/`    |
| Meeting History     | `/api/meetings/`         |
| WebSocket Signaling | `/ws` (STOMP endpoint)   |

---

## 🔒 Security Highlights

- **JWT Authentication** — Every API endpoint is protected with token validation
- **Role-Based Access Control** — Admin, Mentor, and Student routes are strictly separated
- **BCrypt Password Hashing** — Passwords are never stored in plain text
- **CORS Configuration** — Controlled cross-origin access for frontend integration

---

## 🐳 Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/menteeconnect.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build and run
docker build -t menteeconnect-backend .
docker run -p 8080:8080 \
  -e JWT_SECRET=your_secret \
  -e MONGO_URI=mongodb://host.docker.internal:27017/menteeconnect \
  menteeconnect-backend
```

---

## 🔧 Future Enhancements

- [ ] Email notifications via SMTP / Mailgun
- [ ] Cloud file storage (AWS S3 / GCP)
- [ ] Kafka-based event-driven architecture
- [ ] Redis caching layer
- [ ] Unit & integration test coverage
- [ ] OAuth2 login (Google, GitHub)
- [ ] CI/CD deployment pipeline

---

## 👨‍💻 Developer

**Mahesh Shinde**

| Platform    | Link |
|-------------|------|
| 📧 Email    | [maheshshinde9100@gmail.com](mailto:maheshshinde9100@gmail.com) |
| 🌐 Portfolio | [maheshshinde-dev.vercel.app](https://maheshshinde-dev.vercel.app) |
| 🔗 LinkedIn  | [linkedin.com/in/maheshshinde9100](https://www.linkedin.com/in/maheshshinde9100) |
| 🐙 GitHub   | [github.com/maheshshinde9100](https://github.com/maheshshinde9100) |
| 💻 LeetCode  | [leetcode.com/u/code-with-mahesh](https://leetcode.com/u/code-with-mahesh/) |

> 🛡️ MenteeConnect backend is part of the MenteeConnect academic mentorship platform. All rights reserved. Please do not reuse without permission.
