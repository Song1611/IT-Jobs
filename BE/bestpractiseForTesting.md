# Best Practices for Testing — ITJob Backend

## Cấu trúc thư mục test chuẩn

```
src/test/java/com/itjob
│
├── unit
│   ├── util
│   ├── specification
│   ├── service
│   ├── mapper
│   └── aspect
│
├── integration
│   ├── repository
│   ├── service
│   ├── controller
│   ├── security
│   └── cache
│
└── fixture
    ├── UserFixture
    ├── JobFixture
    └── CompanyFixture
```

---

## Ý nghĩa từng thư mục

### `unit/`
Test đơn vị, chạy nhanh, không cần Spring context, không cần hạ tầng bên ngoài.

- `util/` — `CacheKeyGenerator`, `SlugUtil`, `HashUtil`, `FilterParser`... (pure function)
- `specification/` — `TypeConverter`, `FilterValidator`, `SpecificationHelper`, `GenericSpecificationBuilder`
- `service/` — service có logic độc lập: `OtpServiceImpl`, `RateLimitServiceImpl`, `DistributedLockServiceImpl`, `JwtServiceImpl`, `EmailServiceImpl`, `CloudinaryServiceImpl`, `RecommendationServiceImpl`
- `mapper/` — chỉ mapper có logic thủ công: `JobMapper`, `ApplicationMapper`, `UserMapper`, `CommentMapper`, `GenderConverter`
- `aspect/` — `DistributedLockAspect`, `RateLimitAspect` (SpEL key, retry, resolve identifier)

### `integration/`
Test tích hợp, dùng Testcontainers (PostgreSQL + Redis) hoặc MockMvc.

- `repository/` — `@DataJpaTest` kế thừa `AbstractPostgresIntegrationTest`; kiểm tra `@Query`, projection, pagination, specification, constraint
- `service/` — `@SpringBootTest`; kiểm tra chuỗi Service → Repository → Redis → Postgres, cache annotation, transaction
- `controller/` — `@SpringBootTest` + MockMvc; kiểm tra status code, validation, JWT, JSON response, exception
- `security/` — `SecurityConfig`, `CustomJwtDecoder`; endpoints public vs protected, roles
- `cache/` — `JobCacheServiceImpl`, `PostCacheServiceImpl`, `CompanyCacheServiceImpl`, `DashboardCacheServiceImpl`; hit/miss/evict với Redis thật

### `fixture/`
Factory tạo dữ liệu dùng chung, giảm lặp code giữa các test.

- `UserFixture` — cho phép tạo `User`, `Role`, `Permission` với state mặc định
- `JobFixture` — tạo `Job`, `Company`, `Skill` chuẩn
- `CompanyFixture` — tạo `Company`, `CompanyImage`, `CompanyMember`

---

## Nguyên tắc best practice

1. **1 test class = 1 behavior/cụm liên quan** — đặt tên theo pattern `XxxServiceTest`, `XxxRepositoryTest`, `XxxControllerTest`.
2. **AAA pattern** — Arrange → Act → Assert; mỗi test chỉ kiểm tra một hành vi.
3. **Không test trùng trách nhiệm** — đã có Integration thì bỏ Unit (trừ khi logic phức tạp hoặc phụ thuộc infra như Lua script/cache annotation).
4. **Dùng fixture dùng chung** — không tạo entity rải rác trong từng test.
5. **`@DataJpaTest` + `AbstractPostgresIntegrationTest`** cho repository; `@SpringBootTest` cho service/controller/cache.
6. **Container chung (singleton)** — một lần khởi động cho cả suite, dùng `@DynamicPropertySource` để inject JDBC URL.
7. **Kiểm soát state DB giữa test** — rollback transaction hoặc `TRUNCATE` để không nhiễu dữ liệu.
8. **Name test theo behavior** — `shouldThrowXxx_whenCondition`, bằng tiếng Anh hoặc `@DisplayName` tiếng Việt, mỗi test đọc vào hiểu ngay ý nghĩa.
9. **Tách pure logic ra Util/service thuần** — dễ unit test mà không cần mock phức tạp (ví dụ `SlugUtil` đã tách khỏi service).
10. **Ưu tiên coverage theo business flow** — Authentication → Job → Company → Application → Reaction → Refresh Token, hơn là chạy theo số lượng test.