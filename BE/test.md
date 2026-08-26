# Kế hoạch Test Backend ITJob

> Lưu ý của chủ dự án về chiến lược test sau khi đọc kỹ danh sách ~70 test class ban đầu.
> Mục tiêu: tinh gọn xuống còn **~45–55 test class** mà vẫn giữ chất lượng cao, tránh test trùng trách nhiệm.

---

## 1. Đánh giá tổng thể

Kiến trúc phân tầng đề xuất rất tốt:

```
Unit → Repository IT → Service IT → Controller IT
```

Nhưng 70 test class là hơi nhiều, có một số chỗ test trùng trách nhiệm.
Đây là đồ án/portfolio nên cần tinh gọn, ưu tiên **business value** hơn là số lượng.

### Đánh giá theo từng nhóm

| Nhóm | Đánh giá | Khuyến nghị |
|---|---|---|
| A. util | ✅ Rất nên giữ | Giữ nguyên |
| B. specification | ✅ Nên giữ | Giữ nguyên |
| C. service (unit) | ⚠️ Chọn lọc | Không cần unit cho service đã có IT đầy đủ |
| D. mapper | ⚠️ Giữ mapper có logic | Mapper MapStruct đơn giản không cần test riêng |
| E. repository | ✅ Rất quan trọng | Giữ nguyên |
| F. service IT | ⭐ Quan trọng nhất | Giữ |
| G. controller | ✅ Giữ | Giữ |
| H. config | ⚠️ Chọn lọc | Một số không đáng test |

---

## 2. Quyết định theo từng nhóm

### A. Utility — Giữ 100% (pure function, nhanh, không phụ thuộc Spring)

- `CacheKeyGeneratorTest`
- `HashUtilTest`
- `SlugUtilTest`
- `FilterParserTest`
- `OperationResolverTest`
- `RedisOperationTest`
- `PageResponseUtilTest`
- `SecurityUtilTest`

### B. Specification — Giữ

- `TypeConverterTest`
- `FilterValidatorTest`
- `SpecificationHelperTest`
- `GenericSpecificationBuilderTest` — rất dễ phát sinh bug, đặc biệt quan trọng

### C. Service Unit — Tối ưu (chia làm 2 loại)

**Nên unit test** — service có thuật toán hoặc logic độc lập:

- `OtpServiceImplTest`
- `RateLimitServiceImplTest`
- `DistributedLockServiceImplTest`
- `SearchSuggestionServiceImplTest`
- `TrendingJobServiceImplTest`
- `JwtServiceImplTest`
- `EmailServiceImplTest`
- `CloudinaryServiceImplTest`
- `DistributedLockAspectTest`
- `RateLimitAspectTest`
- `RecommendationServiceImplTest`

**Không cần Unit + Integration cùng lúc** — nếu đã có IT đầy đủ thì bỏ unit:

- `JobServiceImpl`
- `CompanyServiceImpl`
- `UserServiceImpl`
- `BlogServiceImpl`
- `CommentServiceImpl`
- `ReactionServiceImpl`

> Nguyên tắc: **không test cùng một behavior hai lần.**

### D. Mapper — Đừng test tất cả

Mapper chỉ là `UserResponse toResponse(User user)` (MapStruct sinh code) → **không cần test**.
Bỏ: `RoleMapperTest`, `SkillMapperTest`, `ProvinceMapperTest`, `PermissionMapperTest`...

**Chỉ test mapper có logic** (custom expression / enum conversion / nested mapping / date conversion):

- `JobMapperTest`
- `ApplicationMapperTest`
- `UserMapperTest`
- `CommentMapperTest`
- `GenderConverterTest`

### E. Repository — Giữ 100%

Nhóm có giá trị rất cao, là lý do dùng Testcontainers.
Đặc biệt kiểm tra:

- Projection (`CompanyJobCountProjection`, `JobApplicationCountProjection`)
- `@Query` (JPQL)
- `CASE WHEN`
- `COALESCE`
- Pagination
- Specification

### F. Service Integration — Quan trọng nhất, giữ gần hết

Đây là nơi kiểm tra toàn bộ chuỗi: Service → Repository → Redis → Postgres.

- `AuthenticationServiceImplTest`
- `JobServiceImplTest`
- `CompanyServiceImplTest`
- `ApplicationServiceImplTest`
- `ReactionServiceImplTest`
- `RefreshTokenServiceImplTest`
- `ViewCountServiceImplIT`
- `RecommendationServiceImplIT`

### G. Controller — Giữ (1 controller = 1 file, chuẩn)

MockMvc kiểm tra: status, validation, JWT, JSON, exception.

- `AuthenticationControllerTest`
- `JobControllerTest`
- `CompanyControllerTest`
- `ApplicationControllerTest`
- `AdminControllerTest`

### H. Config — Tối ưu

**Giữ:**
- `SecurityConfigTest`
- `CustomJwtDecoderTest`
- `GlobalExceptionHandlerTest`

**Không cần thiết:**
- `RedisConfigTest` — nếu Redis serialize đã được kiểm chứng bởi `JobCacheServiceImplTest`, `CompanyCacheServiceImplTest`
- `ApplicationInitConfigTest` — chỉ giữ nếu seed có nhiều business logic

---

## 3. Cấu trúc thư mục test

```
src/test/java/com/itjob
│
├── unit
│   ├── util
│   ├── specification
│   ├── service
│   ├── aspect
│   └── mapper
│
├── integration
│   ├── repository
│   ├── service
│   ├── controller
│   ├── security
│   └── cache
│
├── fixture
│
└── config
    └── AbstractPostgresIntegrationTest
```

---

## 4. Base TestContainer dùng chung

Một base class để tất cả IT dùng **chung 1 container**, không lặp ở 30 class.

```java
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17")
            .withReuse(true);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

Cách dùng:

```java
@DataJpaTest
class JobRepositoryTest extends AbstractPostgresIntegrationTest {
}

@SpringBootTest
class JobServiceImplTest extends AbstractPostgresIntegrationTest {
}
```

---

## 5. Nguyên tắc chốt

1. Test theo **business value**, không chạy theo số lượng.
2. Không test cùng một behavior ở cả Unit và Integration **trừ khi**: service đó có logic phức tạp, hoặc behavior phụ thuộc infra (Lua script, cache annotation, scheduled job).
3. Repository + Service IT + Controller IT là ba lớp bắt buộc đủ.
4. Mapper chỉ test khi có logic thủ công.
5. Ưu tiên coverage theo flow nghiệp vụ: Authentication → Job → Company → Application → Reaction → Refresh Token.