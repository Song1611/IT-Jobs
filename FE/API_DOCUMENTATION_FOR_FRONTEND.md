# IT Job Recruitment System - API Documentation for Frontend

**Version**: 1.0.0  
**Base URL**: `http://localhost:8081`  
**Last Updated**: 2026-07-03

---

## Table of Contents

1. [Response Architecture](#response-architecture)
2. [Authentication](#authentication)
3. [Error Handling](#error-handling)
4. [API Endpoints](#api-endpoints)
   - [Authentication APIs](#authentication-apis)
   - [User APIs](#user-apis)
   - [Job APIs](#job-apis)
   - [Application APIs](#application-apis)
   - [Company APIs](#company-apis)
   - [HR APIs](#hr-apis)
   - [Admin APIs](#admin-apis)
5. [Data Models](#data-models)
6. [Pagination & Filtering](#pagination--filtering)

---

## Response Architecture

All API responses follow a standardized structure for consistency.

### Standard Response: `ApiResponse<T>`

```typescript
interface ApiResponse<T> {
  code: number;        // Status code (1000 = success, see error codes below)
  message?: string;    // Human-readable message (optional)
  result?: T;          // Response data (optional, type varies by endpoint)
}
```

### Paginated Response: `PageResponse<T>`

```typescript
interface PageResponse<T> {
  items: T[];          // Array of items
  page: number;        // Current page number (0-indexed)
  size: number;        // Page size
  totalElements: number; // Total number of items
  totalPages: number;  // Total number of pages
}
```


### Example Responses

#### Success Response (Single Object)
```json
{
  "code": 1000,
  "message": "User retrieved successfully",
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "John Doe",
    "email": "john@example.com"
  }
}
```

#### Success Response (Paginated)
```json
{
  "code": 1000,
  "message": "Jobs retrieved successfully",
  "result": {
    "items": [
      { "id": "...", "title": "Java Developer" },
      { "id": "...", "title": "React Developer" }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 45,
    "totalPages": 5
  }
}
```

#### Success Response (No Data)
```json
{
  "code": 1000,
  "message": "User deleted successfully"
}
```

---

## Authentication

The API uses JWT (JSON Web Token) for authentication.

### Authentication Flow

1. **Login** → Get `accessToken` and `refreshToken`
2. **Use accessToken** in `Authorization` header for API calls
3. **When accessToken expires** → Use `refreshToken` to get new tokens
4. **Logout** → Invalidate tokens

### Headers

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```


---

## Error Handling

### Error Codes

| Code | Meaning | HTTP Status |
|------|---------|-------------|
| 1000 | Success | 200 |
| 1001 | User not found | 404 |
| 1002 | User already exists | 400 |
| 1003 | Invalid credentials | 401 |
| 1004 | Unauthenticated | 401 |
| 1005 | Unauthorized | 403 |
| 1006 | Job not found | 404 |
| 1007 | Company not found | 404 |
| 1008 | Application not found | 404 |
| 1009 | Job not open | 400 |
| 1010 | Already applied | 400 |
| 1011 | Cannot withdraw application | 400 |
| 9999 | Uncategorized error | 500 |

### Error Response Format

```json
{
  "code": 1003,
  "message": "Invalid email or password"
}
```

### Frontend Error Handling Example (TypeScript)

```typescript
try {
  const response = await api.get('/api/users/my-info');
  if (response.data.code !== 1000) {
    // Handle error based on code
    handleError(response.data.code, response.data.message);
  }
  return response.data.result;
} catch (error) {
  // Handle network errors
  console.error('Network error:', error);
}
```


---

## API Endpoints

### Authentication APIs

#### 1. Login
```http
POST /api/auth/login
```

**Request Body**:
```typescript
interface LoginRequest {
  username: string;  // Email address
  password: string;
}
```

**Example**:
```json
{
  "username": "user@example.com",
  "password": "password123"
}
```

**Response**: `ApiResponse<AuthenticationResponse>`
```json
{
  "code": 1000,
  "message": "Login successful",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 3600
  }
}
```

#### 2. Refresh Token
```http
POST /api/auth/refresh
```

**Request Body**:
```typescript
interface RefreshRequest {
  refreshToken: string;
}
```

**Response**: `ApiResponse<AuthenticationResponse>`

#### 3. Logout
```http
POST /api/auth/logout
```

**Request Body**:
```typescript
interface LogoutRequest {
  token: string;  // Access token
}
```

**Response**: `ApiResponse<void>`
```json
{
  "code": 1000,
  "message": "Logout successful"
}
```


---

### User APIs

#### 1. Get My Info (Current User)
```http
GET /api/users/my-info
Authorization: Bearer <token>
```

**Auth Required**: Yes (Any authenticated user)

**Response**: `ApiResponse<UserResponse>`
```json
{
  "code": 1000,
  "result": {
    "id": "uuid",
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "0123456789",
    "gender": "MALE",
    "dateOfBirth": "1990-01-01",
    "avatar": "https://...",
    "coverImage": "https://...",
    "cvUrl": "https://...",
    "address": "123 Street, City",
    "roles": [
      { "name": "USER", "description": "Regular user" }
    ],
    "skills": [
      { "id": "uuid", "name": "Java", "description": "..." }
    ],
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

#### 2. Update My Profile
```http
PUT /api/users/my-profile
Authorization: Bearer <token>
Content-Type: application/json
```

**Auth Required**: Yes (Any authenticated user)

**Request Body**:
```typescript
interface UserUpdateRequest {
  fullName?: string;
  password?: string;     // Will be encrypted
  phone?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  dateOfBirth?: string;  // Format: YYYY-MM-DD
  avatar?: string;
  coverImage?: string;
  cvUrl?: string;
  address?: string;
  skillIds?: string[];   // Array of skill UUIDs
}
```

**Response**: `ApiResponse<UserResponse>`


#### 3. Get All Users (Admin Only)
```http
GET /api/users?filter=<filters>&page=0&size=10
Authorization: Bearer <adminToken>
```

**Auth Required**: Yes (ADMIN role)

**Query Parameters**:
- `filter` (optional): Array of filter strings (see [Filtering](#filtering))
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort field and direction (e.g., `createdAt,desc`)

**Example**: `GET /api/users?filter=email~gmail&page=0&size=10`

**Response**: `ApiResponse<PageResponse<UserResponse>>`

#### 4. Get User by ID (Admin Only)
```http
GET /api/users/{id}
Authorization: Bearer <adminToken>
```

**Auth Required**: Yes (ADMIN role)

**Response**: `ApiResponse<UserResponse>`

#### 5. Update User by ID
```http
PUT /api/users/{id}
Authorization: Bearer <token>
```

**Auth Required**: Yes (User can update self, ADMIN can update anyone)

**Security**: `@PostAuthorize` checks if user owns the profile or is ADMIN

**Request Body**: Same as `UserUpdateRequest` above

**Special Note**: 
- Regular users CANNOT update their `roles`
- Only ADMIN can update `roles` field

**Response**: `ApiResponse<UserResponse>`

#### 6. Delete User (Admin Only)
```http
DELETE /api/users/{id}
Authorization: Bearer <adminToken>
```

**Auth Required**: Yes (ADMIN role only)

**Response**: `ApiResponse<void>`


---

### Job APIs

#### 1. Get Featured Jobs (Public)
```http
GET /api/v1/jobs/featured?limit=10
```

**Auth Required**: No

**Query Parameters**:
- `limit` (optional): Number of jobs to return (default: 10)

**Response**: `ApiResponse<JobResponse[]>`

#### 2. Search Jobs (Recommended - Specification Pattern)
```http
GET /api/v1/jobs?filter=<filters>&page=0&size=10&sort=createdAt,desc
```

**Auth Required**: No

**Query Parameters**:
- `filter` (optional): Array of filter strings
- `page`, `size`, `sort`: Pagination parameters

**Filter Examples**:
```
# Search by title containing "developer"
GET /api/v1/jobs?filter=title~developer

# Multiple conditions (AND logic)
GET /api/v1/jobs?filter=title~developer&filter=workLocation~hanoi

# OR logic (use ' prefix)
GET /api/v1/jobs?filter='title~java&filter='title~python

# Salary range
GET /api/v1/jobs?filter=salaryMax>=1000

# Job type and level
GET /api/v1/jobs?filter=type:full-time&filter=level:senior

# Status filter (only open jobs)
GET /api/v1/jobs?filter=status:open
```

**Supported Operators**:
- `:` (equals) - `filter=type:full-time`
- `~` (contains) - `filter=title~developer`
- `!` (not equals) - `filter=status!closed`
- `>`, `>=`, `<`, `<=` (comparison) - `filter=salaryMax>=1000`
- `@` (in) - `filter=type@full-time,part-time`
- `#` (between) - `filter=salaryMax#1000,3000`

**Response**: `ApiResponse<PageResponse<JobResponse>>`


#### 3. Search Jobs (Legacy - Individual Parameters)
```http
GET /api/v1/jobs/search?keyword=developer&location=hanoi&salaryMin=1000&type=full-time&level=senior
```

**Auth Required**: No

**Query Parameters**:
- `keyword` (optional): Search in title and description
- `location` (optional): Search in workLocation
- `salaryMin` (optional): Minimum salary
- `type` (optional): Job type (full-time, part-time, contract, etc.)
- `level` (optional): Job level (junior, mid-level, senior, etc.)
- `page`, `size`, `sort`: Pagination parameters

**Response**: `ApiResponse<PageResponse<JobResponse>>`

**Note**: This endpoint is kept for backward compatibility. Use the Specification-based search (endpoint #2) for new implementations.

#### 4. Get Job by ID
```http
GET /api/v1/jobs/{id}
Authorization: Bearer <token> (optional)
```

**Auth Required**: No (but if authenticated, includes `isApplied` field)

**Response**: `ApiResponse<JobResponse>`
```json
{
  "code": 1000,
  "message": "Job retrieved successfully",
  "result": {
    "id": "uuid",
    "title": "Senior Java Developer",
    "slug": "senior-java-developer-abc123",
    "description": "We are looking for...",
    "requirements": "- 5+ years experience...",
    "benefits": "- Competitive salary...",
    "workLocation": "Hanoi, Vietnam",
    "salaryMin": 1000,
    "salaryMax": 2000,
    "salaryType": "USD",
    "type": "full-time",
    "level": "senior",
    "experience": "5+ years",
    "quantity": 2,
    "status": "open",
    "expiresAt": "2024-12-31T23:59:59",
    "viewCount": 150,
    "applicationCount": 25,
    "isApplied": false,  // Only present if user is authenticated
    "company": {
      "id": "uuid",
      "name": "Tech Company",
      "logo": "https://...",
      "slug": "tech-company"
    },
    "skills": [
      { "id": "uuid", "name": "Java" },
      { "id": "uuid", "name": "Spring Boot" }
    ],
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```


---

### Application APIs

#### 1. Apply for Job
```http
POST /api/v1/applications
Authorization: Bearer <token>
Content-Type: application/json
```

**Auth Required**: Yes (USER or ADMIN role)

**Request Body**:
```typescript
interface ApplicationRequest {
  jobId: string;           // UUID of the job
  coverLetter?: string;    // Optional cover letter
  cvUrl?: string;          // Optional CV URL (if different from profile)
}
```

**Example**:
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "coverLetter": "I am very interested in this position...",
  "cvUrl": "https://example.com/my-cv.pdf"
}
```

**Response**: `ApiResponse<ApplicationResponse>`
```json
{
  "code": 1000,
  "message": "Application submitted successfully",
  "result": {
    "id": "uuid",
    "job": {
      "id": "uuid",
      "title": "Java Developer",
      "company": {
        "id": "uuid",
        "name": "Tech Company",
        "logo": "https://..."
      }
    },
    "candidate": {
      "id": "uuid",
      "fullName": "John Doe",
      "email": "john@example.com",
      "avatar": "https://..."
    },
    "coverLetter": "I am very interested...",
    "cvUrl": "https://...",
    "status": "pending",
    "appliedAt": "2024-01-01T10:00:00"
  }
}
```


#### 2. Get My Applications
```http
GET /api/v1/applications/me?page=0&size=10
Authorization: Bearer <token>
```

**Auth Required**: Yes (USER or ADMIN role)

**Query Parameters**: Standard pagination parameters

**Response**: `ApiResponse<PageResponse<ApplicationResponse>>`

#### 3. Get Application by ID
```http
GET /api/v1/applications/{id}
Authorization: Bearer <token>
```

**Auth Required**: Yes (Must be the application owner)

**Response**: `ApiResponse<ApplicationResponse>`

#### 4. Withdraw Application
```http
DELETE /api/v1/applications/{id}
Authorization: Bearer <token>
```

**Auth Required**: Yes (Must be the application owner)

**Condition**: Can only withdraw applications with status = "pending"

**Response**: `ApiResponse<void>`
```json
{
  "code": 1000,
  "message": "Application withdrawn successfully"
}
```

---

### Company APIs

*(Coming from api_design_by_roles.md - To be implemented)*

#### 1. Get Top Companies (Public)
```http
GET /api/v1/companies/top?limit=10
```

**Auth Required**: No

**Response**: `ApiResponse<CompanyResponse[]>`

#### 2. Get All Active Companies (Public)
```http
GET /api/v1/companies?page=0&size=20
```

**Auth Required**: No

**Response**: `ApiResponse<PageResponse<CompanyResponse>>`


#### 3. Get Company by ID (Public)
```http
GET /api/v1/companies/{id}
```

**Auth Required**: No

**Response**: `ApiResponse<CompanyResponse>`

#### 4. Get Company by Slug (Public)
```http
GET /api/v1/companies/slug/{slug}
```

**Auth Required**: No

**Example**: `GET /api/v1/companies/slug/tech-company-abc123`

**Response**: `ApiResponse<CompanyResponse>`

---

### HR/Employer APIs

**Note**: All HR APIs require EMPLOYER or ADMIN role

#### 1. Get Company Jobs
```http
GET /api/v1/hr/companies/{companyId}/jobs?status=open&page=0&size=10
Authorization: Bearer <employerToken>
```

**Auth Required**: Yes (EMPLOYER or ADMIN)

**Query Parameters**:
- `status` (optional): Filter by job status (open, closed, pending, rejected)
- Standard pagination parameters

**Response**: `ApiResponse<PageResponse<JobResponse>>`

#### 2. Create Job
```http
POST /api/v1/hr/companies/{companyId}/jobs
Authorization: Bearer <employerToken>
Content-Type: application/json
```

**Auth Required**: Yes (EMPLOYER or ADMIN)

**Request Body**:
```typescript
interface JobRequest {
  title: string;
  description: string;
  requirements?: string;
  benefits?: string;
  workLocation: string;
  salaryMin?: number;
  salaryMax?: number;
  salaryType?: string;  // USD, VND, etc.
  type: string;         // full-time, part-time, contract, etc.
  level: string;        // junior, mid-level, senior, etc.
  experience?: string;
  quantity?: number;
  expiresAt?: string;   // ISO 8601 format
  skillIds?: string[];  // Array of skill UUIDs
}
```

**Response**: `ApiResponse<JobResponse>`


#### 3. Update Job
```http
PUT /api/v1/hr/companies/{companyId}/jobs/{jobId}
Authorization: Bearer <employerToken>
Content-Type: application/json
```

**Auth Required**: Yes (EMPLOYER or ADMIN, must own the company)

**Request Body**: Same as `JobRequest` above

**Response**: `ApiResponse<JobResponse>`

#### 4. Delete Job
```http
DELETE /api/v1/hr/companies/{companyId}/jobs/{jobId}
Authorization: Bearer <employerToken>
```

**Auth Required**: Yes (EMPLOYER or ADMIN, must own the company)

**Note**: This is a soft delete (sets status to "closed")

**Response**: `ApiResponse<void>`

#### 5. Get Job Applications
```http
GET /api/v1/hr/jobs/{jobId}/applications?status=pending&page=0&size=10
Authorization: Bearer <employerToken>
```

**Auth Required**: Yes (EMPLOYER or ADMIN, must own the job's company)

**Query Parameters**:
- `status` (optional): Filter by application status
- Standard pagination parameters

**Response**: `ApiResponse<PageResponse<ApplicationResponse>>`

#### 6. Get Company Applications
```http
GET /api/v1/hr/companies/{companyId}/applications?page=0&size=10
Authorization: Bearer <employerToken>
```

**Auth Required**: Yes (EMPLOYER or ADMIN, must own the company)

**Response**: `ApiResponse<PageResponse<ApplicationResponse>>`


#### 7. Update Application Status
```http
PUT /api/v1/hr/applications/{id}/status
Authorization: Bearer <employerToken>
Content-Type: application/json
```

**Auth Required**: Yes (EMPLOYER or ADMIN, must own the application's company)

**Request Body**:
```typescript
interface UpdateStatusRequest {
  status: string;  // pending, reviewing, interview, accepted, rejected, withdrawn
  notes?: string;  // HR notes (optional)
}
```

**Example**:
```json
{
  "status": "interview",
  "notes": "Scheduled for interview on 2024-02-01"
}
```

**Response**: `ApiResponse<ApplicationResponse>`

#### 8. Mark Application as Viewed
```http
PUT /api/v1/hr/applications/{id}/viewed
Authorization: Bearer <employerToken>
```

**Auth Required**: Yes (EMPLOYER or ADMIN, must own the application's company)

**Response**: `ApiResponse<void>`

#### 9. Get My Company
```http
GET /api/v1/hr/my-company
Authorization: Bearer <employerToken>
```

**Auth Required**: Yes (EMPLOYER or ADMIN)

**Response**: `ApiResponse<CompanyResponse>`

#### 10. Create Company
```http
POST /api/v1/hr/companies
Authorization: Bearer <employerToken>
Content-Type: application/json
```

**Auth Required**: Yes (EMPLOYER or ADMIN)

**Request Body**:
```typescript
interface CompanyRequest {
  name: string;
  description?: string;
  website?: string;
  logo?: string;
  coverImage?: string;
  address?: string;
  size?: string;      // 1-10, 11-50, 51-200, 201-500, 500+
  industry?: string;
}
```

**Response**: `ApiResponse<CompanyResponse>`


#### 11. Update Company
```http
PUT /api/v1/hr/companies/{id}
Authorization: Bearer <employerToken>
Content-Type: application/json
```

**Auth Required**: Yes (EMPLOYER or ADMIN, must own the company)

**Request Body**: Same as `CompanyRequest` above

**Response**: `ApiResponse<CompanyResponse>`

---

### Admin APIs

**Note**: All Admin APIs require ADMIN role

#### 1. Get All Jobs (Admin)
```http
GET /api/v1/admin/jobs?status=all&page=0&size=20
Authorization: Bearer <adminToken>
```

**Auth Required**: Yes (ADMIN only)

**Query Parameters**:
- `status` (optional): Filter by status
- Standard pagination parameters

**Response**: `ApiResponse<PageResponse<JobResponse>>`

#### 2. Approve Job
```http
POST /api/v1/admin/jobs/{id}/approve
Authorization: Bearer <adminToken>
```

**Auth Required**: Yes (ADMIN only)

**Response**: `ApiResponse<void>`
```json
{
  "code": 1000,
  "message": "Job approved successfully"
}
```

#### 3. Reject Job
```http
POST /api/v1/admin/jobs/{id}/reject
Authorization: Bearer <adminToken>
Content-Type: application/json
```

**Auth Required**: Yes (ADMIN only)

**Request Body**:
```typescript
interface RejectRequest {
  reason: string;  // Reason for rejection
}
```

**Response**: `ApiResponse<void>`


#### 4. Get All Companies (Admin)
```http
GET /api/v1/admin/companies?status=all&page=0&size=20
Authorization: Bearer <adminToken>
```

**Auth Required**: Yes (ADMIN only)

**Response**: `ApiResponse<PageResponse<CompanyResponse>>`

#### 5. Approve Company
```http
POST /api/v1/admin/companies/{id}/approve
Authorization: Bearer <adminToken>
```

**Auth Required**: Yes (ADMIN only)

**Response**: `ApiResponse<void>`

#### 6. Reject Company
```http
POST /api/v1/admin/companies/{id}/reject
Authorization: Bearer <adminToken>
Content-Type: application/json
```

**Auth Required**: Yes (ADMIN only)

**Request Body**:
```typescript
interface RejectRequest {
  reason: string;
}
```

**Response**: `ApiResponse<void>`

#### 7. Suspend Company
```http
POST /api/v1/admin/companies/{id}/suspend
Authorization: Bearer <adminToken>
Content-Type: application/json
```

**Auth Required**: Yes (ADMIN only)

**Request Body**:
```typescript
interface SuspendRequest {
  reason: string;
}
```

**Response**: `ApiResponse<void>`

#### 8. Get Dashboard Stats
```http
GET /api/v1/admin/dashboard/stats
Authorization: Bearer <adminToken>
```

**Auth Required**: Yes (ADMIN only)

**Response**: `ApiResponse<DashboardStatsResponse>`
```json
{
  "code": 1000,
  "result": {
    "totalUsers": 1250,
    "totalCompanies": 85,
    "totalJobs": 342,
    "totalApplications": 1876,
    "activeJobs": 215,
    "pendingApplications": 156
  }
}
```


---

## Data Models

### TypeScript Interfaces for Frontend

#### AuthenticationResponse
```typescript
interface AuthenticationResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;  // Seconds until token expires
}
```

#### UserResponse
```typescript
interface UserResponse {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  dateOfBirth?: string;  // ISO 8601 date
  avatar?: string;
  coverImage?: string;
  cvUrl?: string;
  address?: string;
  roles?: RoleResponse[];
  skills?: SkillResponse[];
  createdAt: string;     // ISO 8601 datetime
  updatedAt: string;     // ISO 8601 datetime
}
```

#### RoleResponse
```typescript
interface RoleResponse {
  name: string;          // USER, EMPLOYER, ADMIN
  description?: string;
}
```

#### SkillResponse
```typescript
interface SkillResponse {
  id: string;
  name: string;
  description?: string;
}
```

#### JobResponse
```typescript
interface JobResponse {
  id: string;
  title: string;
  slug: string;
  description: string;
  requirements?: string;
  benefits?: string;
  workLocation: string;
  salaryMin?: number;
  salaryMax?: number;
  salaryType?: string;
  type: string;          // full-time, part-time, contract, etc.
  level: string;         // junior, mid-level, senior, etc.
  experience?: string;
  quantity?: number;
  status: string;        // open, closed, pending, rejected
  expiresAt?: string;    // ISO 8601 datetime
  viewCount: number;
  applicationCount: number;
  isApplied?: boolean;   // Only present if user is authenticated
  company: CompanyBriefResponse;
  skills?: SkillResponse[];
  createdAt: string;
  updatedAt: string;
}
```


#### CompanyBriefResponse
```typescript
interface CompanyBriefResponse {
  id: string;
  name: string;
  logo?: string;
  slug: string;
}
```

#### CompanyResponse
```typescript
interface CompanyResponse {
  id: string;
  name: string;
  slug: string;
  description?: string;
  website?: string;
  logo?: string;
  coverImage?: string;
  address?: string;
  size?: string;
  industry?: string;
  status: string;        // pending, active, suspended, rejected
  verifiedAt?: string;
  viewCount: number;
  followerCount: number;
  createdAt: string;
  updatedAt: string;
}
```

#### ApplicationResponse
```typescript
interface ApplicationResponse {
  id: string;
  job: JobBriefResponse;
  candidate?: UserBriefResponse;
  coverLetter?: string;
  cvUrl?: string;
  status: string;        // pending, reviewing, interview, accepted, rejected, withdrawn
  hrNotes?: string;
  rejectionReason?: string;
  appliedAt: string;
  viewedAt?: string;
  reviewedAt?: string;
  interviewAt?: string;
  respondedAt?: string;
  viewedByEmployer: boolean;
}
```

#### JobBriefResponse
```typescript
interface JobBriefResponse {
  id: string;
  title: string;
  slug: string;
  workLocation: string;
  salaryMin?: number;
  salaryMax?: number;
  type: string;
  level: string;
  company: CompanyBriefResponse;
}
```

#### UserBriefResponse
```typescript
interface UserBriefResponse {
  id: string;
  fullName: string;
  email: string;
  avatar?: string;
  phone?: string;
}
```


#### DashboardStatsResponse
```typescript
interface DashboardStatsResponse {
  totalUsers: number;
  totalCompanies: number;
  totalJobs: number;
  totalApplications: number;
  activeJobs: number;
  pendingApplications: number;
}
```

---

## Pagination & Filtering

### Pagination Parameters

All paginated endpoints support these query parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | number | 0 | Page number (0-indexed) |
| `size` | number | 20 | Number of items per page |
| `sort` | string | - | Sort field and direction |

**Sort Examples**:
```
# Sort by creation date descending
?sort=createdAt,desc

# Sort by multiple fields
?sort=createdAt,desc&sort=title,asc

# Sort by name ascending
?sort=name,asc
```

### Filtering

The Specification-based search supports advanced filtering:

#### Filter Syntax
```
filter=<field><operator><value>
```

#### Operators

| Operator | Name | Example | Description |
|----------|------|---------|-------------|
| `:` | Equals | `filter=type:full-time` | Exact match |
| `~` | Contains | `filter=title~developer` | Case-insensitive contains |
| `!` | Not equals | `filter=status!closed` | Not equal to |
| `>` | Greater than | `filter=salaryMax>2000` | Greater than |
| `>=` | Greater or equal | `filter=salaryMax>=1000` | Greater than or equal |
| `<` | Less than | `filter=quantity<5` | Less than |
| `<=` | Less or equal | `filter=quantity<=10` | Less than or equal |
| `@` | In | `filter=type@full-time,part-time` | Value in list |
| `#` | Between | `filter=salaryMax#1000,3000` | Between two values |


#### AND Logic (Default)
Multiple filters with different prefixes are combined with AND:
```
GET /api/v1/jobs?filter=title~developer&filter=workLocation~hanoi
# Returns jobs where title contains "developer" AND workLocation contains "hanoi"
```

#### OR Logic
Prefix filter parameter with `'` (single quote) for OR logic:
```
GET /api/v1/jobs?filter='title~java&filter='title~python
# Returns jobs where title contains "java" OR title contains "python"
```

#### Complex Example
```
GET /api/v1/jobs?filter=status:open&filter='title~java&filter='title~python&filter=salaryMax>=1000
# Returns jobs where:
# - status = "open" AND
# - (title contains "java" OR title contains "python") AND
# - salaryMax >= 1000
```

### Frontend Implementation Example

#### React/TypeScript Example

```typescript
import axios, { AxiosInstance } from 'axios';

// API Client Setup
const apiClient: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle token refresh on 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const { data } = await axios.post(
            'http://localhost:8080/api/auth/refresh',
            { refreshToken }
          );
          localStorage.setItem('accessToken', data.result.accessToken);
          localStorage.setItem('refreshToken', data.result.refreshToken);
          
          // Retry original request
          error.config.headers.Authorization = `Bearer ${data.result.accessToken}`;
          return apiClient.request(error.config);
        } catch (refreshError) {
          // Redirect to login
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);
```


#### API Service Example

```typescript
// services/api.service.ts
interface ApiResponse<T> {
  code: number;
  message?: string;
  result?: T;
}

interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// Authentication
export const authAPI = {
  login: async (username: string, password: string) => {
    const { data } = await apiClient.post<ApiResponse<AuthenticationResponse>>(
      '/api/auth/login',
      { username, password }
    );
    return data.result;
  },

  logout: async (token: string) => {
    await apiClient.post('/api/auth/logout', { token });
  },
};

// User
export const userAPI = {
  getMyInfo: async () => {
    const { data } = await apiClient.get<ApiResponse<UserResponse>>(
      '/api/users/my-info'
    );
    return data.result;
  },

  updateMyProfile: async (request: UserUpdateRequest) => {
    const { data } = await apiClient.put<ApiResponse<UserResponse>>(
      '/api/users/my-profile',
      request
    );
    return data.result;
  },
};

// Job
export const jobAPI = {
  searchJobs: async (filters?: string[], page = 0, size = 10) => {
    const params = new URLSearchParams();
    if (filters) {
      filters.forEach(f => params.append('filter', f));
    }
    params.append('page', page.toString());
    params.append('size', size.toString());
    
    const { data } = await apiClient.get<ApiResponse<PageResponse<JobResponse>>>(
      `/api/v1/jobs?${params.toString()}`
    );
    return data.result;
  },

  getJobById: async (id: string) => {
    const { data } = await apiClient.get<ApiResponse<JobResponse>>(
      `/api/v1/jobs/${id}`
    );
    return data.result;
  },
};

// Application
export const applicationAPI = {
  applyForJob: async (request: ApplicationRequest) => {
    const { data } = await apiClient.post<ApiResponse<ApplicationResponse>>(
      '/api/v1/applications',
      request
    );
    return data.result;
  },

  getMyApplications: async (page = 0, size = 10) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<ApplicationResponse>>>(
      `/api/v1/applications/me?page=${page}&size=${size}`
    );
    return data.result;
  },
};
```


#### React Hook Example

```typescript
// hooks/useJobs.ts
import { useState, useEffect } from 'react';
import { jobAPI } from '../services/api.service';

export const useJobs = (filters?: string[], page = 0, size = 10) => {
  const [jobs, setJobs] = useState<PageResponse<JobResponse> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchJobs = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await jobAPI.searchJobs(filters, page, size);
        setJobs(data);
      } catch (err: any) {
        setError(err.message || 'Failed to fetch jobs');
      } finally {
        setLoading(false);
      }
    };

    fetchJobs();
  }, [filters, page, size]);

  return { jobs, loading, error };
};

// Usage in component
function JobList() {
  const { jobs, loading, error } = useJobs(
    ['status:open', 'title~developer'],
    0,
    10
  );

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      {jobs?.items.map(job => (
        <JobCard key={job.id} job={job} />
      ))}
      <Pagination
        currentPage={jobs?.page || 0}
        totalPages={jobs?.totalPages || 0}
      />
    </div>
  );
}
```

---

## Best Practices for Frontend

### 1. Token Management
```typescript
// Store tokens securely
localStorage.setItem('accessToken', token);
localStorage.setItem('refreshToken', refreshToken);

// Clear on logout
const logout = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  // Redirect to login
};
```

### 2. Error Handling
```typescript
const handleApiError = (code: number, message?: string) => {
  switch (code) {
    case 1001:
      toast.error('User not found');
      break;
    case 1003:
      toast.error('Invalid credentials');
      break;
    case 1004:
      // Redirect to login
      router.push('/login');
      break;
    case 1005:
      toast.error('You do not have permission');
      break;
    default:
      toast.error(message || 'An error occurred');
  }
};
```


### 3. Role-Based UI Rendering
```typescript
// Get user role from token or user info
const hasRole = (role: string) => {
  const userRoles = user?.roles?.map(r => r.name) || [];
  return userRoles.includes(role);
};

// Conditional rendering
{hasRole('ADMIN') && <AdminPanel />}
{hasRole('EMPLOYER') && <CompanyDashboard />}
{hasRole('USER') && <CandidateProfile />}
```

### 4. Pagination Component
```typescript
interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
}) => {
  return (
    <div className="pagination">
      <button
        disabled={currentPage === 0}
        onClick={() => onPageChange(currentPage - 1)}
      >
        Previous
      </button>
      
      <span>Page {currentPage + 1} of {totalPages}</span>
      
      <button
        disabled={currentPage >= totalPages - 1}
        onClick={() => onPageChange(currentPage + 1)}
      >
        Next
      </button>
    </div>
  );
};
```

### 5. Filter Builder
```typescript
// Build filter array for job search
const buildJobFilters = (params: {
  keyword?: string;
  location?: string;
  minSalary?: number;
  type?: string;
  level?: string;
}) => {
  const filters: string[] = ['status:open']; // Always filter open jobs
  
  if (params.keyword) {
    filters.push(`'title~${params.keyword}`);
    filters.push(`'description~${params.keyword}`);
  }
  
  if (params.location) {
    filters.push(`workLocation~${params.location}`);
  }
  
  if (params.minSalary) {
    filters.push(`salaryMax>=${params.minSalary}`);
  }
  
  if (params.type) {
    filters.push(`type:${params.type}`);
  }
  
  if (params.level) {
    filters.push(`level:${params.level}`);
  }
  
  return filters;
};

// Usage
const filters = buildJobFilters({
  keyword: 'developer',
  location: 'hanoi',
  minSalary: 1000,
  type: 'full-time',
});
// Result: ['status:open', "'title~developer", "'description~developer", 
//          'workLocation~hanoi', 'salaryMax>=1000', 'type:full-time']
```


---

## Common Scenarios

### Scenario 1: User Login and Profile Update

```typescript
// 1. Login
const loginResponse = await authAPI.login('user@example.com', 'password123');
localStorage.setItem('accessToken', loginResponse.accessToken);
localStorage.setItem('refreshToken', loginResponse.refreshToken);

// 2. Get user info
const user = await userAPI.getMyInfo();
console.log(user.fullName, user.email);

// 3. Update profile
const updatedUser = await userAPI.updateMyProfile({
  fullName: 'John Updated',
  phone: '0987654321',
  avatar: 'https://example.com/new-avatar.jpg',
});
```

### Scenario 2: Job Search and Application

```typescript
// 1. Search for jobs
const filters = ['status:open', 'title~developer', 'workLocation~hanoi'];
const jobs = await jobAPI.searchJobs(filters, 0, 10);

// 2. View job details
const job = await jobAPI.getJobById(jobs.items[0].id);
console.log('Is Applied:', job.isApplied);

// 3. Apply for job
if (!job.isApplied) {
  const application = await applicationAPI.applyForJob({
    jobId: job.id,
    coverLetter: 'I am very interested in this position...',
  });
  console.log('Application submitted:', application.id);
}

// 4. View my applications
const myApplications = await applicationAPI.getMyApplications(0, 10);
console.log('Total applications:', myApplications.totalElements);
```

### Scenario 3: HR Managing Applications

```typescript
// 1. Get company jobs
const companyJobs = await hrAPI.getCompanyJobs(companyId, 'open', 0, 10);

// 2. Get applications for a job
const applications = await hrAPI.getJobApplications(jobId, null, 0, 20);

// 3. Review application
const application = applications.items[0];

// 4. Update application status
await hrAPI.updateApplicationStatus(application.id, {
  status: 'interview',
  notes: 'Scheduled for interview on 2024-02-01 at 10:00 AM',
});

// 5. Mark as viewed
await hrAPI.markApplicationAsViewed(application.id);
```

### Scenario 4: Admin Approving Content

```typescript
// 1. Get pending jobs
const pendingJobs = await adminAPI.getAllJobs('pending', 0, 10);

// 2. Approve or reject
for (const job of pendingJobs.items) {
  if (/* approval criteria */) {
    await adminAPI.approveJob(job.id);
  } else {
    await adminAPI.rejectJob(job.id, {
      reason: 'Job description does not meet quality standards',
    });
  }
}

// 3. Get dashboard stats
const stats = await adminAPI.getDashboardStats();
console.log('Active jobs:', stats.activeJobs);
console.log('Pending applications:', stats.pendingApplications);
```


---

## Testing with Postman/Insomnia

### Environment Variables

```json
{
  "baseUrl": "http://localhost:8080",
  "accessToken": "{{accessToken}}",
  "refreshToken": "{{refreshToken}}"
}
```

### Example Request Collection

#### 1. Login
```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "username": "admin@example.com",
  "password": "admin123"
}
```

**Post-response Script** (Save tokens):
```javascript
const response = pm.response.json();
if (response.code === 1000) {
  pm.environment.set('accessToken', response.result.accessToken);
  pm.environment.set('refreshToken', response.result.refreshToken);
}
```

#### 2. Get My Info
```http
GET {{baseUrl}}/api/users/my-info
Authorization: Bearer {{accessToken}}
```

#### 3. Search Jobs
```http
GET {{baseUrl}}/api/v1/jobs?filter=status:open&filter=title~developer&page=0&size=10
```

#### 4. Apply for Job
```http
POST {{baseUrl}}/api/v1/applications
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "coverLetter": "I am very interested..."
}
```

---

## Security Considerations

### 1. Token Storage
- **DO**: Store tokens in `httpOnly` cookies (if possible) or `localStorage`
- **DON'T**: Store tokens in plain cookies or sessionStorage for sensitive apps
- **IMPORTANT**: Always use HTTPS in production

### 2. Authorization Checks
Frontend should hide/show UI based on roles, but **NEVER** rely solely on frontend for security.
Backend always validates permissions via `@PreAuthorize` and `@PostAuthorize`.

### 3. Sensitive Data
- Never log tokens or passwords
- Clear tokens on logout
- Implement token refresh before expiration

### 4. CORS Configuration
Backend should be configured to allow requests only from trusted origins in production.


---

## Changelog

### Version 1.0.0 (2026-07-03)

#### Added
- Complete API documentation for Frontend team
- Response architecture with `ApiResponse<T>` and `PageResponse<T>`
- Authentication endpoints (login, refresh, logout)
- User management endpoints with RBAC
- Job search with Specification pattern (filter array)
- Legacy job search (backward compatibility)
- Application management (apply, view, withdraw)
- HR/Employer endpoints (manage jobs, applications, company)
- Admin endpoints (approve/reject content, dashboard stats)
- Complete TypeScript interfaces for all data models
- Pagination and filtering documentation
- Frontend implementation examples (React/TypeScript)
- Common usage scenarios
- Best practices and security considerations

#### Features
- JWT-based authentication
- Role-based access control (USER, EMPLOYER, ADMIN)
- Advanced filtering with Specification pattern
- Pagination support on all list endpoints
- `@PostAuthorize` for owner-based security
- Soft delete for jobs (status = closed)

---

## Support & Contact

### Backend Developer
- **Email**: backend@itjob.com
- **API Base URL**: http://localhost:8080
- **Documentation**: This file

### Resources
- **Error Codes**: See [Error Handling](#error-handling) section
- **Filter Examples**: See `FILTER_EXAMPLES.md`
- **Specification Pattern**: See `SPECIFICATION_REFACTOR_SUMMARY.md`
- **RBAC Implementation**: See `RBAC_IMPLEMENTATION_SUMMARY.md`

---

## Quick Reference Card

### Authentication
```
POST   /api/auth/login         → Login
POST   /api/auth/refresh       → Refresh token
POST   /api/auth/logout        → Logout
```

### User
```
GET    /api/users/my-info      → Get current user
PUT    /api/users/my-profile   → Update current user
GET    /api/users              → Get all users (ADMIN)
GET    /api/users/{id}         → Get user by ID (ADMIN)
PUT    /api/users/{id}         → Update user (Self or ADMIN)
DELETE /api/users/{id}         → Delete user (ADMIN)
```

### Job
```
GET    /api/v1/jobs/featured   → Featured jobs
GET    /api/v1/jobs            → Search jobs (Specification)
GET    /api/v1/jobs/search     → Search jobs (Legacy)
GET    /api/v1/jobs/{id}       → Get job by ID
```

### Application
```
POST   /api/v1/applications        → Apply for job
GET    /api/v1/applications/me     → My applications
GET    /api/v1/applications/{id}   → Get application
DELETE /api/v1/applications/{id}   → Withdraw application
```

### HR (EMPLOYER, ADMIN)
```
GET    /api/v1/hr/companies/{companyId}/jobs              → Company jobs
POST   /api/v1/hr/companies/{companyId}/jobs              → Create job
PUT    /api/v1/hr/companies/{companyId}/jobs/{jobId}      → Update job
DELETE /api/v1/hr/companies/{companyId}/jobs/{jobId}      → Delete job
GET    /api/v1/hr/jobs/{jobId}/applications               → Job applications
PUT    /api/v1/hr/applications/{id}/status                → Update status
```

### Admin (ADMIN only)
```
GET    /api/v1/admin/jobs                → All jobs
POST   /api/v1/admin/jobs/{id}/approve   → Approve job
POST   /api/v1/admin/jobs/{id}/reject    → Reject job
GET    /api/v1/admin/dashboard/stats     → Dashboard stats
```

---

**End of Documentation**

For questions or issues, please contact the backend development team.
