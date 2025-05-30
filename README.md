# Bilet Al - Transportation Ticket Booking System

Bilet Al is a comprehensive backend system for a transportation ticket booking platform that allows customers to purchase tickets for flights and bus journeys. The project is implemented using **Java Spring Boot 3.4.4** with **PostgreSQL** database and provides a robust, secure, and scalable solution for transportation management.

## 🚀 Technology Stack

- **Framework**: Spring Boot 3.4.4
- **Language**: Java 17
- **Database**: PostgreSQL
- **Security**: JWT (JSON Web Tokens) with role-based authorization
- **Documentation**: Swagger/OpenAPI 3
- **Email**: Spring Boot Mail with HTML templates
- **Build Tool**: Maven
- **Architecture**: RESTful API with layered architecture

## 📋 Key Features

- **Multi-Modal Transportation**: Support for both flights and bus expeditions
- **Secure Authentication**: JWT-based authentication with role-based access control
- **Email Integration**: User registration, activation, and password reset via email
- **Advanced Search**: Dynamic filtering and pagination using JPA Specifications
- **Seat Management**: Comprehensive seat reservation system
- **Admin Panel**: Full administrative control over transportation schedules
- **Token Management**: Secure token whitelist system for session management
- **Soft Delete**: Data preservation with soft delete patterns

## 🏗️ System Architecture

The application follows a clean, layered architecture pattern:

```
┌─────────────────┐
│   Controllers   │ ← REST API Layer (HTTP requests/responses)
├─────────────────┤
│    Services     │ ← Business Logic Layer
├─────────────────┤
│  Repositories   │ ← Data Access Layer (JPA/Hibernate)
├─────────────────┤
│    Database     │ ← PostgreSQL Database
└─────────────────┘
```

## 📦 Project Structure
```plaintext
--com.biletal.biletalbackend
  |-- config
  |   |-- SecurityConfig.java
  |   |-- DatabaseInitializer.java
  |   |-- SwaggerConfig.java
  |
  |-- controller
  |   |-- AuthController.java
  |   |-- UserController.java
  |
  |-- dto
  |   |-- ApiResponseDto.java
  |   |-- AdminUpdateRequest.java
  |   |-- LoginRequest.java
  |   |-- LoginResponse.java
  |   |-- PasswordRequest.java
  |   |-- RegistrationRequest.java
  |   |-- UserResponseDto.java
  |   |-- UserUpdateRequest.java
  |
  |-- custenum
  |   |-- Gender.java
  |   |-- Role.java
  |
  |-- exception
  |   |-- GlobalExceptionHandler.java
  |
  |-- model
  |   |-- RegistrationToken.java
  |   |-- User.java
  |
  |-- repository
  |   |-- RegistrationTokenRepository.java
  |   |-- UserRepository.java
  |
  |-- security
  |   |-- CustomUserDetailsService.java
  |   |-- JwtService.java
  |   |-- JwtTokenFilter.java
  |   |-- SecurityLoggingFilter.java
  |   |-- TokenWhitelistService.java
  |
  |-- service
  |   |-- AuthService.java
  |   |-- UserService.java
  |
  |-- BiletalbackendApplication.java
  |
  |-- src/main/resources
      |-- application.properties
      |-- META-INF
          |-- additional-spring-configuration-metadata.json
      |-- templates
          |-- activate.html
```

# Feature Documentation

## Authentication and User Management

### User Registration Process
1. **End User Registration** (`/api/auth/register`): 
   - Users submit registration details via `RegistrationRequest` with name, email, etc.
   - System validates the request and checks for existing email addresses
   - A registration token is generated and sent to the user's email
   - Response contains a success message if registration is successful

2. **Account Activation** (`/activate`):
   - Users receive an email with an activation link containing a token
   - When the link is clicked, the system validates the token (checks if valid, expired, or already used)
   - Upon successful validation, the user is presented with a form to create a password
   - If the token is invalid, expired, or used, an appropriate error message is displayed

3. **Password Creation** (`/api/set-password`):
   - After activation, users set their password by submitting a `PasswordRequest`
   - The system validates the token and password requirements
   - Upon successful password creation, the user account is fully activated and ready to use

### Authentication

1. **End User Login** (`/api/auth/login`):
   - Users provide email and password via `LoginRequest`
   - The system authenticates credentials and assigns the END_USER role
   - Upon successful authentication, a JWT token is returned along with user information
   - Failed authentication returns appropriate error messages

2. **Admin Login** (`/api/auth/admin/login`):
   - Admins provide email and password via the same `LoginRequest` format
   - The system authenticates credentials and validates admin role (SYSTEM_ADMIN)
   - Upon successful authentication, a JWT token is returned with admin privileges
   - Failed authentication returns appropriate error messages

3. **Logout** (`/api/auth/logout`):
   - Users send their JWT token in the Authorization header
   - The system invalidates the token by removing it from the whitelist
   - Response confirms successful logout

4. **Get Current User** (`/api/auth/current-user`):
   - Authenticated users can retrieve their own profile information
   - The system extracts the user ID from the JWT token in the Authorization header
   - Upon successful validation, returns the user's information as a `UserResponseDto`
   - Failed authorization returns appropriate error messages with HTTP status codes

### User Account Management

1. **Update User Profile** (`/api/users/update`):
   - Authenticated end users can update their profile information
   - The system validates the JWT token to identify the user and verify END_USER role
   - Update request is validated and processed
   - Response confirms successful profile update or provides error details

2. **Delete User Account** (`/api/users/delete`):
   - Authenticated end users can delete their own account
   - The system performs a soft delete (marks account as deleted rather than removing from database)
   - JWT token is invalidated as part of the delete process
   - Response confirms successful account deletion

3. **Update Admin Account** (`/api/admin/update`):
   - Authenticated system admins can update their account information
   - The system validates the JWT token to identify the admin and verify SYSTEM_ADMIN role
   - Update request is validated and processed
   - Response confirms successful account update or provides error details

## Security Features

1. **JWT-based Authentication**:
   - All authenticated endpoints are secured using JWT tokens
   - Tokens contain user ID and role information for authorization
   - Tokens are validated for each protected request
   - Token whitelist mechanism prevents use of invalidated tokens

2. **Role-based Authorization**:
   - Different endpoints require specific roles (END_USER, ADMIN, SYSTEM_ADMIN)
   - Controllers verify user roles before processing requests
   - Unauthorized access attempts return appropriate error responses

3. **Exception Handling**:
   - Global exception handler processes common errors
   - Custom responses for validation errors, authentication failures, and authorization issues
   - Consistent error format across the API

## API Response Format

All API endpoints return responses in a consistent format:
- Success responses include a message and a boolean success flag
- Error responses include an error message and a false success flag
- HTTP status codes provide additional context about the response

## Data Validation

- Input validation is implemented for all request DTOs
- Common validation includes email format, required fields, password strength
- Validation errors are caught and returned with descriptive messages

## Authentication Flow

```
┌────────────┐     Registration     ┌────────────┐
│            │─────────────────────>│            │
│            │                      │            │
│            │       Email          │            │
│   User     │<─────────────────────│   System   │
│            │  with Activate Link  │            │
│            │                      │            │
│            │  Activate Account    │            │
│            │─────────────────────>│            │
│            │                      │            │
│            │   Set Password       │            │
│            │─────────────────────>│            │
│            │                      │            │
│            │       Login          │            │
│            │─────────────────────>│            │
│            │                      │            │
│            │    JWT Token         │            │
│            │<─────────────────────│            │
└────────────┘                      └────────────┘
```

```
src/
├── main/
│   ├── java/com/biletal/biletalbackend/
│   │   ├── BiletalbackendApplication.java      # Main Spring Boot application
│   │   ├── config/                             # Configuration classes
│   │   │   ├── DatabaseInitializer.java        # Database seeding
│   │   │   ├── SecurityConfig.java             # Security configuration
│   │   │   └── SwaggerConfig.java              # API documentation config
│   │   ├── controller/                         # REST API controllers
│   │   │   ├── AuthController.java             # Authentication endpoints
│   │   │   ├── BusExpeditionController.java    # Bus management endpoints
│   │   │   ├── FlightController.java           # Flight management endpoints
│   │   │   └── UserController.java             # User management endpoints
│   │   ├── custenum/                          # Custom enumerations
│   │   │   ├── BusType.java                   # Bus type enumeration
│   │   │   ├── Gender.java                    # Gender enumeration
│   │   │   └── Role.java                      # User role enumeration
│   │   ├── dto/                               # Data Transfer Objects
│   │   │   ├── request/                       # Request DTOs
│   │   │   └── response/                      # Response DTOs
│   │   ├── exception/                         # Custom exceptions
│   │   ├── model/                             # JPA entities
│   │   │   ├── User.java                      # User entity
│   │   │   ├── Flight.java                    # Flight entity
│   │   │   ├── BusExpedition.java             # Bus expedition entity
│   │   │   ├── RegistrationToken.java         # Email verification token
│   │   │   └── PasswordResetToken.java        # Password reset token
│   │   ├── repository/                        # Data access layer
│   │   ├── security/                          # Security components
│   │   │   ├── JwtService.java                # JWT token management
│   │   │   ├── JwtTokenFilter.java            # JWT filter
│   │   │   ├── TokenWhitelistService.java     # Token whitelist management
│   │   │   └── CustomUserDetailsService.java # User details service
│   │   ├── service/                           # Business logic layer
│   │   └── specification/                     # JPA specifications for queries
│   └── resources/
│       ├── application.properties             # Application configuration
│       ├── static/logo/                       # Brand assets
│       └── templates/                         # Email HTML templates
└── test/                                      # Test classes
```

## 🗄️ Database Schema

The system uses PostgreSQL with the following main entities:

### Core Entities

#### User Entity
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    gender VARCHAR(10),
    birth_date DATE,
    role VARCHAR(20) NOT NULL DEFAULT 'END_USER',
    is_active BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

#### Flight Entity
```sql
CREATE TABLE flights (
    id BIGSERIAL PRIMARY KEY,
    flight_number VARCHAR(10) UNIQUE NOT NULL,
    departure_airport VARCHAR(100) NOT NULL,
    arrival_airport VARCHAR(100) NOT NULL,
    departure_date_time TIMESTAMP NOT NULL,
    arrival_date_time TIMESTAMP NOT NULL,
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    airline VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

#### Bus Expedition Entity
```sql
CREATE TABLE bus_expeditions (
    id BIGSERIAL PRIMARY KEY,
    expedition_number VARCHAR(20) UNIQUE NOT NULL,
    departure_terminal VARCHAR(100) NOT NULL,
    arrival_terminal VARCHAR(100) NOT NULL,
    departure_date_time TIMESTAMP NOT NULL,
    arrival_date_time TIMESTAMP NOT NULL,
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    bus_type VARCHAR(20) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

#### Authentication Tokens
```sql
CREATE TABLE registration_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id),
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id),
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🔐 Security Implementation

### JWT Authentication
The system implements a comprehensive JWT-based authentication system:

- **Token Generation**: Uses HMAC SHA-256 algorithm with configurable secret key
- **Token Expiration**: Configurable expiration time (default: 24 hours)
- **Token Whitelist**: Maintains active token whitelist for secure session management
- **Role-Based Access**: Two roles - `END_USER` and `SYSTEM_ADMIN`

### Security Features
- **Password Encryption**: BCrypt hashing for secure password storage
- **CORS Configuration**: Configurable CORS settings for frontend integration
- **Method-Level Security**: `@PreAuthorize` annotations for fine-grained access control
- **Token Blacklisting**: Secure logout with token invalidation

## 🌐 API Endpoints

### Authentication Endpoints (`/api/auth`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| POST | `/register` | User registration with email verification | Public |
| POST | `/login` | User authentication | Public |
| POST | `/logout` | User logout with token invalidation | Authenticated |
| GET | `/activate` | Email verification activation | Public |
| POST | `/forgot-password` | Password reset request | Public |
| POST | `/reset-password` | Password reset confirmation | Public |

### User Management (`/api/users`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| GET | `/profile` | Get current user profile | Authenticated |
| PUT | `/profile` | Update user profile | Authenticated |
| DELETE | `/profile` | Delete user account (soft delete) | Authenticated |
| GET | `/` | List all users (paginated) | Admin Only |
| DELETE | `/{id}` | Delete user by ID | Admin Only |

### Flight Management (`/api/flights`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| GET | `/` | Search flights with filters | Public |
| GET | `/{id}` | Get flight details | Public |
| POST | `/` | Create new flight | Admin Only |
| PUT | `/{id}` | Update flight | Admin Only |
| DELETE | `/{id}` | Delete flight | Admin Only |

### Bus Expedition Management (`/api/bus-expeditions`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| GET | `/` | Search bus expeditions with filters | Public |
| GET | `/{id}` | Get bus expedition details | Public |
| POST | `/` | Create new bus expedition | Admin Only |
| PUT | `/{id}` | Update bus expedition | Admin Only |
| DELETE | `/{id}` | Delete bus expedition | Admin Only |

## 🔍 Advanced Search & Filtering

The system implements dynamic search capabilities using **JPA Specifications**:

### Flight Search Parameters
- **Route**: Departure and arrival airports
- **Date Range**: Departure date filtering
- **Price Range**: Minimum and maximum price
- **Airline**: Specific airline filtering
- **Availability**: Available seats filtering

### Bus Expedition Search Parameters
- **Route**: Departure and arrival terminals
- **Date Range**: Departure date filtering
- **Price Range**: Minimum and maximum price
- **Bus Type**: Bus type filtering (STANDARD, VIP, SLEEPER)
- **Company**: Bus company filtering

### Pagination Support
- **Page Size**: Configurable results per page
- **Sorting**: Multiple field sorting support
- **Total Count**: Total results metadata

## 📧 Email Integration

### Email Templates
The system includes responsive HTML email templates:

- **`activate.html`**: Account activation email
- **`reset-password.html`**: Password reset email
- **`login.html`**: Login notification email

### Email Features
- **SMTP Configuration**: Gmail SMTP integration
- **Template Engine**: Thymeleaf for dynamic content
- **Async Processing**: Non-blocking email sending
- **Error Handling**: Comprehensive email delivery error handling

## 🛠️ Configuration

### Application Properties
Key configuration parameters:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/biletal
spring.datasource.username=${DB_USERNAME:biletal}
spring.datasource.password=${DB_PASSWORD:password}

# JWT Configuration
jwt.secret=${JWT_SECRET:your-secret-key}
jwt.expiration=${JWT_EXPIRATION:86400000}

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}

# Server Configuration
server.port=${PORT:8080}
app.base-url=${BASE_URL:http://localhost:8080}
```

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- PostgreSQL 12+
- Maven 3.6+

### Installation

1. **Clone the repository:**
```bash
git clone <repository-url>
cd biletalbackend
```

2. **Set up PostgreSQL database:**
```sql
CREATE DATABASE biletal;
CREATE USER biletal WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE biletal TO biletal;
```

3. **Configure environment variables:**
```bash
export DB_USERNAME=biletal
export DB_PASSWORD=password
export JWT_SECRET=your-super-secret-jwt-key
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=your-app-password
```

4. **Run the application:**
```bash
./mvnw spring-boot:run
```

5. **Access the API documentation:**
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - API Docs: `http://localhost:8080/v3/api-docs`

### Docker Setup (Optional)

```bash
# Start PostgreSQL with Docker Compose
docker-compose up -d

# Run the application
./mvnw spring-boot:run
```

## 🧪 Testing

### Run Tests
```bash
./mvnw test
```

### API Testing
Use the included Swagger UI for interactive API testing, or import the OpenAPI specification into your preferred API testing tool.

## 📚 API Documentation

The project includes comprehensive API documentation using **Swagger/OpenAPI 3**:

- **Interactive UI**: Full API exploration with request/response examples
- **Schema Documentation**: Complete DTO and model documentation
- **Authentication Testing**: Built-in JWT token testing support
- **Error Responses**: Detailed error response documentation

## 🏢 Business Logic

### User Management
- **Registration Flow**: Email verification with secure token system
- **Profile Management**: Complete user profile CRUD operations
- **Password Security**: Secure password reset with time-limited tokens

### Transportation Management
- **Multi-Modal Support**: Unified system for flights and bus expeditions
- **Seat Management**: Real-time seat availability tracking
- **Dynamic Pricing**: Flexible pricing structure support

### Administrative Features
- **User Administration**: Complete user management for system administrators
- **Transportation Scheduling**: Full CRUD operations for flights and bus expeditions
- **System Monitoring**: Comprehensive logging and monitoring capabilities

## 🔧 Technical Implementation Details

### Data Transfer Objects (DTOs)
The system uses comprehensive DTOs for API communication:

- **Request DTOs**: Input validation and data transformation
- **Response DTOs**: Consistent API response structure
- **Validation**: Bean validation with custom constraints

### Exception Handling
Global exception handling with:

- **Custom Exceptions**: Business logic specific exceptions
- **Global Handler**: Centralized exception handling
- **Error Responses**: Standardized error response format

### Soft Delete Pattern
Implements soft delete across all entities:

- **Logical Deletion**: `deleted_at` timestamp field
- **Query Filtering**: Automatic exclusion of deleted records
- **Data Recovery**: Ability to restore soft-deleted records

## 🔮 Future Enhancements

- **Ticket Booking**: Complete booking and payment integration
- **Seat Selection**: Interactive seat selection interface
- **Real-time Updates**: WebSocket integration for real-time updates
- **Mobile API**: Mobile-optimized API endpoints
- **Analytics**: Business intelligence and reporting features
- **Multi-language**: Internationalization support

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request