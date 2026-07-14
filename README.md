<div align="center">

# 🧵 Loom

### A modern, open-source newsletter platform built with microservices

*Write. Publish. Grow your audience.*

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.3-6DB33F?style=for-the-badge&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-GKE-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)](https://cloud.google.com/kubernetes-engine)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Neo4j](https://img.shields.io/badge/Neo4j-008CC1?style=for-the-badge&logo=neo4j&logoColor=white)](https://neo4j.com/)

</div>

---

## 📖 Overview

**Loom** is a Substack-style newsletter platform built on a cloud-native microservices architecture. Writers publish long-form newsletters; readers subscribe to their favourite writers and get personalised feeds. The platform is enriched by an AI assistant (`loom-intelligence-service`) that auto-summarises articles, suggests tags, generates titles, and powers semantic search — all backed by OpenAI and pgvector.

---

## 🏗️ Architecture

```
                          ┌─────────────┐
                          │   Client    │
                          └──────┬──────┘
                                 │ HTTP
                          ┌──────▼──────┐
                          │ API Gateway │  ← JWT auth filter
                          │  port 8080  │
                          └──────┬──────┘
                ┌────────────────┼─────────────────────┐
                │                │                     │
        ┌───────▼──────┐ ┌───────▼──────┐ ┌───────────▼─────────┐
        │ user-service │ │posts-service │ │connections-service  │
        │  port 9020   │ │  port 9010   │ │     port 9030       │
        │  PostgreSQL  │ │  PostgreSQL  │ │      Neo4j          │
        └───────┬──────┘ └───────┬──────┘ └─────────────────────┘
                │                │
                │  Kafka Events  │
       ┌────────▼────────────────▼────────┐
       │              Apache Kafka        │
       └─────┬───────────────────┬────────┘
             │                   │
   ┌─────────▼──────┐  ┌─────────▼──────────┐
   │notification-   │  │intelligence-service│
   │   service      │  │    port 9012       │
   │  port 9040     │  │ PostgreSQL+pgvector│
   │  PostgreSQL    │  │      OpenAI        │
   └────────────────┘  └────────────────────┘

        ┌──────────────────┐
        │ uploader-service │  ← Cloudinary / GCS
        │   port 9050      │
        └──────────────────┘
```

---

## 🚀 Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| `api-gateway` | 8080 | — | JWT validation, request routing, auth filter |
| `user-service` | 9020 | PostgreSQL | Signup, login, profile picture management |
| `posts-service` | 9010 | PostgreSQL | Newsletter CRUD, likes, comments, restacking, feeds |
| `connections-service` | 9030 | Neo4j | Writer subscriptions (directed graph), subscriber counts |
| `notification-service` | 9040 | PostgreSQL | Event-driven notifications via Kafka consumers |
| `uploader-service` | 9050 | — | File upload to Cloudinary / Google Cloud Storage |
| `intelligence-service` | 9012 | PostgreSQL + pgvector | AI summaries, tag suggestions, title generation, semantic search |
| `discover-server` | 8761 | — | Eureka service registry (local dev only) |

---

## ✨ Features

### 📰 Newsletter Platform
- **Publish newsletters** with title, subtitle, rich content, and cover images
- **Subscription feed** — see only posts from writers you follow
- **Explore feed** — discover global content sorted by recency
- **Restack** — reshare a post with your own commentary (like Substack Restacks)
- **Comments** — threaded discussion on every post
- **Likes** — one like per user per post, with unlike support

### 🤖 AI Intelligence (powered by OpenAI)
- **Auto-summarise** — generate a 280-character social snippet from a long newsletter
- **Tag suggestions** — extract 3–5 relevant keyword tags from content
- **Title & subtitle** — get AI-recommended headline and deck
- **Semantic search** — find thematically similar posts using pgvector embeddings

### 🔔 Event-Driven Notifications
- New publication → notify all subscribers
- Post liked → notify post owner
- Comment added → notify post owner
- Post restacked → notify original author

### 👤 User Management
- JWT-based authentication
- Profile picture upload via Cloudinary
- Directed subscription model (subscribe to writers, no mutual acceptance required)

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.3 |
| **Service Discovery** | Spring Cloud Netflix Eureka (local) |
| **API Gateway** | Spring Cloud Gateway |
| **Inter-service Comms** | OpenFeign (sync), Apache Kafka (async) |
| **Auth** | JWT (jjwt) |
| **Databases** | PostgreSQL 16, Neo4j (graph) |
| **AI / Vector Search** | Spring AI, OpenAI GPT-4o-mini, pgvector |
| **File Storage** | Cloudinary, Google Cloud Storage |
| **Containerisation** | Docker (Jib Maven Plugin) |
| **Orchestration** | Kubernetes (GKE) |
| **Build** | Maven + Maven Wrapper |

---

## 📁 Project Structure

```
Loom/
├── APIGateway/               # Spring Cloud Gateway + JWT filter
├── DiscoverServer/           # Eureka registry (local dev)
├── userService/              # Auth, user profiles
├── postsService/             # Posts, likes, comments, feeds
├── ConnectionsService/       # Neo4j subscription graph
├── notification-service/     # Kafka consumer → notifications
├── uploader-service/         # Cloudinary/GCS file uploads
├── intelligence-service/     # AI + vector search
├── k8s/                      # All Kubernetes manifests
│   ├── api-gateway.yml
│   ├── user-service.yml
│   ├── posts-service.yml
│   ├── connections-service.yml
│   ├── notification-service.yml
│   ├── uploader-service.yml
│   ├── intelligence-service.yml
│   ├── user-db.yml
│   ├── posts-db.yml
│   ├── notification-db.yml
│   ├── connections-db.yml
│   ├── kafka.yml
│   └── ingress.yml
└── .env                      # Local environment variables
```

---

## ⚙️ Local Development Setup

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker + Docker Compose
- PostgreSQL 16 running locally
- Neo4j running locally
- Apache Kafka running locally

### 1. Clone the repository
```bash
git clone https://github.com/aditighoshagd/Loom.git
cd Loom
```

### 2. Configure environment variables
Copy and fill in your credentials:
```bash
cp .env .env.local
```

Key variables to set:
```env
JWT_SECRET_KEY=your-secret-key

USER_DB_URL=jdbc:postgresql://localhost:5432/userDB
USER_DB_USERNAME=postgres
USER_DB_PASSWORD=your-password

POSTS_DB_URL=jdbc:postgresql://localhost:5432/postsDB
POSTS_DB_USERNAME=postgres
POSTS_DB_PASSWORD=your-password

NEO4J_URI=bolt://localhost:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=password

CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret

OPENAI_API_KEY=sk-...
```

### 3. Start services (in order)
```bash
# 1. Start Eureka
cd DiscoverServer && ./mvnw spring-boot:run

# 2. Start core services (each in its own terminal)
cd userService       && ./mvnw spring-boot:run
cd postsService      && ./mvnw spring-boot:run
cd ConnectionsService && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd uploader-service  && ./mvnw spring-boot:run
cd intelligence-service && ./mvnw spring-boot:run

# 3. Start the gateway last
cd APIGateway && ./mvnw spring-boot:run
```

---

## ☁️ Kubernetes Deployment (GKE)

### 1. Build & push Docker images
```bash
# Run inside each service directory
./mvnw compile jib:build
```

### 2. Connect to your GKE cluster
```bash
gcloud container clusters get-credentials <cluster-name> --region <region>
```

### 3. Create the Kubernetes Secret
Fill in your `.env` file with all real values, then:
```bash
kubectl create secret generic loom-secrets --from-env-file=.env
```

### 4. Deploy infrastructure
```bash
# Databases
kubectl apply -f k8s/user-db.yml
kubectl apply -f k8s/posts-db.yml
kubectl apply -f k8s/notification-db.yml
kubectl apply -f k8s/connections-db.yml

# Kafka
kubectl apply -f k8s/kafka.yml

# Wait for pods to be ready
kubectl get pods -w
```

### 5. Deploy microservices
```bash
kubectl apply -f k8s/user-service.yml
kubectl apply -f k8s/posts-service.yml
kubectl apply -f k8s/connections-service.yml
kubectl apply -f k8s/notification-service.yml
kubectl apply -f k8s/uploader-service.yml
kubectl apply -f k8s/intelligence-service.yml
```

### 6. Deploy API Gateway & Ingress
```bash
kubectl apply -f k8s/api-gateway.yml
kubectl apply -f k8s/ingress.yml
```

---

## 🔌 API Reference

All routes go through the API Gateway at `http://<gateway-host>`. All endpoints (except signup/login) require:
```
Authorization: Bearer <jwt-token>
```

### Authentication (`/api/v1/users`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/users/signup` | ❌ | Create a new account |
| `POST` | `/api/v1/users/login` | ❌ | Login and receive JWT |
| `PUT` | `/api/v1/users/core/profile-picture` | ✅ | Update profile picture URL |

### Posts (`/api/v1/posts`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/posts/core` | ✅ | Create a new post (multipart: post + optional image) |
| `GET` | `/api/v1/posts/core/{postId}` | ✅ | Get a post by ID |
| `GET` | `/api/v1/posts/core/users/{userId}/allPosts` | ✅ | Get all posts by a writer |
| `GET` | `/api/v1/posts/core/feed` | ✅ | Get personalised feed (subscribed writers) |
| `GET` | `/api/v1/posts/core/explore` | ✅ | Get global explore feed |
| `POST` | `/api/v1/posts/core/{postId}/restack` | ✅ | Restack a post |
| `POST` | `/api/v1/posts/core/{postId}/comments` | ✅ | Add a comment |
| `GET` | `/api/v1/posts/core/{postId}/comments` | ✅ | Get comments for a post |
| `POST` | `/api/v1/posts/likes/{postId}` | ✅ | Like a post |
| `DELETE` | `/api/v1/posts/likes/{postId}` | ✅ | Unlike a post |

### AI Features (`/api/v1/posts` — proxied to intelligence-service)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/posts/core/{postId}/ai-summary` | ✅ | Get AI-generated summary of a post |
| `GET` | `/api/v1/posts/core/{postId}/ai-tags` | ✅ | Get AI-suggested tags |
| `GET` | `/api/v1/posts/core/semantic-search?query=&limit=` | ✅ | Semantic similarity search |

### Connections (`/api/v1/connections`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/connections/core/request/{userId}` | ✅ | Subscribe to a writer |
| `POST` | `/api/v1/connections/core/reject/{userId}` | ✅ | Unsubscribe from a writer |
| `GET` | `/api/v1/connections/core/{userId}/first-degree` | ✅ | Get all writers a user follows |
| `GET` | `/api/v1/connections/core/{userId}/subscribers/count` | ✅ | Get subscriber count for a writer |

### File Uploads (`/api/v1/uploads`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/uploads/file` | ✅ | Upload a file (multipart/form-data), returns CDN URL |

---

## 🔐 Environment Variables Reference

| Variable | Service(s) | Description |
|---|---|---|
| `JWT_SECRET_KEY` | user-service, api-gateway | JWT signing secret |
| `USER_DB_URL` | user-service | PostgreSQL JDBC URL for user database |
| `USER_DB_USERNAME` | user-service | DB username |
| `USER_DB_PASSWORD` | user-service | DB password |
| `POSTS_DB_URL` | posts-service, intelligence-service | PostgreSQL JDBC URL |
| `POSTS_DB_USERNAME` | posts-service, intelligence-service | DB username |
| `POSTS_DB_PASSWORD` | posts-service, intelligence-service | DB password |
| `NOTIFICATION_DB_URL` | notification-service | PostgreSQL JDBC URL |
| `NOTIFICATION_DB_USERNAME` | notification-service | DB username |
| `NOTIFICATION_DB_PASSWORD` | notification-service | DB password |
| `NEO4J_URI` | connections-service | Neo4j bolt URI |
| `NEO4J_USERNAME` | connections-service | Neo4j username |
| `NEO4J_PASSWORD` | connections-service | Neo4j password |
| `CLOUDINARY_CLOUD_NAME` | uploader-service | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | uploader-service | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | uploader-service | Cloudinary API secret |
| `OPENAI_API_KEY` | intelligence-service | OpenAI API key |

---

## 📊 Kafka Topics

| Topic | Producer | Consumers | Payload |
|---|---|---|---|
| `user_created_topic` | user-service | connections-service | `UserCreatedEvent { userId, name }` |
| `post_created_topic` | posts-service | notification-service, intelligence-service | `PostCreated { postId, userId, ownerUserId, content, title }` |
| `post_liked_topic` | posts-service | notification-service | `PostLiked { postId, ownerUserId, likedByUserId }` |
| `comment_created_topic` | posts-service | notification-service | `CommentCreatedEvent { commentId, postId, commenterUserId, postOwnerUserId, content }` |
| `post_restacked_topic` | posts-service | notification-service | `PostRestackedEvent { postId, originalPostId, restackedByUserId, ownerUserId }` |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Push and open a Pull Request

---

<div align="center">

Built with ❤️ using Spring Boot, Kafka, and OpenAI

</div>
