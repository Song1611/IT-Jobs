# Lộ trình Test Backend ITJob

## Lộ trình khuyến nghị

1. **Utility Unit Test** — Khởi động nhanh, không cần Spring hay Docker
2. **Specification Unit Test** — Kiểm tra filter, converter và builder
3. **Service Unit Test (logic thuần)** — OTP, RateLimit, JWT, Aspect, Recommendation
4. **Repository Integration Test** — PostgreSQL + Testcontainers
5. **Service Integration Test** — CRUD, Cache, Redis, Transaction
6. **Controller Integration Test** — MockMvc + JWT + Validation

---

## Giai đoạn 1 — Utility (nên làm đầu tiên)

Đây là nhóm dễ nhất và giúp làm quen với JUnit.

Viết theo thứ tự:

- `CacheKeyGeneratorTest`
- `SlugUtilTest`
- `HashUtilTest`
- `OperationResolverTest`
- `FilterParserTest`
- `PageResponseUtilTest`
- `SecurityUtilTest`

Ví dụ `CacheKeyGeneratorTest` chỉ cần kiểm tra:

| Behavior | Test |
|---|---|
| Pageable bình thường | ✅ |
| Có sort | ✅ |
| Không sort | ✅ |
| Null filter | ✅ |

> Không cần Mockito, không cần Spring.

---

## Giai đoạn 2 — Specification

Sau khi quen AAA pattern thì sang:

- `TypeConverterTest`
- `FilterValidatorTest`
- `SpecificationHelperTest`
- `GenericSpecificationBuilderTest`

Đây vẫn là pure unit test.

---

## Giai đoạn 3 — Service Unit

Nhóm này mới dùng Mockito.

Bắt đầu bằng những service ít phụ thuộc:

- `JwtServiceImplTest`
- `OtpServiceImplTest`
- `RateLimitServiceImplTest`
- `DistributedLockServiceImplTest`
- `EmailServiceImplTest`
- `CloudinaryServiceImplTest`

Lúc này sẽ luyện:

- `@Mock`
- `@InjectMocks`
- `when()`
- `verify()`
- `ArgumentCaptor`

---

## Giai đoạn 4 — Repository Integration

Đây là lúc đưa Testcontainers vào.

Thứ tự:

- `UserRepositoryTest`
- `JobRepositoryTest`
- `CompanyRepositoryTest`
- `ApplicationRepositoryTest`
- `ReactionRepositoryTest`

Kiểm tra:

- `@Query`
- Projection
- Pagination
- Specification
- Constraint

---

## Giai đoạn 5 — Service Integration

Đây là phần giá trị nhất của ITJob.

Nên làm theo business priority:

- `AuthenticationServiceImplTest`
- `JobServiceImplTest`
- `CompanyServiceImplTest`
- `ApplicationServiceImplTest`
- `ReactionServiceImplTest`
- `RefreshTokenServiceImplTest`

Sau đó mới đến:

- `BlogService`
- `CommentService`
- `ReviewService`
- `ProvinceService`
- `DashboardService`

---

## Giai đoạn 6 — Controller

Cuối cùng mới viết MockMvc.

Ví dụ:

- `AuthenticationControllerTest`
- `JobControllerTest`
- `CompanyControllerTest`
- `ApplicationControllerTest`
- `UserControllerTest`

Kiểm tra:

- 200 / 201 / 400 / 401 / 403
- JSON response
- Validation

---

## Tuần tự thực hiện (thực tế)

1. `CacheKeyGeneratorTest`
2. `SlugUtilTest`
3. `HashUtilTest`
4. `OperationResolverTest`
5. `FilterParserTest`
6. `JwtServiceImplTest`
7. `OtpServiceImplTest`
8. `RateLimitServiceImplTest`
9. `UserRepositoryTest`
10. `JobRepositoryTest`
11. `AuthenticationServiceImplTest`
12. `JobServiceImplTest`
13. `AuthenticationControllerTest`
14. `JobControllerTest`

---

## Tại sao không bắt đầu bằng JobServiceImplTest?

Vì nó cùng lúc dính rất nhiều thứ:

```
JobService
 ├── Repository
 ├── Mapper
 ├── Redis
 ├── Cloudinary
 ├── Specification
 ├── Security
 └── Transaction
```

Nếu chưa quen Mockito và Testcontainers thì sẽ rất dễ rối.

Trong khi `CacheKeyGeneratorTest` chỉ có:

```
Input
 ↓
Method
 ↓
Assert
```

Học được AAA pattern trước, rồi mới tăng dần độ khó.

---

## Kết luận

Bắt đầu từ nhóm A (Utility Unit Test) là hợp lý nhất. Sau khi hoàn thành nhóm A và B, sẽ nắm vững JUnit. Nhóm C giúp thành thạo Mockito, rồi mới chuyển sang Testcontainers ở nhóm E và F. Đây là lộ trình có độ khó tăng dần và cũng là cách nhiều dự án Spring Boot thực tế triển khai test suite.