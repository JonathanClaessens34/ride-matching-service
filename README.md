# Ride Matching Service

## Overview

This project implements a thread-safe, in-memory ride matching service.

The system assigns the nearest available driver to incoming ride requests using Euclidean distance. Driver allocation is atomic to ensure that a driver cannot be assigned to multiple rides concurrently.

The application is built using Java 17 and Spring Boot.

---

## 🛠️ Tech Stack

| Component | Version |
|-----------|---------|
| **Java** | 17+ |
| **Spring Boot** | 4.0.2 |
| **Build Tool** | Maven |
| **Testing** | JUnit 5 |
| **Concurrency** | ConcurrentHashMap, AtomicBoolean |

---

## 🏗️ Architecture

The project follows a **layered architecture**:

```
Controllers
    ↓
Services (Business Logic)
    ↓
Repositories (Data Storage)
    ↓
Models (Domain Objects)
```

- **Controller** – REST API endpoints
- **Service** – Business logic and matching algorithm
- **Repository** – Thread-safe in-memory storage
- **Model** – Domain objects (Driver, Ride, Location)
- **DTO** – Request/response objects

All data is stored in memory using `ConcurrentHashMap`.

---

## 💡 Design Decisions

### In-Memory Storage

Drivers and rides are stored using `ConcurrentHashMap`. No persistence layer is used, as the assignment specifies an in-memory solution.

### Thread Safety Strategy

Driver allocation is implemented using:

- **`AtomicBoolean`** – Represents driver availability
- **`compareAndSet(true, false)`** – Atomically claims a driver

**Guarantees:**
- ✅ A driver cannot be assigned to more than one ride
- ✅ Concurrent ride requests are handled safely
- ✅ No explicit locks are required

### Matching Algorithm

When a ride is requested:

1. Retrieve all available drivers
2. Sort drivers by Euclidean distance to the pickup location
3. Attempt to atomically allocate each driver in order
4. Assign the first successfully claimed driver

**Time Complexity:** `O(n log n)` due to sorting

> **Note:** In a production system, a spatial index (e.g., KD-tree or geohashing) would improve scalability.

---

## 📡 API Endpoints

### 1️⃣ Register Driver

**`POST /drivers`**

**Request:**
```json
{
  "driverId": "driver-1",
  "x": 10,
  "y": 20
}
```

**Response:** `200 OK`

---

### 2️⃣ Update Driver (Location & Availability)

**`PUT /drivers/{driverId}`**

**Request:**
```json
{
  "x": 15,
  "y": 25,
  "available": true
}
```

**Response:**
```json
{
  "id": "driver-1",
  "x": 15,
  "y": 25,
  "available": true
}
```

---

### 3️⃣ Request Ride

**`POST /rides`**

**Request:**
```json
{
  "riderId": "rider-1",
  "x": 12,
  "y": 22
}
```

**Response:**
```json
{
  "rideId": "ride-123",
  "driverId": "driver-1",
  "riderId": "rider-1",
  "pickupX": 12,
  "pickupY": 22
}
```

---

### 4️⃣ Complete Ride

**`POST /rides/{rideId}/complete`**

Marks the ride as completed and makes the driver available again.

**Response:** `200 OK`

---

## 🚀 Running the Application

### Prerequisites

- **Java 17+**
- **Maven 3.6+**

### Setup (First Time Only)

First, navigate to the project directory where `pom.xml` is located:

```bash
cd ride-matching-service
```

Then run the build command to download dependencies:

```bash
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

Application starts at: **http://localhost:8080**

---

## 🧪 Running Tests

```bash
mvn test
```

**Tests Cover:**

- ✅ Driver registration and updates
- ✅ Matching logic (nearest driver selection)
- ✅ Ride lifecycle
- ✅ Edge cases (no available drivers)
- ✅ Concurrency safety (atomic allocation)

---

## 📋 Assumptions

- Each driver can handle only one ride at a time
- Driver IDs must be unique
- Rider accounts are not modeled (riderId is treated as input data)
- Distance is calculated using Euclidean distance
- Data is not persisted between restarts

---

## 🔮 Possible Improvements

- Spatial indexing for large numbers of drivers (KD-tree, R-tree)
- Persistent storage (relational database with JPA)
- Structured error responses with detailed error codes
- Real-time driver location updates (WebSocket)
- Driver rating and review system
- Advanced matching algorithms (fairness, efficiency)
