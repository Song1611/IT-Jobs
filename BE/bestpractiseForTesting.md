# Best Practices for Testing — ITJob Backend

> Mục tiêu: tài liệu này là chuẩn tham chiếu khi viết bất kỳ test nào trong dự án.
> Áp dụng cho cả Unit test và Integration test. Khi review code, đối chiếu theo checklist ở cuối file.

---

## 1. Cấu trúc thư mục test chuẩn

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

## 2. Ý nghĩa từng thư mục

### `unit/`
Test đơn vị, chạy nhanh (< 1s/class), không cần Spring context, không cần hạ tầng bên ngoài.

- `util/` — `CacheKeyGenerator`, `SlugUtil`, `HashUtil`, `FilterParser`, `RedisOperation`... (pure function)
- `specification/` — `TypeConverter`, `FilterValidator`, `SpecificationHelper`, `GenericSpecificationBuilder`
- `service/` — service có logic độc lập: `OtpServiceImpl`, `RateLimitServiceImpl`, `DistributedLockServiceImpl`, `JwtServiceImpl`, `EmailServiceImpl`, `CloudinaryServiceImpl`, `RecommendationServiceImpl`
- `mapper/` — chỉ mapper có logic thủ công: `JobMapper`, `ApplicationMapper`, `UserMapper`, `CommentMapper`, `GenderConverter`
- `aspect/` — `DistributedLockAspect`, `RateLimitAspect` (SpEL key, retry, resolve identifier)

Quy tắc cứng: **unit test không được** connect DB/Redis, không được `@SpringBootTest`.

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

Nguyên tắc fixture: mặc định hợp lệ + tối thiểu trường, cho phép override từng field khi test cụ thể cần.

---

## 3. Viết code sao cho DỄ TEST (testable design)

Đây là phần quan trọng nhất: **test khó viết thường là do production code khó test**, không phải do người viết test.

### 3.1. Tách pure logic ra Util/static

Logic tính toán, format, parse, validate không phụ thuộc I/O → đưa vào class `final` + constructor private, method `static`:

```java
// TỐT: pure function — test không cần mock gì cả
public final class CacheKeyGenerator {
    private CacheKeyGenerator() {}
    public static String forPageable(Pageable p) { ... }
}
```

Ví dụ trong dự án: `SlugUtil` đã tách khỏi service → chỉ cần assert in/out, không cần Spring.

### 3.2. Constructor injection, không dùng field injection

```java
// TỐT — dễ new bằng tay trong unit test, dễ override dependency
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
}

// XẤU — @Autowired trên field: phải dùng reflection để thay dependency
@Service
public class JobServiceImpl {
    @Autowired private JobRepository jobRepository;
}
```

### 3.3. Không giữ state tĩnh thay đổi được (mutable static state)

Trừ `SecurityContextHolder` (bắt buộc bởi Spring Security), tránh biến static có thể bị mutate — gây test chạy lần đầu pass, lần sau fail, hoặc fail khi chạy song song.

Nếu buộc phải đọc static context (như `SecurityUtil`), test phải **setup trong `@BeforeEach` và clear trong `@AfterEach`**.

### 3.4. Trả về giá trị thay vì side-effect

Hàm trả kết quả (return) luôn dễ test hơn hàm chỉ ghi log/ghi DB rồi trả void. Khi thiết kế method mới, ưu tiên signature kiểu:

```java
FilterComponents parse(String filter);   // dễ test: assert return
void processAndSave(String filter);      // khó test: phải verify side-effect
```

### 3.5. Nhận vào abstraction, không hardcode infra

Method nhận `Supplier<T>`, interface, hoặc param — thay vì tự gọi `RedisTemplate` ngay trong thân hàm. Ví dụ chuẩn trong dự án: `RedisOperation.supply(Supplier<T> action, ...)` — test truyền lambda throw exception mà không cần Redis thật.

### 3.6. Clock/thời gian inject được

Code dùng `Instant.now()` trực tiếp sẽ không test được logic "hết hạn". Nếu nghiệp vụ phụ thuộc thời gian (OTP, refresh token), inject `Clock` bean.

---

## 4. Quy ước đặt tên

| Thứ | Quy ước | Ví dụ |
|---|---|---|
| Class test | `<ClassUnderTest>Test` | `SlugUtilTest`, `JobRepositoryTest` |
| Method test | tiếng Anh, mô tả behavior | `collapsesDuplicateHyphens()` |
| `@DisplayName` | **hoàn toàn tiếng Anh**, dạng hành vi | `"converts to lowercase"` |
| Nhóm test | `@Nested` class theo method được test | `class ForPageable { ... }` |
| IT đánh dấu | suffix `IT` hoặc `Test` + tag | `ViewCountServiceImplIT` |

Lưu ý:
- `@DisplayName` tiếng Anh **100%** — tên method đã là tiếng Anh thì DisplayName cũng vậy.
- DisplayName nên đọc thành câu mô tả hành vi: input → output, hoặc điều kiện → kết quả.
  - Tốt: `"wildcard on both sides -> CONTAINS"`
  - Tệ: `"test case 1"`, `"kiểm tra hàm parse"`

---

## 5. Cấu trúc một test chuẩn (AAA)

Mỗi test 3 đoạn: **Arrange – Act – Assert**, mỗi test chỉ kiểm tra **một hành vi**.

```java
@Test
@DisplayName("build with mapper function -> maps each item")
void buildWithMapper() {
    // Arrange — chuẩn bị input & dependency
    Page<String> page = pageOf("a", "b");

    // Act — gọi ĐÚNG 1 lần method đang test
    PageResponse<Integer> result = PageResponseUtil.build(page, String::length);

    // Assert — kiểm tra kết quả
    assertThat(result.getItems()).containsExactly(1, 1);
}
```

Quy tắc:
1. Một `@Test` = một hành vi. Nếu cần chữ "và" khi mô tả test → tách thành 2 test.
2. Không có logic if/for trong test (trừ khi chính vòng lặp là nội dung test).
3. Helper/private method cho Arrange được phép và khuyến khích (`pageOf(...)`, `createJwt(...)`).
4. Test không được phụ thuộc thứ tự chạy (`@TestMethodOrder` chỉ dùng khi thật sự bất khả kháng).

---

## 6. Quy tắc ASSERTION (quan trọng nhất khi review)

### 6.1. Assert GIÁ TRỊ CUỐI CÙNG, không assert "một phần"

Khi output là **deterministic** (luôn giống nhau với cùng input), phải assert toàn bộ:

```java
// XẤU — production trả "companyId:abc:RUBBISH:status:open" vẫn PASS
assertThat(key).startsWith("companyId:").endsWith(":status:open");

// TỐT — assert đúng chuỗi cuối cùng
assertThat(key).isEqualTo("companyId:" + companyId + ":status:open");
```

Chỉ chấp nhận assert một phần (`startsWith`, `contains`) khi output có **thành phần ngẫu nhiên/không kiểm soát được** (ví dụ slug có random suffix) — và phần ngẫu nhiên đó phải có test riêng kiểm tra format.

### 6.2. Assert ĐỦ các trường của object trả về

Parser/builder trả object nhiều trường → assert tất cả trường liên quan, không chỉ 1–2 trường:

```java
// XẤU — fieldName/operator sai vẫn PASS vì chỉ check value
assertThat(FilterParser.parse("status@active,pending").getValue()).isEqualTo("active,pending");

// TỐT
FilterComponents in = FilterParser.parse("status@active,pending");
assertThat(in.getFieldName()).isEqualTo("status");
assertThat(in.getOperator()).isEqualTo("@");
assertThat(in.getValue()).isEqualTo("active,pending");
```

### 6.3. Assert cả hướng NGƯỢC (negative case)

Với hàm boolean/phân loại, luôn kèm case false:

```java
assertThat(SecurityUtil.hasRole("ADMIN")).isTrue();
assertThat(SecurityUtil.hasRole("EMPLOYER")).isFalse();          // role khác
assertThat(SecurityUtil.hasRole("USER_READ")).isFalse();         // permission ≠ role (không có prefix ROLE_)
```

### 6.4. Dùng đúng matcher AssertJ

| Tình huống | Matcher |
|---|---|
| Bằng chuỗi/giá trị | `isEqualTo` |
| Danh sách đúng thứ tự | `containsExactly` |
| Danh sách không quan tâm thứ tự | `containsExactlyInAnyOrder` |
| Exception + kiểm tra bên trong | `assertThatThrownBy(...).isInstanceOf(...)` |
| Format chuỗi | `matches("[0-9a-f]{64}")` |
| Null / rỗng | `isNull()`, `isEmpty()`, `isZero()` |

Không dùng JUnit `assertEquals` lẫn lộn với AssertJ — chọn AssertJ cho toàn dự án.

### 6.5. Test dữ liệu phản ánh đúng hệ thống thật

Dữ liệu trong test phải theo convention production. Ví dụ authorities trong hệ thống là `ROLE_ADMIN`, `ROLE_USER` (prefix `ROLE_` từ `JwtServiceImpl.buildScope`) + permission không prefix (`USER_READ`). Test phải dựng dữ liệu y như JWT thật sinh ra, không tự chế dạng dữ liệu khác.

---

## 7. Khi nào dùng MOCK

- **Unit test service**: mock repository/mapper/client bên ngoài bằng Mockito (`@ExtendWith(MockitoExtension.class)`).
- **Không mock**: value object, DTO, util thuần, class đang được test.
- **Không over-mock**: nếu phải mock > 4–5 dependency cho 1 test → tín hiệu design cần tách lớp, hoặc behavior này nên chuyển xuống Integration test.
- Mock chỉ để **cho phép test chạy**, không phải để **chứng minh logic** — chứng minh logic là việc của assert.

---

## 8. Chọn Unit hay Integration (quyết định nhanh)

| Tình huống | Loại test |
|---|---|
| Pure function (util, parser, converter) | Unit |
| Service có thuật toán độc lập (OTP, rate limit, recommendation scoring) | Unit |
| Service CRUD đơn giản đã có IT đủ | Chỉ Integration |
| Repository `@Query`/projection/specification | Integration (`@DataJpaTest` + Testcontainers) |
| Cache annotation, Lua script, scheduled job | Integration (cần Redis thật từ Testcontainers) |
| HTTP status/validation/JWT ở endpoint | Integration (MockMvc) |

> Nguyên tắc gốc: **không test cùng một behavior hai lần.**

---

## 9. Những gì KHÔNG cần test

1. Getter/setter, Lombok-generated code.
2. Mapper MapStruct không có logic custom (`RoleMapper`, `SkillMapper`...).
3. Config chỉ khai báo bean đơn giản (`RedisConfig`).
4. Framework itself — đừng test lại Spring Data JPA hoạt động thế nào; chỉ test **query của bạn** trả đúng dữ liệu.
5. Code không thể sửa được từ phía mình (third-party).

---

## 10. Lỗi thường gặp (pitfalls)

| Lỗi | Hậu quả | Cách tránh |
|---|---|---|
| Assert một phần output | Bug ở giữa chuỗi vẫn PASS | `isEqualTo` full value khi deterministic |
| Assert thiếu trường object | Parser trả sai field vẫn PASS | Assert đủ mọi trường liên quan |
| Test phụ thuộc DB/Redis thật | Fail khi máy khác chạy | Unit: không infra; IT: Testcontainers |
| Quên clear `SecurityContextHolder` | Test rò rỉ state sang test khác | `@AfterEach` clearContext |
| Test phụ thuộc thời gian thực | Flaky lúc nửa đêm | Inject `Clock`, hoặc assert khoảng tương đối |
| Test phụ thuộc thứ tự | Pass khi chạy riêng, fail khi chạy cả suite | Mỗi test tự setup state của nó |
| DisplayName tiếng Việt/lộn xộn | Report khó đọc, không thống nhất | Tiếng Anh 100%, mô tả behavior |
| Hardcode magic value không giải thích | Người sau không hiểu expected đến từ đâu | Comment ngắn hoặc đặt tên biến có nghĩa |

---

## 11. Checklist trước khi commit test

- [ ] Tên class/method/DisplayName đúng quy ước, DisplayName tiếng Anh 100%.
- [ ] AAA rõ ràng, 1 test = 1 hành vi.
- [ ] Assertion đầy đủ: full value khi deterministic, đủ trường khi là object, có negative case khi là boolean.
- [ ] Không connect infra ngoài trong unit test.
- [ ] State được dọn sạch (`@AfterEach`) — không ảnh hưởng test khác.
- [ ] Chạy `.\mvnw.cmd test` local BUILD SUCCESS trước khi push.
- [ ] Test mới nằm đúng thư mục (`unit/...` hoặc `integration/...`).
