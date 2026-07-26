<div align="center">

<img src="https://img.shields.io/badge/🧵-Loom-6366f1?style=for-the-badge&labelColor=1e1b4b&color=6366f1" height="50" alt="Loom"/>

# 🧵 Loom

### A cloud-native newsletter platform built with microservices

*Write. Publish. Grow your audience.*

<br/>

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.3-6DB33F?style=for-the-badge&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-GKE-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)](https://cloud.google.com/kubernetes-engine)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Neo4j](https://img.shields.io/badge/Neo4j-Graph_DB-008CC1?style=for-the-badge&logo=neo4j&logoColor=white)](https://neo4j.com/)
[![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o_mini-412991?style=for-the-badge&logo=openai&logoColor=white)](https://openai.com/)
[![Docker](https://img.shields.io/badge/Docker-Jib-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

<br/>

[![Live](https://img.shields.io/badge/🌐_Live-loom.solvix.buzz-10b981?style=for-the-badge)](https://loom.solvix.buzz)
[![Frontend Repo](https://img.shields.io/badge/Frontend-Loom--Frontend-6366f1?style=for-the-badge&logo=github)](https://github.com/aditighoshagd/Loom-Frontend)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Services](#-services)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Kafka Event Flow](#-kafka-event-flow)
- [API Reference](#-api-reference)
- [Environment Variables](#-environment-variables)
- [Local Development](#-local-development)
- [GKE Deployment](#-kubernetes-deployment-gke)
- [Contributing](#-contributing)

---

## 📖 Overview

**Loom** is an open-source, Substack-inspired newsletter platform built entirely on a **cloud-native microservices architecture**. It is designed for writers who want to publish long-form newsletters and for readers who want a curated, personalised reading experience.

What makes Loom different:

- **Event-driven backbone** — all cross-service communication happens asynchronously through Apache Kafka, keeping services fully decoupled
- **Graph-based social model** — subscriptions are modelled as a directed graph in Neo4j, making follower/subscriber queries blazing fast
- **AI-first content layer** — every published post is automatically indexed as a pgvector embedding. Writers get AI-generated summaries, tags, and titles. Readers get semantic search
- **Production-grade deployment** — the entire stack runs on GKE with Google-managed SSL, Kubernetes secrets, and persistent volume claims for all databases

---

## 🏗️ Architecture

```
                         ┌───────────────────────────────────┐
                         │           CLIENT (Browser)        │
                         │    React 19 · Vite · Tailwind     │
                         │        loom.solvix.buzz            │
                         └─────────────────┬─────────────────┘
                                           │ HTTPS (port 443)
                                           │ Google Managed SSL
                         ┌─────────────────▼─────────────────┐
                         │         GCE Ingress (GKE)         │
                         │  routes: /api/v1 → api-gateway    │
                         │          /       → frontend        │
                         └─────────────────┬─────────────────┘
                                           │
                         ┌─────────────────▼─────────────────┐
                         │           API Gateway              │
                         │    Spring Cloud Gateway  :8080     │
                         │  ┌────────────────────────────┐   │
                         │  │  JWT Authentication Filter  │   │
                         │  └─────────────┬──────────────┘   │
                         └────────────────┼───────────────────┘
              ┌──────────────────┬────────┴────────┬───────────────────┐
              │                  │                  │                   │
    ┌─────────▼────────┐ ┌───────▼──────┐ ┌────────▼────────┐ ┌───────▼────────┐
    │   user-service   │ │posts-service │ │connections-     │ │uploader-       │
    │    port 9020     │ │  port 9010   │ │service :9030    │ │service :9050   │
    │   PostgreSQL     │ │  PostgreSQL  │ │   Neo4j Graph   │ │Cloudinary/GCS  │
    └─────────┬────────┘ └───────┬──────┘ └─────────────────┘ └────────────────┘
              │                  │
              │   Kafka Events   │
     ┌────────▼──────────────────▼────────────────────┐
     │                 Apache Kafka                    │
     │  Topics: user_created · post_created            │
     │          post_liked · comment_created           │
     │          post_restacked                         │
     └──────────┬──────────────────────┬──────────────┘
                │                      │
     ┌──────────▼───────┐   ┌──────────▼────────────┐
     │notification-     │   │ intelligence-service   │
     │service  :9040    │   │      port 9012         │
     │  PostgreSQL      │   │ PostgreSQL + pgvector  │
     │  (Kafka consumer)│   │  OpenAI GPT-4o-mini    │
     └──────────────────┘   └────────────────────────┘

     ┌─────────────────────┐
     │   discover-server   │  ← Eureka registry (local dev only)
     │      port 8761      │
     └─────────────────────┘
```

> **Note:** In production on GKE, service discovery happens via Kubernetes DNS (`lb://SERVICE-NAME` is replaced with `http://service-name.default.svc.cluster.local`). Eureka is not used in production.

---

## 🚀 Services

<details>
<summary><strong>🔀 api-gateway</strong> — Port 8080 · Spring Cloud Gateway</summary>

The single entry point for all client requests. Responsible for:
- **JWT validation** via a custom `AuthenticationFilter` that intercepts all protected routes
- **Request routing** to downstream services based on path prefixes
- **Load balancing** using Spring Cloud LoadBalancer (Eureka in local, Kubernetes DNS in GKE)
- Public routes (`/api/v1/users/signup`, `/api/v1/users/login`) bypass the auth filter
- All other routes require a valid `Authorization: Bearer <token>` header

</details>

<details>
<summary><strong>👤 user-service</strong> — Port 9020 · PostgreSQL</summary>

Handles all user identity and authentication concerns:
- **Signup** — creates a new user, hashes password with BCrypt, publishes `UserCreatedEvent` to Kafka
- **Login** — validates credentials, issues signed JWT with `userId` in the subject claim
- **Profile picture** — accepts a URL (from `uploader-service`) and persists it on the user record
- JWT secret is shared with `api-gateway` for token verification

</details>

<details>
<summary><strong>📝 posts-service</strong> — Port 9010 · PostgreSQL</summary>

The core content engine. Handles:
- **Newsletter CRUD** — create, read, update, delete posts with multipart image support
- **Personalised feed** — fetches a list of followed writer IDs from `connections-service` via OpenFeign, returns their posts sorted by date
- **Explore feed** — returns all posts globally, most recent first
- **Likes** — one like per user per post; toggle unlike supported
- **Comments** — threaded comment creation and retrieval
- **Restack** — reshare another writer's post with attribution
- Publishes Kafka events for `post_created`, `post_liked`, `comment_created`, `post_restacked`

</details>

<details>
<summary><strong>🔗 connections-service</strong> — Port 9030 · Neo4j Graph Database</summary>

Models the social graph between users. All relationships are directed (writer → reader):
- **Subscribe** — creates a directed `FOLLOWS` relationship in Neo4j
- **Unsubscribe** — removes the relationship
- **First-degree follows** — returns all users that a given user follows
- **Subscriber count** — counts incoming `FOLLOWS` relationships for a user
- Listens for `UserCreatedEvent` from Kafka to automatically create user nodes in Neo4j

</details>

<details>
<summary><strong>🔔 notification-service</strong> — Port 9040 · PostgreSQL</summary>

Purely event-driven. Has no REST endpoints; everything is triggered by Kafka:
- `post_created_topic` → notify all subscribers of the writer
- `post_liked_topic` → notify post owner that their post was liked
- `comment_created_topic` → notify post owner of a new comment
- `post_restacked_topic` → notify original author their post was restacked
- Persists all notifications to PostgreSQL for retrieval via a REST read endpoint

</details>

<details>
<summary><strong>📤 uploader-service</strong> — Port 9050 · Cloudinary / Google Cloud Storage</summary>

Stateless file upload service that accepts `multipart/form-data` and returns a CDN URL:
- Supports both **Cloudinary** (image optimisation, CDN) and **Google Cloud Storage** (buckets)
- Returns a public URL that callers (e.g. `posts-service`) store on their entities
- Configured via environment variables to switch between storage providers

</details>

<details>
<summary><strong>🤖 intelligence-service</strong> — Port 9012 · PostgreSQL + pgvector + OpenAI</summary>

The AI brain of Loom. Listens to Kafka for new posts and processes them:
- **Embedding generation** — calls OpenAI embeddings API and stores the vector in pgvector
- **AI Summary** — uses GPT-4o-mini to generate a 280-character social-ready snippet
- **Tag suggestions** — extracts 3–5 keyword tags from post content
- **Title & subtitle generation** — recommends headline and deck copy
- **Semantic search** — cosine similarity search over pgvector embeddings to find thematically similar posts

</details>

<details>
<summary><strong>🔎 discover-server</strong> — Port 8761 · Eureka (Local Only)</summary>

Netflix Eureka service registry used only in local development. All services register here so they can discover each other by name (e.g. `lb://POSTS-SERVICE`). Not deployed on GKE — replaced by Kubernetes DNS.

</details>

---

## ✨ Features

### 📰 Newsletter Publishing
| Feature | Description |
|---|---|
| Rich Newsletters | Title, subtitle, rich content body, and optional cover image upload |
| Subscription Feed | Personalised feed showing only posts from writers you follow |
| Explore Feed | Global feed of all posts, sorted by most recent |
| Restack | Reshare any post with your own commentary (like Substack Restacks) |
| Comments | Threaded discussion section on every post |
| Likes | One like per user per post, with unlike support |

### 🤖 AI Intelligence (GPT-4o-mini + pgvector)
| Feature | Description |
|---|---|
| Auto-Summary | 280-character social snippet generated from the full article |
| Tag Suggestions | 3–5 keyword tags extracted from post content |
| Title Generation | AI-recommended headline and subtitle |
| Semantic Search | Find thematically related posts using cosine similarity on pgvector embeddings |

### 🔔 Real-time Notifications (Kafka)
| Trigger | Recipient |
|---|---|
| New publication | All subscribers of the writer |
| Post liked | Post owner |
| Comment added | Post owner |
| Post restacked | Original post author |

### 👤 User & Social Graph
| Feature | Description |
|---|---|
| JWT Authentication | Stateless auth; JWT issued on login, verified at the gateway |
| Profile Pictures | Upload via Cloudinary, stored as a URL on the user record |
| Directed Subscriptions | Follow writers without requiring mutual acceptance |
| Subscriber Counts | Fast graph queries via Neo4j for follower metrics |

---

## 🛠️ Tech Stack

| Category | Technology | Version | Notes |
|---|---|---|---|
| **Language** | Java | 21 | Virtual threads via Project Loom (fitting name 😄) |
| **Framework** | Spring Boot | 3.3.3 | Web, Data JPA, Security, Actuator |
| **API Gateway** | Spring Cloud Gateway | 4.x | Reactive, non-blocking |
| **Service Discovery** | Netflix Eureka | Spring Cloud 2023.x | Local dev only |
| **Inter-service (sync)** | OpenFeign | Spring Cloud | posts → connections subscription lookup |
| **Messaging (async)** | Apache Kafka | — | 5 topics, decoupled event flow |
| **Auth** | JWT (jjwt) | 0.12.x | HS256 signed, userId in subject claim |
| **ORM** | Spring Data JPA + Hibernate | — | PostgreSQL services |
| **Graph DB** | Spring Data Neo4j | — | connections-service only |
| **Relational DB** | PostgreSQL | 16 | user, posts, notification, intelligence |
| **Vector DB** | pgvector extension | 0.8.x | Semantic search in intelligence-service |
| **AI** | Spring AI + OpenAI | GPT-4o-mini | Summaries, tags, embeddings |
| **File Storage** | Cloudinary + Google Cloud Storage | — | Profile pics & post images |
| **Containerisation** | Docker via Jib Maven Plugin | — | No Dockerfile needed for backend |
| **Orchestration** | Kubernetes (GKE) | 1.35.x | Autopilot cluster |
| **Ingress** | GCE Ingress + Google Managed SSL | — | HTTPS via `loom.solvix.buzz` |
| **Build** | Maven + Maven Wrapper | 3.9.x | `./mvnw` in each service |
| **Frontend** | React 19 + Vite 8 + Tailwind 4 | — | [Loom-Frontend](https://github.com/aditighoshagd/Loom-Frontend) |

---

## 📁 Project Structure

```
Loom/
├── APIGateway/                        # Spring Cloud Gateway + JWT filter
│   └── src/main/java/com/loom/
│       ├── filter/AuthenticationFilter.java
│       └── util/JwtUtil.java
│
├── DiscoverServer/                    # Eureka service registry (local dev)
│
├── userService/                       # Auth, user profiles
│   └── src/main/java/com/loom/userService/
│       ├── controller/
│       │   ├── AuthController.java    # POST /signup, POST /login
│       │   └── UserCoreController.java # PUT /profile-picture
│       ├── service/AuthService.java
│       ├── entity/User.java
│       └── event/UserCreatedEvent.java
│
├── postsService/                      # Posts, likes, comments, feeds
│   └── src/main/java/com/loom/postsService/
│       ├── controller/
│       │   ├── PostsController.java   # CRUD, feed, explore, restack
│       │   ├── LikesController.java
│       │   └── CommentsController.java
│       ├── client/
│       │   ├── ConnectionsServiceClient.java  # Feign: get followed writers
│       │   └── IntelligenceServiceClient.java # Feign: AI features
│       └── event/
│           ├── PostCreated.java
│           ├── PostLiked.java
│           ├── PostRestackedEvent.java
│           └── CommentCreatedEvent.java
│
├── ConnectionsService/                # Neo4j subscription graph
│   └── src/main/java/com/loom/connectionsService/
│       ├── controller/ConnectionsController.java
│       ├── service/ConnectionsService.java
│       └── consumer/UserCreatedConsumer.java  # Creates Neo4j nodes
│
├── notification-service/              # Kafka-driven notifications
│   └── src/main/java/com/loom/notification_service/
│       ├── consumer/PostsConsumer.java  # Listens to all post events
│       └── service/NotificationService.java
│
├── uploader-service/                  # File uploads → CDN URL
│   └── src/main/java/com/loom/uploader_service/
│       ├── UploaderController.java
│       └── service/
│           ├── CloudinaryUploaderService.java
│           └── GoogleCloudStorageUploaderService.java
│
├── intelligence-service/              # AI + pgvector semantic search
│   └── src/main/java/com/loom/intelligence_service/
│       ├── controller/IntelligenceController.java
│       └── service/IntelligenceService.java  # OpenAI + pgvector
│
├── k8s/                               # All Kubernetes manifests
│   ├── api-gateway.yml
│   ├── user-service.yml
│   ├── posts-service.yml
│   ├── connections-service.yml
│   ├── notification-service.yml
│   ├── uploader-service.yml
│   ├── intelligence-service.yml
│   ├── frontend.yml
│   ├── certificate.yml               # Google Managed SSL for loom.solvix.buzz
│   ├── ingress.yml                   # GCE Ingress (HTTP + HTTPS)
│   ├── user-db.yml                   # PostgreSQL StatefulSet
│   ├── posts-db.yml
│   ├── notification-db.yml
│   ├── connections-db.yml            # Neo4j StatefulSet
│   └── kafka.yml                     # Kafka StatefulSet (2 replicas)
│
└── .env                              # Local environment variables (do not commit)
```

---

## 📊 Kafka Event Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Event Flow Diagram                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  user-service ──[UserCreatedEvent]──────► connections-service           │
│               (userId, name)               (creates Neo4j user node)    │
│                                                                         │
│  posts-service ─[PostCreated]──────────► notification-service           │
│               (postId, userId, title)       (notify all subscribers)    │
│                                   └──────► intelligence-service         │
│                                             (generate embedding + index) │
│                                                                         │
│  posts-service ─[PostLiked]────────────► notification-service           │
│               (postId, ownerUserId)         (notify post owner)         │
│                                                                         │
│  posts-service ─[CommentCreated]───────► notification-service           │
│               (commentId, postOwner)        (notify post owner)         │
│                                                                         │
│  posts-service ─[PostRestacked]────────► notification-service           │
│               (postId, originalAuthor)      (notify original author)    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

| Topic | Producer | Consumer(s) | Event Payload |
|---|---|---|---|
| `user_created_topic` | user-service | connections-service | `{ userId, name }` |
| `post_created_topic` | posts-service | notification-service, intelligence-service | `{ postId, userId, ownerUserId, content, title }` |
| `post_liked_topic` | posts-service | notification-service | `{ postId, ownerUserId, likedByUserId }` |
| `comment_created_topic` | posts-service | notification-service | `{ commentId, postId, commenterUserId, postOwnerUserId, content }` |
| `post_restacked_topic` | posts-service | notification-service | `{ postId, originalPostId, restackedByUserId, ownerUserId }` |

---

## 🔌 API Reference

All routes go through the **API Gateway** at `https://loom.solvix.buzz`. Protected routes require:
```http
Authorization: Bearer <jwt-token>
```

<details>
<summary><strong>🔐 Authentication — <code>/api/v1/users</code></strong></summary>

| Method | Endpoint | Auth Required | Request Body | Description |
|---|---|---|---|---|
| `POST` | `/api/v1/users/signup` | ❌ | `{ name, email, password }` | Create a new account. Publishes `UserCreatedEvent` |
| `POST` | `/api/v1/users/login` | ❌ | `{ email, password }` | Returns signed JWT |
| `PUT` | `/api/v1/users/core/profile-picture` | ✅ | `{ profilePictureUrl }` | Update profile picture URL |

**Signup Response:**
```json
{
  "userId": "uuid",
  "name": "Aditi Ghosh",
  "email": "aditi@example.com"
}
```

**Login Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "uuid"
}
```

</details>

<details>
<summary><strong>📝 Posts — <code>/api/v1/posts</code></strong></summary>

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/posts/core` | ✅ | Create a post. `multipart/form-data` with `post` (JSON) and optional `image` (file) |
| `GET` | `/api/v1/posts/core/{postId}` | ✅ | Get a single post by ID |
| `GET` | `/api/v1/posts/core/users/{userId}/allPosts` | ✅ | All posts by a writer |
| `GET` | `/api/v1/posts/core/feed` | ✅ | Personalised feed from followed writers |
| `GET` | `/api/v1/posts/core/explore` | ✅ | Global explore feed (all posts, newest first) |
| `POST` | `/api/v1/posts/core/{postId}/restack` | ✅ | Restack a post with optional commentary |
| `POST` | `/api/v1/posts/core/{postId}/comments` | ✅ | Add a comment |
| `GET` | `/api/v1/posts/core/{postId}/comments` | ✅ | Get all comments for a post |
| `POST` | `/api/v1/posts/likes/{postId}` | ✅ | Like a post |
| `DELETE` | `/api/v1/posts/likes/{postId}` | ✅ | Unlike a post |

**Create Post Request (`multipart/form-data`):**
```
post: { "title": "My Newsletter", "subtitle": "A brief intro", "content": "Full body..." }
image: <binary file>  (optional)
```

</details>

<details>
<summary><strong>🤖 AI Features — <code>/api/v1/posts</code> (proxied to intelligence-service)</strong></summary>

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/posts/core/{postId}/ai-summary` | ✅ | AI-generated 280-character summary |
| `GET` | `/api/v1/posts/core/{postId}/ai-tags` | ✅ | 3–5 AI-suggested keyword tags |
| `GET` | `/api/v1/posts/core/semantic-search?query=&limit=` | ✅ | Semantic cosine-similarity search |

**Semantic Search Example:**
```
GET /api/v1/posts/core/semantic-search?query=machine+learning+startups&limit=5
```

</details>

<details>
<summary><strong>🔗 Connections — <code>/api/v1/connections</code></strong></summary>

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/connections/core/request/{userId}` | ✅ | Subscribe to a writer (creates Neo4j edge) |
| `POST` | `/api/v1/connections/core/reject/{userId}` | ✅ | Unsubscribe from a writer (removes Neo4j edge) |
| `GET` | `/api/v1/connections/core/{userId}/first-degree` | ✅ | All writers a user follows |
| `GET` | `/api/v1/connections/core/{userId}/subscribers/count` | ✅ | Subscriber count for a writer |

</details>

<details>
<summary><strong>📤 File Uploads — <code>/api/v1/uploads</code></strong></summary>

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/uploads/file` | ✅ | Upload a file, returns CDN URL |

**Upload Request (`multipart/form-data`):**
```
file: <binary image>
```

**Upload Response:**
```json
{
  "url": "https://res.cloudinary.com/your-cloud/image/upload/v.../filename.jpg"
}
```

</details>

---

## 🔐 Environment Variables

<details>
<summary><strong>View all environment variables</strong></summary>

| Variable | Service(s) | Description |
|---|---|---|
| `JWT_SECRET_KEY` | user-service, api-gateway | HS256 JWT signing secret (min. 256 bits) |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | all services | Eureka URL (local dev only) |
| `USER_DB_URL` | user-service | PostgreSQL JDBC URL |
| `USER_DB_USERNAME` | user-service | DB username |
| `USER_DB_PASSWORD` | user-service | DB password |
| `POSTS_DB_URL` | posts-service, intelligence-service | PostgreSQL JDBC URL |
| `POSTS_DB_USERNAME` | posts-service, intelligence-service | DB username |
| `POSTS_DB_PASSWORD` | posts-service, intelligence-service | DB password |
| `NOTIFICATION_DB_URL` | notification-service | PostgreSQL JDBC URL |
| `NOTIFICATION_DB_USERNAME` | notification-service | DB username |
| `NOTIFICATION_DB_PASSWORD` | notification-service | DB password |
| `NEO4J_URI` | connections-service | Neo4j bolt URI e.g. `bolt://localhost:7687` |
| `NEO4J_USERNAME` | connections-service | Neo4j username |
| `NEO4J_PASSWORD` | connections-service | Neo4j password |
| `CLOUDINARY_CLOUD_NAME` | uploader-service | Your Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | uploader-service | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | uploader-service | Cloudinary API secret |
| `GCLOUD_STORAGE_BUCKET_NAME` | uploader-service | GCS bucket name |
| `GCLOUD_CREDENTIALS_JSON` | uploader-service | GCP service account JSON (inline) |
| `OPENAI_API_KEY` | intelligence-service | OpenAI API key (`sk-...`) |
| `USER_SERVICE_URI` | api-gateway | Override for K8s DNS (e.g. `http://user-service`) |
| `POSTS_SERVICE_URI` | api-gateway | Override for K8s DNS |
| `CONNECTIONS_SERVICE_URI` | api-gateway | Override for K8s DNS |
| `INTELLIGENCE_SERVICE_URI` | api-gateway | Override for K8s DNS |
| `UPLOADER_SERVICE_URI` | api-gateway | Override for K8s DNS |

</details>

---

## ⚙️ Local Development

### Prerequisites

Make sure you have these running locally:

| Dependency | Version | Notes |
|---|---|---|
| Java | 21+ | Required for all services |
| Maven | 3.9+ | Or use the bundled `./mvnw` wrapper |
| PostgreSQL | 16 | Create 4 databases: `userDB`, `postsDB`, `notificationDB`, and a shared one for intelligence |
| Neo4j | 5.x | For connections-service |
| Apache Kafka | 3.x | With Zookeeper or KRaft |

### 1. Clone the repository
```bash
git clone https://github.com/aditighoshagd/Loom.git
cd Loom
```

### 2. Set up environment variables
```bash
cp .env .env.local
# Fill in .env.local with your real credentials
```

### 3. Create the PostgreSQL databases
```sql
CREATE DATABASE "userDB";
CREATE DATABASE "postsDB";
CREATE DATABASE "notificationDB";
```

### 4. Start services (recommended order)

Start each in a separate terminal window:

```bash
# Step 1 — Service registry (must start first)
cd DiscoverServer && ./mvnw spring-boot:run

# Step 2 — Core domain services (any order)
cd userService         && ./mvnw spring-boot:run
cd postsService        && ./mvnw spring-boot:run
cd ConnectionsService  && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd uploader-service    && ./mvnw spring-boot:run
cd intelligence-service && ./mvnw spring-boot:run

# Step 3 — API Gateway (start last)
cd APIGateway && ./mvnw spring-boot:run
```

> **Tip:** Use `kubectl port-forward` to test against your GKE cluster locally without exposing internal services.

### 5. Start the frontend
```bash
git clone https://github.com/aditighoshagd/Loom-Frontend.git
cd Loom-Frontend
echo "VITE_API_BASE_URL=http://localhost:8080" > .env.local
npm install && npm run dev
```

---

## ☁️ Kubernetes Deployment (GKE)

### 1. Build & push Docker images using Jib
```bash
# Run inside each service directory (no Dockerfile needed)
cd userService && ./mvnw compile jib:build
cd postsService && ./mvnw compile jib:build
cd ConnectionsService && ./mvnw compile jib:build
cd notification-service && ./mvnw compile jib:build
cd uploader-service && ./mvnw compile jib:build
cd intelligence-service && ./mvnw compile jib:build
cd APIGateway && ./mvnw compile jib:build
```

### 2. Connect to your GKE cluster
```bash
gcloud container clusters get-credentials <cluster-name> --region <region>
```

### 3. Create the Kubernetes Secret
```bash
kubectl create secret generic loom-secrets --from-env-file=.env
```

### 4. Deploy stateful infrastructure (databases + Kafka)
```bash
kubectl apply -f k8s/user-db.yml
kubectl apply -f k8s/posts-db.yml
kubectl apply -f k8s/notification-db.yml
kubectl apply -f k8s/connections-db.yml
kubectl apply -f k8s/kafka.yml

# Wait until all pods are Running
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

### 6. Deploy frontend
```bash
kubectl apply -f k8s/frontend.yml
```

### 7. Deploy API Gateway, SSL Certificate & Ingress
```bash
kubectl apply -f k8s/api-gateway.yml
kubectl apply -f k8s/certificate.yml   # Google Managed SSL for loom.solvix.buzz
kubectl apply -f k8s/ingress.yml       # GCE Ingress (HTTP + HTTPS)
```

> **SSL Note:** The Google Managed Certificate takes ~10–20 minutes to provision. During this time `http://` works but `https://` may show errors.

### 8. Verify the deployment
```bash
kubectl get pods
kubectl get ingress
kubectl get managedcertificate loom-cert
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Write clean, well-documented code
4. Commit your changes with meaningful messages
5. Push and open a Pull Request

---

## 📜 License

This project is open source. Feel free to use it as a reference for building cloud-native microservices.

---

<div align="center">

Built with ❤️ using Spring Boot, Apache Kafka, Neo4j, and OpenAI

**[🌐 loom.solvix.buzz](https://loom.solvix.buzz)** · **[Frontend Repo](https://github.com/aditighoshagd/Loom-Frontend)**

</div>
