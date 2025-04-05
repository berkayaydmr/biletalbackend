This project is a simple implementation of an backend project is a part of a project that allows customers to buy a ticket for transportation. Its named Bilet Al. The project is implemented with Java Spring Boot and Postgresql. Up to planned sprints it will be developed. The sprint plans making by our Işık University Software Development Project course instructor. The first sprint is planned to be completed in 2-3 weeks. First sprint contains features like administrator login, logout, create account, end user login, delete account, edit admin account, edit user account with documanted details. 

# Bilet Al Backend Project Structure
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