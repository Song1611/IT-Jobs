 📚 ITJOB-BE API Documentation

## 📖 Mục lục
- [Giới thiệu](#giới-thiệu)
- [Base URL](#base-url)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
  - [Auth APIs](#1-auth-apis)
  - [User APIs](#2-user-apis)
  - [Company APIs](#3-company-apis)
  - [Job APIs](#4-job-apis)
  - [Application APIs](#5-application-apis)
  - [Skill APIs](#6-skill-apis)
  - [Post APIs](#7-post-apis)
  - [Interaction APIs](#8-interaction-apis)
  - [Follow APIs](#9-follow-apis)
  - [Review APIs](#10-review-apis)
  - [Blog APIs](#11-blog-apis)
  - [Blog Category APIs](#12-blog-category-apis)   
  - [Search APIs](#13-search-apis)
  - [Location APIs](#14-location-apis)
- [Data Models](#data-models)
- [Error Handling](#error-handling)

---

## Giới thiệu

ITJOB-BE là REST API backend cho nền tảng tuyển dụng việc làm IT, được xây dựng bằng ASP.NET Core 8.0.

**Công nghệ:**
- ASP.NET Core 8.0 Web API
- Entity Framework Core 8.0
- SQL Server
- JWT Authentication
- Cloudinary (File Upload)
- BCrypt (Password Hashing)

**Tính năng chính:**
- Quản lý người dùng (Ứng viên, Nhà tuyển dụng, Admin)
- Quản lý công ty và việc làm
- Ứng tuyển công việc
- Mạng xã hội (Posts, Likes, Comments)
- Blog và Review công ty
- Tìm kiếm và lọc công việc

---

## Base URL

```
http://localhost:5000
```

---

## Authentication

### JWT Bearer Token

Hầu hết các endpoints yêu cầu authentication thông qua JWT token.

**Header:**
```
Authorization: Bearer <access_token>
```

**Token Types:**
- **Access Token**: JWT token, expire sau 60 phút
- **Refresh Token**: Lưu trong HttpOnly Cookie, expire sau 7 ngày

### Roles

Hệ thống có 3 loại role:
- `user` - Ứng viên (tìm việc, ứng tuyển)
- `employer` - Nhà tuyển dụng (đăng tin, quản lý công ty)
- `admin` - Quản trị viên

**Role Authorization:**
Một số endpoints yêu cầu role cụ thể thông qua query parameter:
```
?role=employer
```

---


## API Endpoints

---

## 1. Auth APIs

### 1.1 Đăng ký tài khoản ứng viên

Tạo tài khoản mới cho ứng viên (role = `user`).

**Endpoint:** `POST /api/Auth/register-user`

**Request Body:**
```json
{
  "fullName": "Nguyễn Văn A",
  "email": "user@example.com",
  "password": "Password123!",
  "phone": "0901234567",
  "gender": "male",
  "dateOfBirth": "1995-01-15"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Đăng ký tài khoản ứng viên thành công",
  "data": {
    "user": {
      "id": 1,
      "fullName": "Nguyễn Văn A",
      "email": "user@example.com",
      "phone": "0901234567",
      "gender": "male",
      "dateOfBirth": "1995-01-15",
      "role": "user",
      "avatar": null,
      "coverImage": null
    }
  }
}
```

---


Tạo tài khoản HR + Công ty + Liên kết CompanyMember.

**Endpoint:** `POST /api/Auth/register-hr`

**Request Body:**
```json
{
  "fullName": "Trần Thị B",
  "email": "hr@company.com",
  "password": "Password123!",
  "phone": "0907654321",
  "gender": "female",
  "dateOfBirth": "1990-05-20",
  "avatar": "https://example.com/avatar.jpg",
  "coverImage": "https://example.com/cover.jpg",
  "companyName": "Tech Company Ltd",
  "companyAvatar": "https://example.com/company-logo.png",
  "companyCoverImage": "https://example.com/company-cover.jpg",
  "companyNationality": "Việt Nam",
  "companyWebsite": "https://techcompany.com",
  "companyDescription": "Công ty công nghệ hàng đầu",
  "companyFoundedYear": 2015,
  "companyAddress": "123 Đường ABC",
  "wardId": 1
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Đăng ký tài khoản nhà tuyển dụng thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "base64_encoded_refresh_token",
    "user": {
      "id": 2,
      "fullName": "Trần Thị B",
      "email": "hr@company.com",
      "role": "employer"
    },
    "company": {
      "id": 1,
      "name": "Tech Company Ltd",
      "avatar": "https://example.com/company-logo.png",
      "website": "https://techcompany.com"
    }
  }
}
```

---

### 1.3 Đăng nhập

**Endpoint:** `POST /api/Auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "base64_encoded_refresh_token",
    "user": {
      "id": 1,
      "fullName": "Nguyễn Văn A",
      "email": "user@example.com",
      "role": "user",
      "avatar": "https://example.com/avatar.jpg"
    }
  }
}
```

**Note:** Refresh token cũng được lưu trong HttpOnly Cookie.

---

### 1.4 Làm mới token

**Endpoint:** `POST /api/Auth/refresh-token`

**Headers:**
```
Cookie: refreshToken=<refresh_token>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Làm mới token thành công",
  "data": {
    "accessToken": "new_access_token",
    "user": {
      "id": 1,
      "fullName": "Nguyễn Văn A",
      "email": "user@example.com"
    }
  }
}
```

---

### 1.5 Đăng xuất

**Endpoint:** `POST /api/Auth/logout`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Đăng xuất thành công"
}
```

---


## 2. User APIs

### 2.1 Lấy thông tin user

**Endpoint:** `GET /api/User/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "fullName": "Nguyễn Văn A",
  "email": "user@example.com",
  "phone": "0901234567",
  "gender": "male",
  "dateOfBirth": "1995-01-15",
  "avatar": "https://example.com/avatar.jpg",
  "coverImage": "https://example.com/cover.jpg",
  "role": "user"
}
```

---

### 2.2 Cập nhật thông tin user

**Endpoint:** `PUT /api/User/{id}`

**Request Body:**
```json
{
  "fullName": "Nguyễn Văn A Updated",
  "phone": "0909999999",
  "gender": "male",
  "dateOfBirth": "1995-01-15",
  "avatar": "https://example.com/new-avatar.jpg",
  "coverImage": "https://example.com/new-cover.jpg"
}
```

**Response:** `200 OK`
```json
{
  "message": "Profile updated successfully",
  "data": {
    "id": 1,
    "fullName": "Nguyễn Văn A Updated",
    "email": "user@example.com"
  }
}
```

---

### 2.3 Đổi mật khẩu

**Endpoint:** `POST /api/User/{id}/change-password`

**Request Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword123!"
}
```

**Response:** `200 OK`
```json
{
  "message": "Password changed successfully"
}
```

---

### 2.4 Lấy danh sách đơn ứng tuyển của user

**Endpoint:** `GET /api/User/{id}/applications`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 25,
  "totalPages": 3,
  "data": [
    {
      "jobId": 1,
      "userId": 1,
      "cvUrl": "https://drive.google.com/cv.pdf",
      "coverLetter": "I am interested in this position...",
      "status": "pending",
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    }
  ]
}
```

---

### 2.5 Lấy danh sách kỹ năng của user

**Endpoint:** `GET /api/User/{id}/skills`

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": 1,
      "name": "ReactJS"
    },
    {
      "id": 2,
      "name": "Node.js"
    }
  ]
}
```

---

### 2.6 Thêm kỹ năng cho user

**Endpoint:** `POST /api/User/{id}/skills`

**Request Body:**
```json
{
  "skillId": 3
}
```

**Response:** `200 OK`
```json
{
  "message": "Skill added to user",
  "userId": 1,
  "skillId": 3
}
```

---

### 2.7 Xóa kỹ năng của user

**Endpoint:** `DELETE /api/User/{id}/skills/{skillId}`

**Response:** `204 No Content`

---


## 3. Company APIs

### 3.1 Lấy danh sách công ty

**Endpoint:** `GET /api/Companies`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 50,
  "totalPages": 5,
  "data": [
    {
      "id": 1,
      "name": "FPT Software",
      "avatar": "https://example.com/fpt-logo.png",
      "coverImage": "https://example.com/fpt-cover.jpg",
      "nationality": "Việt Nam",
      "website": "https://fptsoftware.com",
      "description": "Công ty phần mềm hàng đầu Việt Nam",
      "foundedYear": 1999,
      "address": "Tòa nhà FPT, Quận 9",
      "wardId": 1,
      "createdAt": "2020-01-15T09:00:00"
    }
  ]
}
```

---

### 3.2 Lấy danh sách logo công ty

**Endpoint:** `GET /api/Companies/logos`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 50,
  "totalPages": 5,
  "data": [
    {
      "id": 1,
      "name": "FPT Software",
      "avatar": "https://example.com/fpt-logo.png"
    }
  ]
}
```

---

### 3.3 Lấy thông tin chi tiết công ty

**Endpoint:** `GET /api/Companies/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "FPT Software",
  "avatar": "https://example.com/fpt-logo.png",
  "coverImage": "https://example.com/fpt-cover.jpg",
  "nationality": "Việt Nam",
  "website": "https://fptsoftware.com",
  "description": "Công ty phần mềm hàng đầu Việt Nam",
  "foundedYear": 1999,
  "address": "Tòa nhà FPT, Quận 9",
  "wardId": 1,
  "createdByUserId": 1,
  "createdAt": "2020-01-15T09:00:00",
  "updatedAt": "2025-01-15T10:00:00"
}
```

---

### 3.4 Tạo công ty mới

**Endpoint:** `POST /api/Companies?role=employer`

**Authorization:** Required (employer role)

**Request Body:**
```json
{
  "name": "New Tech Company",
  "avatar": "https://example.com/logo.png",
  "coverImage": "https://example.com/cover.jpg",
  "nationality": "Việt Nam",
  "website": "https://newtech.com",
  "description": "Công ty công nghệ mới",
  "foundedYear": 2024
}
```

**Response:** `201 Created`
```json
{
  "id": 11,
  "name": "New Tech Company",
  "avatar": "https://example.com/logo.png",
  "createdAt": "2025-01-15T10:30:00"
}
```

---

### 3.5 Cập nhật thông tin công ty

**Endpoint:** `PUT /api/Companies/{id}?role=employer`

**Authorization:** Required (employer role)

**Request Body:**
```json
{
  "name": "Updated Company Name",
  "avatar": "https://example.com/new-logo.png",
  "coverImage": "https://example.com/new-cover.jpg",
  "nationality": "Việt Nam",
  "website": "https://updated-website.com",
  "description": "Updated description",
  "foundedYear": 2024
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Updated Company Name",
  "updatedAt": "2025-01-15T11:00:00"
}
```

---

### 3.6 Xóa công ty

**Endpoint:** `DELETE /api/Companies/{id}?role=employer`

**Authorization:** Required (employer or admin role)

**Response:** `204 No Content`

---

### 3.7 Lấy công ty của HR đang đăng nhập

**Endpoint:** `GET /api/Companies/my-company?role=employer`

**Authorization:** Required (employer role)

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "FPT Software",
  "avatar": "https://example.com/fpt-logo.png",
  "coverImage": "https://example.com/fpt-cover.jpg",
  "nationality": "Việt Nam",
  "website": "https://fptsoftware.com",
  "description": "Công ty phần mềm hàng đầu Việt Nam",
  "foundedYear": 1999
}
```

---

### 3.8 Upload avatar công ty

**Endpoint:** `POST /api/Companies/upload-avatar?role=employer`

**Authorization:** Required (employer role)

**Content-Type:** `multipart/form-data`

**Request Body:**
```
file: <image_file>
```

**Response:** `200 OK`
```json
{
  "avatarUrl": "https://cloudinary.com/uploaded-avatar.jpg",
  "message": "Upload ảnh đại diện thành công"
}
```

---

### 3.9 Upload cover image công ty

**Endpoint:** `POST /api/Companies/upload-cover?role=employer`

**Authorization:** Required (employer role)

**Content-Type:** `multipart/form-data`

**Request Body:**
```
file: <image_file>
```

**Response:** `200 OK`
```json
{
  "coverImageUrl": "https://cloudinary.com/uploaded-cover.jpg",
  "message": "Upload ảnh bìa thành công"
}
```

---

### 3.10 Cập nhật thông tin công ty của HR

**Endpoint:** `PUT /api/Companies/my-company?role=employer`

**Authorization:** Required (employer role)

**Request Body:**
```json
{
  "name": "Updated Company Name",
  "nationality": "Việt Nam",
  "website": "https://updated-website.com",
  "description": "Updated description",
  "foundedYear": 2024,
  "address": "123 New Address",
  "wardId": 2
}
```

**Response:** `200 OK`
```json
{
  "data": {
    "id": 1,
    "name": "Updated Company Name",
    "updatedAt": "2025-01-15T12:00:00"
  },
  "message": "Cập nhật thông tin công ty thành công"
}
```

---


## 4. Job APIs

### 4.1 Lấy danh sách công việc

**Endpoint:** `GET /api/Job`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 100,
  "totalPages": 10,
  "data": [
    {
      "id": 1,
      "companyId": 1,
      "title": "Senior Frontend Developer (ReactJS)",
      "description": "Phát triển ứng dụng web với ReactJS...",
      "type": "full-time",
      "quantity": 2,
      "deadline": "2025-12-31",
      "status": "open",
      "createdAt": "2025-01-10T09:00:00",
      "updatedAt": "2025-01-10T09:00:00"
    }
  ]
}
```

---

### 4.2 Lấy thông tin chi tiết công việc

**Endpoint:** `GET /api/Job/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "companyId": 1,
  "title": "Senior Frontend Developer (ReactJS)",
  "description": "Phát triển ứng dụng web với ReactJS, NextJS...",
  "type": "full-time",
  "quantity": 2,
  "deadline": "2025-12-31",
  "status": "open",
  "createdAt": "2025-01-10T09:00:00",
  "updatedAt": "2025-01-10T09:00:00",
  "company": {
    "id": 1,
    "name": "FPT Software",
    "avatar": "https://example.com/fpt-logo.png"
  },
  "skills": [
    {
      "id": 1,
      "name": "ReactJS"
    },
    {
      "id": 2,
      "name": "TypeScript"
    }
  ]
}
```

---

### 4.3 Tạo công việc mới

**Endpoint:** `POST /api/Job/{userId}?role=employer`

**Authorization:** Required (employer role)

**Request Body:**
```json
{
  "companyId": 1,
  "title": "Backend Developer (Node.js)",
  "description": "Xây dựng API RESTful với Node.js và Express",
  "type": "full-time",
  "quantity": 3,
  "deadline": "2025-11-30",
  "status": "open"
}
```

**Response:** `201 Created`
```json
{
  "id": 11,
  "companyId": 1,
  "title": "Backend Developer (Node.js)",
  "description": "Xây dựng API RESTful với Node.js và Express",
  "type": "full-time",
  "quantity": 3,
  "deadline": "2025-11-30",
  "status": "open",
  "createdAt": "2025-01-15T10:30:00"
}
```

---

### 4.4 Cập nhật công việc

**Endpoint:** `PUT /api/Job/{id}?role=employer`

**Authorization:** Required (employer role)

**Request Body:**
```json
{
  "companyId": 1,
  "title": "Senior Backend Developer (Node.js)",
  "description": "Updated description",
  "type": "full-time",
  "quantity": 2,
  "deadline": "2025-12-31",
  "status": "open"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "title": "Senior Backend Developer (Node.js)",
  "updatedAt": "2025-01-15T11:00:00"
}
```

---

### 4.5 Xóa công việc

**Endpoint:** `DELETE /api/Job/{id}?role=employer`

**Authorization:** Required (employer or admin role)

**Response:** `204 No Content`

---

### 4.6 Lấy công việc đăng hôm nay

**Endpoint:** `GET /api/Job/today`

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": 15,
      "title": "DevOps Engineer",
      "companyId": 3,
      "createdAt": "2025-01-15T08:00:00"
    }
  ]
}
```

---

### 4.7 Lấy công việc theo kỹ năng

**Endpoint:** `GET /api/Job/by-skill`

**Query Parameters:**
- `skillId` (int, required)
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Example:** `GET /api/Job/by-skill?skillId=1&PageNumber=1&PageSize=10`

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 15,
  "totalPages": 2,
  "data": [
    {
      "id": 1,
      "title": "Senior Frontend Developer (ReactJS)",
      "companyId": 1
    }
  ]
}
```

---

### 4.8 Lấy công việc theo công ty

**Endpoint:** `GET /api/Job/by-company`

**Query Parameters:**
- `companyId` (int, required)
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Example:** `GET /api/Job/by-company?companyId=1&PageNumber=1&PageSize=10`

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 8,
  "totalPages": 1,
  "data": [
    {
      "id": 1,
      "title": "Senior Frontend Developer (ReactJS)",
      "companyId": 1,
      "status": "open"
    }
  ]
}
```

---

### 4.9 Lấy công việc của HR

**Endpoint:** `GET /api/Job/by-user/{userId}?role=employer`

**Authorization:** Required (employer role)

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 5,
  "totalPages": 1,
  "data": [
    {
      "id": 1,
      "title": "Senior Frontend Developer (ReactJS)",
      "companyId": 1,
      "status": "open",
      "createdAt": "2025-01-10T09:00:00"
    }
  ]
}
```

---


## 5. Application APIs

### 5.1 Lấy danh sách đơn ứng tuyển

**Endpoint:** `GET /api/Application`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 50,
  "totalPages": 5,
  "data": [
    {
      "jobId": 1,
      "userId": 5,
      "cvUrl": "https://drive.google.com/cv/user5_cv.pdf",
      "coverLetter": "I am very interested in this position...",
      "status": "pending",
      "createdAt": "2025-01-12T14:30:00",
      "updatedAt": "2025-01-12T14:30:00",
      "job": {
        "id": 1,
        "title": "Senior Frontend Developer (ReactJS)",
        "companyId": 1
      },
      "user": {
        "id": 5,
        "fullName": "Nguyễn Văn E",
        "email": "user5@example.com"
      }
    }
  ]
}
```

---


## 6. Skill APIs

### 6.1 Lấy danh sách kỹ năng

**Endpoint:** `GET /api/skills`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 50,
  "totalPages": 5,
  "data": [
    {
      "id": 1,
      "name": "ReactJS",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    },
    {
      "id": 2,
      "name": "Node.js",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### 6.2 Tạo kỹ năng mới

**Endpoint:** `POST /api/skills`

**Request Body:**
```json
{
  "name": "Vue.js"
}
```

**Response:** `201 Created`
```json
{
  "id": 11,
  "name": "Vue.js",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

---

### 6.3 Cập nhật kỹ năng

**Endpoint:** `PUT /api/skills/{id}`

**Request Body:**
```json
{
  "name": "Vue.js 3"
}
```

**Response:** `200 OK`
```json
{
  "id": 11,
  "name": "Vue.js 3",
  "updatedAt": "2025-01-15T11:00:00"
}
```

---

### 6.4 Xóa kỹ năng

**Endpoint:** `DELETE /api/skills/{id}`

**Response:** `204 No Content`

---


## 7. Post APIs

### 7.1 Lấy danh sách bài đăng

**Endpoint:** `GET /api/Post`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 100,
  "totalPages": 10,
  "data": [
    {
      "id": 1,
      "content": "Chúng tôi đang tuyển dụng Senior Frontend Developer!",
      "userId": 3,
      "companyId": 1,
      "createdAt": "2025-01-14T10:00:00",
      "updatedAt": "2025-01-14T10:00:00",
      "user": {
        "id": 3,
        "fullName": "Nguyễn Văn C",
        "avatar": "https://example.com/avatar3.jpg"
      },
      "company": {
        "id": 1,
        "name": "FPT Software",
        "avatar": "https://example.com/fpt-logo.png"
      },
      "attachments": [
        {
          "id": 1,
          "fileType": "image",
          "fileUrl": "https://example.com/post-image.jpg"
        }
      ],
      "totalLikes": 15,
      "totalComments": 8
    }
  ]
}
```

---

### 7.2 Lấy chi tiết bài đăng

**Endpoint:** `GET /api/Post/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "content": "Chúng tôi đang tuyển dụng Senior Frontend Developer!",
  "userId": 3,
  "companyId": 1,
  "createdAt": "2025-01-14T10:00:00",
  "updatedAt": "2025-01-14T10:00:00",
  "user": {
    "id": 3,
    "fullName": "Nguyễn Văn C",
    "avatar": "https://example.com/avatar3.jpg"
  },
  "company": {
    "id": 1,
    "name": "FPT Software",
    "avatar": "https://example.com/fpt-logo.png",
    "address": "Tòa nhà FPT, Quận 9"
  },
  "attachments": [
    {
      "id": 1,
      "fileType": "image",
      "fileUrl": "https://example.com/post-image.jpg"
    }
  ],
  "interactions": [
    {
      "id": 1,
      "userId": 5,
      "isLiked": true,
      "content": null,
      "createdAt": "2025-01-14T10:15:00"
    },
    {
      "id": 2,
      "userId": 6,
      "isLiked": false,
      "content": "Mức lương bao nhiêu vậy ạ?",
      "createdAt": "2025-01-14T10:20:00"
    }
  ]
}
```

---

### 7.3 Tạo bài đăng mới

**Endpoint:** `POST /api/Post`

**Request Body:**
```json
{
  "content": "Thông báo tuyển dụng vị trí Backend Developer",
  "userId": 3,
  "companyId": 1
}
```

**Note:** `userId` hoặc `companyId` có thể null (bài đăng cá nhân hoặc bài đăng công ty).

**Response:** `201 Created`
```json
{
  "id": 11,
  "content": "Thông báo tuyển dụng vị trí Backend Developer",
  "userId": 3,
  "companyId": 1,
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

---

### 7.4 Cập nhật bài đăng

**Endpoint:** `PUT /api/Post/{id}`

**Request Body:**
```json
{
  "content": "Updated content for the post",
  "userId": 3,
  "companyId": 1
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "content": "Updated content for the post",
  "updatedAt": "2025-01-15T11:00:00"
}
```

---

### 7.5 Xóa bài đăng

**Endpoint:** `DELETE /api/Post/{id}`

**Response:** `204 No Content`

---


## 8. Interaction APIs

Interaction bao gồm likes và comments trên bài đăng.

### 8.1 Tạo interaction (Like hoặc Comment)

**Endpoint:** `POST /api/Interaction`

**Request Body (Like):**
```json
{
  "postId": 1,
  "userId": 5,
  "isLiked": true,
  "content": null
}
```

**Request Body (Comment):**
```json
{
  "postId": 1,
  "userId": 5,
  "isLiked": false,
  "content": "Great opportunity! How can I apply?"
}
```

**Response:** `201 Created`
```json
{
  "id": 15,
  "postId": 1,
  "userId": 5,
  "isLiked": false,
  "content": "Great opportunity! How can I apply?",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

---

### 8.2 Lấy interactions của bài đăng

**Endpoint:** `GET /api/Interaction/post/{postId}`

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": 1,
      "postId": 1,
      "userId": 5,
      "isLiked": true,
      "content": null,
      "createdAt": "2025-01-14T10:15:00",
      "user": {
        "id": 5,
        "fullName": "Nguyễn Văn E",
        "avatar": "https://example.com/avatar5.jpg"
      }
    },
    {
      "id": 2,
      "postId": 1,
      "userId": 6,
      "isLiked": false,
      "content": "Mức lương bao nhiêu vậy ạ?",
      "createdAt": "2025-01-14T10:20:00",
      "user": {
        "id": 6,
        "fullName": "Trần Thị F",
        "avatar": "https://example.com/avatar6.jpg"
      }
    }
  ]
}
```

---

### 8.3 Xóa interaction

**Endpoint:** `DELETE /api/Interaction/{id}`

**Response:** `204 No Content`

---


## 9. Follow APIs

### 9.1 Toggle follow công ty

**Endpoint:** `POST /api/follows`

**Request Body:**
```json
{
  "userId": 5,
  "companyId": 1
}
```

**Response:** `200 OK`
```json
{
  "followed": true
}
```

**Note:** Nếu đã follow thì sẽ unfollow, nếu chưa follow thì sẽ follow.

---

### 9.2 Lấy danh sách công ty user đang follow

**Endpoint:** `GET /api/follows/user/{userId}`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 5,
  "totalPages": 1,
  "data": [
    {
      "userId": 5,
      "companyId": 1,
      "createdAt": "2025-01-10T09:00:00",
      "company": {
        "id": 1,
        "name": "FPT Software",
        "avatar": "https://example.com/fpt-logo.png",
        "description": "Công ty phần mềm hàng đầu Việt Nam"
      }
    }
  ]
}
```

---

### 9.3 Lấy danh sách user follow công ty

**Endpoint:** `GET /api/follows/company/{companyId}`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 150,
  "totalPages": 15,
  "data": [
    {
      "userId": 5,
      "companyId": 1,
      "createdAt": "2025-01-10T09:00:00",
      "user": {
        "id": 5,
        "fullName": "Nguyễn Văn E",
        "avatar": "https://example.com/avatar5.jpg",
        "email": "user5@example.com"
      }
    }
  ]
}
```

---


## 10. Review APIs

### 10.1 Lấy chi tiết review

**Endpoint:** `GET /api/Review/{id}`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 5,
    "userName": "Nguyễn Văn E",
    "userAvatar": "https://example.com/avatar5.jpg",
    "companyId": 1,
    "companyName": "FPT Software",
    "rating": 5,
    "comment": "Môi trường làm việc chuyên nghiệp, đồng nghiệp thân thiện.",
    "createdAt": "2025-01-10T14:30:00",
    "updatedAt": "2025-01-10T14:30:00"
  }
}
```

---

### 10.2 Lấy reviews của công ty

**Endpoint:** `GET /api/Review/company/{companyId}`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "page": 1,
    "pageSize": 10,
    "totalItems": 25,
    "totalPages": 3,
    "data": [
      {
        "id": 1,
        "userId": 5,
        "userName": "Nguyễn Văn E",
        "userAvatar": "https://example.com/avatar5.jpg",
        "companyId": 1,
        "companyName": "FPT Software",
        "rating": 5,
        "comment": "Môi trường làm việc chuyên nghiệp",
        "createdAt": "2025-01-10T14:30:00"
      }
    ]
  }
}
```

---

### 10.3 Lấy reviews của user

**Endpoint:** `GET /api/Review/user/{userId}`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "page": 1,
    "pageSize": 10,
    "totalItems": 3,
    "totalPages": 1,
    "data": [
      {
        "id": 1,
        "userId": 5,
        "userName": "Nguyễn Văn E",
        "companyId": 1,
        "companyName": "FPT Software",
        "rating": 5,
        "comment": "Môi trường làm việc tốt",
        "createdAt": "2025-01-10T14:30:00"
      }
    ]
  }
}
```

---

### 10.4 Tạo review mới

**Endpoint:** `POST /api/Review`

**Request Body:**
```json
{
  "rating": 5,
  "comment": "Công ty rất tốt, môi trường làm việc chuyên nghiệp",
  "companyId": 1
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Tạo review thành công",
  "data": {
    "id": 15,
    "userId": 5,
    "companyId": 1,
    "rating": 5,
    "comment": "Công ty rất tốt, môi trường làm việc chuyên nghiệp",
    "createdAt": "2025-01-15T10:30:00"
  }
}
```

---

### 10.5 Cập nhật review

**Endpoint:** `PUT /api/Review/{id}`

**Request Body:**
```json
{
  "rating": 4,
  "comment": "Updated review comment"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Cập nhật review thành công",
  "data": {
    "id": 1,
    "rating": 4,
    "comment": "Updated review comment",
    "updatedAt": "2025-01-15T11:00:00"
  }
}
```

---

### 10.6 Xóa review

**Endpoint:** `DELETE /api/Review/{id}`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Xóa review thành công"
}
```

---


## 11. Blog APIs

### 11.1 Lấy danh sách blog

**Endpoint:** `GET /api/Blog`

**Query Parameters:**
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)
- `categoryId` (int, optional) - Lọc theo category

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 50,
  "totalPages": 5,
  "data": [
    {
      "id": 1,
      "userId": 3,
      "categoryId": 1,
      "title": "Kinh nghiệm phỏng vấn vị trí Frontend Developer",
      "excerpt": "Chia sẻ những câu hỏi thường gặp và cách chuẩn bị...",
      "content": "## Giới thiệu\n\nPhỏng vấn vị trí Frontend Developer...",
      "readTime": "5 phút đọc",
      "image": "https://example.com/blog-image.jpg",
      "createdAt": "2025-01-10T09:00:00",
      "updatedAt": "2025-01-10T09:00:00",
      "author": {
        "id": 3,
        "fullName": "Nguyễn Văn C",
        "avatar": "https://example.com/avatar3.jpg"
      },
      "category": {
        "id": 1,
        "name": "Phỏng vấn"
      }
    }
  ]
}
```

---

### 11.2 Lấy chi tiết blog

**Endpoint:** `GET /api/Blog/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "userId": 3,
  "categoryId": 1,
  "title": "Kinh nghiệm phỏng vấn vị trí Frontend Developer",
  "excerpt": "Chia sẻ những câu hỏi thường gặp và cách chuẩn bị...",
  "content": "## Giới thiệu\n\nPhỏng vấn vị trí Frontend Developer là một quá trình...",
  "readTime": "5 phút đọc",
  "image": "https://example.com/blog-image.jpg",
  "createdAt": "2025-01-10T09:00:00",
  "updatedAt": "2025-01-10T09:00:00",
  "author": {
    "id": 3,
    "fullName": "Nguyễn Văn C",
    "avatar": "https://example.com/avatar3.jpg",
    "email": "user3@example.com"
  },
  "category": {
    "id": 1,
    "name": "Phỏng vấn"
  }
}
```

---

### 11.3 Tạo blog mới

**Endpoint:** `POST /api/Blog`

**Request Body:**
```json
{
  "userId": 3,
  "categoryId": 1,
  "title": "Cách học React.js hiệu quả",
  "excerpt": "Hướng dẫn chi tiết về cách học React.js từ cơ bản đến nâng cao",
  "content": "## Giới thiệu\n\nReact.js là một thư viện JavaScript...",
  "readTime": "10 phút đọc",
  "image": "https://example.com/react-blog.jpg"
}
```

**Response:** `201 Created`
```json
{
  "id": 15,
  "userId": 3,
  "categoryId": 1,
  "title": "Cách học React.js hiệu quả",
  "excerpt": "Hướng dẫn chi tiết về cách học React.js từ cơ bản đến nâng cao",
  "content": "## Giới thiệu\n\nReact.js là một thư viện JavaScript...",
  "readTime": "10 phút đọc",
  "image": "https://example.com/react-blog.jpg",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

---

### 11.4 Cập nhật blog

**Endpoint:** `PUT /api/Blog/{id}`

**Request Body:**
```json
{
  "userId": 3,
  "categoryId": 1,
  "title": "Cách học React.js hiệu quả (Updated)",
  "excerpt": "Updated excerpt",
  "content": "Updated content...",
  "readTime": "12 phút đọc",
  "image": "https://example.com/updated-image.jpg"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "title": "Cách học React.js hiệu quả (Updated)",
  "updatedAt": "2025-01-15T11:00:00"
}
```

---

### 11.5 Xóa blog

**Endpoint:** `DELETE /api/Blog/{id}`

**Response:** `204 No Content`

---


## 12. Blog Category APIs

### 12.1 Lấy danh sách category

**Endpoint:** `GET /api/BlogCategory`

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": 1,
      "name": "Phỏng vấn",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    },
    {
      "id": 2,
      "name": "Học tập",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    },
    {
      "id": 3,
      "name": "Tìm việc",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    },
    {
      "id": 4,
      "name": "Nghề nghiệp",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### 12.2 Lấy chi tiết category

**Endpoint:** `GET /api/BlogCategory/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Phỏng vấn",
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

---

### 12.3 Tạo category mới

**Endpoint:** `POST /api/BlogCategory`

**Request Body:**
```json
{
  "name": "Công nghệ"
}
```

**Response:** `201 Created`
```json
{
  "id": 5,
  "name": "Công nghệ",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

---

### 12.4 Cập nhật category

**Endpoint:** `PUT /api/BlogCategory/{id}`

**Request Body:**
```json
{
  "name": "Công nghệ mới"
}
```

**Response:** `200 OK`
```json
{
  "id": 5,
  "name": "Công nghệ mới",
  "updatedAt": "2025-01-15T11:00:00"
}
```

---

### 12.5 Xóa category

**Endpoint:** `DELETE /api/BlogCategory/{id}`

**Response:** `204 No Content`

---


## 13. Search APIs

### 13.1 Tìm kiếm công việc

**Endpoint:** `GET /api/Search`

**Query Parameters:**
- `keyword` (string, optional) - Từ khóa tìm kiếm
- `location` (string, optional) - Địa điểm
- `type` (string, optional) - Loại công việc (full-time, part-time)
- `skillIds` (array of int, optional) - Danh sách skill IDs
- `PageNumber` (int, default: 1)
- `PageSize` (int, default: 10)

**Example:** `GET /api/Search?keyword=frontend&location=HCM&type=full-time&PageNumber=1&PageSize=10`

**Response:** `200 OK`
```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 15,
  "totalPages": 2,
  "data": [
    {
      "id": 1,
      "title": "Senior Frontend Developer (ReactJS)",
      "description": "Phát triển ứng dụng web với ReactJS...",
      "type": "full-time",
      "quantity": 2,
      "deadline": "2025-12-31",
      "status": "open",
      "company": {
        "id": 1,
        "name": "FPT Software",
        "avatar": "https://example.com/fpt-logo.png",
        "address": "Tòa nhà FPT, Quận 9"
      },
      "skills": [
        {
          "id": 1,
          "name": "ReactJS"
        },
        {
          "id": 2,
          "name": "TypeScript"
        }
      ]
    }
  ]
}
```

---


## 14. Location APIs

### 14.1 Lấy danh sách tỉnh/thành phố

**Endpoint:** `GET /api/Locations/provinces`

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": 1,
      "name": "TP. Hồ Chí Minh"
    },
    {
      "id": 2,
      "name": "Hà Nội"
    },
    {
      "id": 3,
      "name": "Đà Nẵng"
    }
  ]
}
```

---

### 14.2 Lấy danh sách quận/huyện theo tỉnh

**Endpoint:** `GET /api/Locations/wards/{provinceId}`

**Response:** `200 OK`
```json
{
  "data": [
    {
      "id": 1,
      "provinceId": 1,
      "name": "Phường Bến Nghé"
    },
    {
      "id": 2,
      "provinceId": 1,
      "name": "Phường Tân Định"
    }
  ]
}
```

---


## Data Models

### UserResponse

```json
{
  "id": 1,
  "fullName": "Nguyễn Văn A",
  "email": "user@example.com",
  "phone": "0901234567",
  "gender": "male",
  "dateOfBirth": "1995-01-15",
  "avatar": "https://example.com/avatar.jpg",
  "coverImage": "https://example.com/cover.jpg",
  "cvUrl": "https://drive.google.com/cv.pdf",
  "role": "user",
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

---

### LoginResponse

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "base64_encoded_refresh_token",
  "user": {
    "id": 1,
    "fullName": "Nguyễn Văn A",
    "email": "user@example.com",
    "role": "user"
  }
}
```

---

### CompanyResponse

```json
{
  "id": 1,
  "name": "FPT Software",
  "avatar": "https://example.com/fpt-logo.png",
  "coverImage": "https://example.com/fpt-cover.jpg",
  "nationality": "Việt Nam",
  "website": "https://fptsoftware.com",
  "description": "Công ty phần mềm hàng đầu Việt Nam",
  "foundedYear": 1999,
  "address": "Tòa nhà FPT, Quận 9",
  "wardId": 1,
  "wardName": "Phường Bến Nghé",
  "provinceName": "TP. Hồ Chí Minh",
  "createdByUserId": 1,
  "createdAt": "2020-01-15T09:00:00",
  "updatedAt": "2025-01-15T10:00:00"
}
```

---

### JobResponse

```json
{
  "id": 1,
  "companyId": 1,
  "title": "Senior Frontend Developer (ReactJS)",
  "description": "Phát triển ứng dụng web với ReactJS...",
  "type": "full-time",
  "quantity": 2,
  "deadline": "2025-12-31",
  "status": "open",
  "createdAt": "2025-01-10T09:00:00",
  "updatedAt": "2025-01-10T09:00:00",
  "company": {
    "id": 1,
    "name": "FPT Software",
    "avatar": "https://example.com/fpt-logo.png"
  },
  "skills": [
    {
      "id": 1,
      "name": "ReactJS"
    }
  ]
}
```

---

### ApplicationResponse

```json
{
  "jobId": 1,
  "userId": 5,
  "cvUrl": "https://drive.google.com/cv.pdf",
  "coverLetter": "I am interested in this position...",
  "status": "pending",
  "createdAt": "2025-01-12T14:30:00",
  "updatedAt": "2025-01-12T14:30:00",
  "job": {
    "id": 1,
    "title": "Senior Frontend Developer (ReactJS)"
  },
  "user": {
    "id": 5,
    "fullName": "Nguyễn Văn E"
  }
}
```

---

### PostResponse

```json
{
  "id": 1,
  "content": "Chúng tôi đang tuyển dụng Senior Frontend Developer!",
  "userId": 3,
  "companyId": 1,
  "createdAt": "2025-01-14T10:00:00",
  "updatedAt": "2025-01-14T10:00:00",
  "user": {
    "id": 3,
    "fullName": "Nguyễn Văn C",
    "avatar": "https://example.com/avatar3.jpg"
  },
  "company": {
    "id": 1,
    "name": "FPT Software",
    "avatar": "https://example.com/fpt-logo.png"
  },
  "attachments": [
    {
      "id": 1,
      "fileType": "image",
      "fileUrl": "https://example.com/post-image.jpg"
    }
  ],
  "totalLikes": 15,
  "totalComments": 8
}
```

---

### InteractionResponse

```json
{
  "id": 1,
  "postId": 1,
  "userId": 5,
  "isLiked": true,
  "content": null,
  "createdAt": "2025-01-14T10:15:00",
  "updatedAt": "2025-01-14T10:15:00",
  "user": {
    "id": 5,
    "fullName": "Nguyễn Văn E",
    "avatar": "https://example.com/avatar5.jpg"
  }
}
```

---

### ReviewResponse

```json
{
  "id": 1,
  "userId": 5,
  "userName": "Nguyễn Văn E",
  "userAvatar": "https://example.com/avatar5.jpg",
  "companyId": 1,
  "companyName": "FPT Software",
  "rating": 5,
  "comment": "Môi trường làm việc chuyên nghiệp",
  "createdAt": "2025-01-10T14:30:00",
  "updatedAt": "2025-01-10T14:30:00"
}
```

---

### BlogResponse

```json
{
  "id": 1,
  "userId": 3,
  "categoryId": 1,
  "title": "Kinh nghiệm phỏng vấn vị trí Frontend Developer",
  "excerpt": "Chia sẻ những câu hỏi thường gặp...",
  "content": "## Giới thiệu\n\nPhỏng vấn vị trí Frontend Developer...",
  "readTime": "5 phút đọc",
  "image": "https://example.com/blog-image.jpg",
  "createdAt": "2025-01-10T09:00:00",
  "updatedAt": "2025-01-10T09:00:00",
  "author": {
    "id": 3,
    "fullName": "Nguyễn Văn C",
    "avatar": "https://example.com/avatar3.jpg"
  },
  "category": {
    "id": 1,
    "name": "Phỏng vấn"
  }
}
```

---

### ResponseData<T> (Pagination)

```json
{
  "page": 1,
  "pageSize": 10,
  "totalItems": 100,
  "totalPages": 10,
  "data": [
    // Array of T
  ]
}
```

---


## Error Handling

### Error Response Format

Tất cả các lỗi đều trả về theo format:

```json
{
  "success": false,
  "message": "Error message description"
}
```

### HTTP Status Codes

| Status Code | Meaning | Description |
|-------------|---------|-------------|
| 200 | OK | Request thành công |
| 201 | Created | Tạo resource thành công |
| 204 | No Content | Xóa resource thành công |
| 400 | Bad Request | Request không hợp lệ (validation error) |
| 401 | Unauthorized | Chưa đăng nhập hoặc token không hợp lệ |
| 403 | Forbidden | Không có quyền truy cập |
| 404 | Not Found | Resource không tồn tại |
| 500 | Internal Server Error | Lỗi server |

### Common Error Examples

**400 Bad Request - Validation Error:**
```json
{
  "success": false,
  "message": "Email đã được sử dụng"
}
```

**401 Unauthorized:**
```json
{
  "success": false,
  "message": "Token không hợp lệ hoặc đã hết hạn"
}
```

**403 Forbidden:**
```json
{
  "message": "Access denied. Required roles: employer. Your role: user"
}
```

**404 Not Found:**
```json
{
  "message": "Company not found"
}
```

---

## Testing với Swagger

API documentation có sẵn Swagger UI tại:

```
http://localhost:5000/swagger
```

Swagger UI cho phép:
- Xem tất cả endpoints
- Test API trực tiếp từ browser
- Xem request/response schema
- Thêm JWT token để test authenticated endpoints

### Cách sử dụng JWT trong Swagger:

1. Đăng nhập qua endpoint `/api/Auth/login`
2. Copy `accessToken` từ response
3. Click nút **Authorize** ở góc trên bên phải
4. Nhập: `Bearer <your_access_token>`
5. Click **Authorize**
6. Bây giờ có thể test các protected endpoints

---

## Environment Variables

Tạo file `.env` trong thư mục root:

```env
# Database
DefaultConnection=Server=localhost;Database=ITJOB;User Id=sa;Password=YourPassword;TrustServerCertificate=True;

# JWT Settings
SecretKey=your-super-secret-key-at-least-32-characters-long
Issuer=ITJOB_API
Audience=ITJOB_Client
AccessTokenExpireMinutes=60
RefreshTokenExpireDays=7

# Cloudinary
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name
```

---

## Setup & Run

### Prerequisites

- .NET 8.0 SDK
- SQL Server
- Visual Studio 2022 hoặc VS Code

### Installation

1. Clone repository:
```bash
git clone <repository-url>
cd ITJOB-BE
```

2. Restore packages:
```bash
dotnet restore
```

3. Tạo file `.env` và cấu hình connection string

4. Run migrations:
```bash
dotnet ef database update
```

5. Run application:
```bash
dotnet run
```

6. Truy cập Swagger UI:
```
http://localhost:5000/swagger
```

---

## Database Schema

### Entity Relationship Diagram

```
User (1) ----< (N) Application (N) >---- (1) Job (N) >---- (1) Company
  |                                          |                    |
  |                                          |                    |
  +--< (N) SkillUser (N) >--< (1) Skill     +--< (N) SkillJob    +--< (N) Follow
  |                                                                |
  +--< (N) Post                                                    +--< (N) Review
  |         |                                                      |
  |         +--< (N) Interaction                                   +--< (N) CompanyMember
  |         +--< (N) Attachment
  |
  +--< (N) Blog (N) >---- (1) BlogCategory
```

### Key Tables

- **User**: Người dùng (user/employer/admin)
- **Company**: Công ty
- **CompanyMember**: Liên kết user-company
- **Job**: Công việc
- **Application**: Đơn ứng tuyển
- **Skill**: Kỹ năng
- **Post**: Bài đăng mạng xã hội
- **Interaction**: Like/Comment
- **Follow**: Theo dõi công ty
- **Review**: Đánh giá công ty
- **Blog**: Bài viết blog

---

## Contact & Support

- **Email**: support@itjob.com
- **Documentation**: http://localhost:5000/swagger
- **Repository**: <repository-url>

---

## License

Copyright © 2025 ITJOB-BE. All rights reserved.

---

**Last Updated:** January 15, 2025
**API Version:** 1.0.0
