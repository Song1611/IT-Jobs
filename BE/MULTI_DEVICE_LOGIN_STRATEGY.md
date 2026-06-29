# 📱 Multi-Device Login Strategy

## 🎯 Current Implementation: Multi-Device Support

### ✅ Hiện Tại: CHO PHÉP Đăng Nhập Nhiều Thiết Bị

User có thể đăng nhập đồng thời trên:
- 📱 Điện thoại (iPhone)
- 💻 Laptop (MacBook)
- 🖥️ Desktop (PC)
- 📱 Tablet (iPad)

Mỗi thiết bị có **refresh token riêng**, tất cả đều valid đồng thời.

---

## 📊 So Sánh 2 Strategies

### Strategy 1: Multi-Device (Hiện Tại) ✅

**Code:**
```java
@Override
public AuthenticationResponse authenticate(AuthenticationRequest request) {
    // Verify user
    User user = ...;
    
    // NOT revoking old tokens - support multiple devices
    // refreshTokenService.revokeAllUserTokens(user); ← COMMENTED OUT
    
    // Create new token for this device
    String refreshToken = refreshTokenService.createRefreshToken(user);
    
    return response;
}
```

**Behavior:**
```
Day 1: Login on iPhone    → Token A (active)
Day 2: Login on Laptop    → Token B (active)
Day 3: Login on iPad      → Token C (active)

Result: A, B, C ALL active
```

**Pros:**
- ✅ User experience tốt
- ✅ Dùng app trên nhiều thiết bị đồng thời
- ✅ Không bị logout khi login thiết bị khác
- ✅ Standard behavior của hầu hết apps (Gmail, Facebook, etc.)

**Cons:**
- ⚠️ Nhiều tokens active → quản lý phức tạp hơn
- ⚠️ User quên logout → tokens tồn tại lâu
- ⚠️ Security risk nếu token bị đánh cắp

**Use Cases:**
- Social media apps
- Email clients
- Productivity apps
- Job recruitment platforms ✅ (recommended)

---

### Strategy 2: Single-Device

**Code:**
```java
@Override
public AuthenticationResponse authenticate(AuthenticationRequest request) {
    // Verify user
    User user = ...;
    
    // Revoke ALL old tokens - single device only
    refreshTokenService.revokeAllUserTokens(user);
    
    // Create new token
    String refreshToken = refreshTokenService.createRefreshToken(user);
    
    return response;
}
```

**Behavior:**
```
Day 1: Login on iPhone    → Token A (active)
Day 2: Login on Laptop    → Token A (revoked), Token B (active)
                             iPhone logged out!
Day 3: Login on iPad      → Token B (revoked), Token C (active)
                             Laptop logged out!

Result: Only latest device active
```

**Pros:**
- ✅ Chỉ 1 session active → quản lý đơn giản
- ✅ Security cao hơn
- ✅ Force user logout khỏi thiết bị cũ

**Cons:**
- ❌ UX kém: Login thiết bị mới → thiết bị cũ logout
- ❌ User phải re-login nhiều lần
- ❌ Không phù hợp với use case đa thiết bị

**Use Cases:**
- Banking apps (bảo mật cao)
- Admin dashboards
- Financial trading platforms
- Apps có license 1 thiết bị

---

## 🔧 How to Switch Strategy

### Switch to Single-Device:

```java
// In AuthenticationServiceImpl.java
@Override
public AuthenticationResponse authenticate(AuthenticationRequest request) {
    User user = ...;
    
    // Uncomment this line:
    refreshTokenService.revokeAllUserTokens(user);
    
    String refreshToken = refreshTokenService.createRefreshToken(user);
    return response;
}
```

### Switch to Multi-Device (Current):

```java
// In AuthenticationServiceImpl.java
@Override
public AuthenticationResponse authenticate(AuthenticationRequest request) {
    User user = ...;
    
    // Keep this commented:
    // refreshTokenService.revokeAllUserTokens(user);
    
    String refreshToken = refreshTokenService.createRefreshToken(user);
    return response;
}
```

---

## 🎨 Advanced: Hybrid Strategy (Best Practice)

### Strategy 3: Limited Multi-Device

Cho phép tối đa N thiết bị (ví dụ: 5 thiết bị).

**Implementation:**

```java
@Value("${app.max-devices-per-user}")
private int maxDevicesPerUser = 5;

@Override
@Transactional
public AuthenticationResponse authenticate(AuthenticationRequest request) {
    User user = ...;
    
    // Get active tokens count
    List<RefreshToken> activeTokens = 
        refreshTokenRepository.findAllByUsernameAndRevokedFalse(user.getEmail());
    
    // If exceeds limit, revoke oldest tokens
    if (activeTokens.size() >= maxDevicesPerUser) {
        // Sort by created_at, revoke oldest
        activeTokens.stream()
            .sorted(Comparator.comparing(RefreshToken::getCreatedAt))
            .limit(activeTokens.size() - maxDevicesPerUser + 1)
            .forEach(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
    }
    
    // Create new token
    String refreshToken = refreshTokenService.createRefreshToken(user);
    
    return response;
}
```

**Behavior:**
```
Device limit = 3

Day 1: Login on iPhone    → Token A (active)
Day 2: Login on Laptop    → Token B (active)
Day 3: Login on iPad      → Token C (active)
Day 4: Login on Desktop   → Token A (revoked), Token D (active)
                             iPhone logged out, others still active

Result: Latest 3 devices active
```

**Pros:**
- ✅ Balance giữa UX và security
- ✅ Tự động logout thiết bị cũ nhất
- ✅ Giới hạn số lượng tokens

---

## 📋 Add Device Tracking (Optional Enhancement)

### Enhanced RefreshToken Entity:

```java
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id
    private UUID id;
    
    private UUID token;
    private String username;
    private Instant expiryTime;
    private boolean revoked;
    
    // Device tracking fields
    private String deviceId;        // Unique device identifier
    private String deviceName;      // "iPhone 15 Pro"
    private String deviceType;      // "mobile", "tablet", "desktop"
    private String ipAddress;       // Login IP
    private String userAgent;       // Browser/App info
    private Instant lastUsedAt;     // Last refresh timestamp
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Database Schema Update:

```sql
ALTER TABLE refresh_token
ADD COLUMN device_id VARCHAR(255),
ADD COLUMN device_name VARCHAR(255),
ADD COLUMN device_type VARCHAR(20),
ADD COLUMN ip_address VARCHAR(45),
ADD COLUMN user_agent TEXT,
ADD COLUMN last_used_at TIMESTAMP;

CREATE INDEX idx_refresh_token_device_id ON refresh_token(device_id);
CREATE INDEX idx_refresh_token_last_used ON refresh_token(last_used_at);
```

### Updated Service:

```java
@Override
public String createRefreshToken(User user, DeviceInfo deviceInfo) {
    RefreshToken refreshToken = RefreshToken.builder()
            .username(user.getEmail())
            .expiryTime(Instant.now().plus(refreshTokenDuration, ChronoUnit.SECONDS))
            .revoked(false)
            .deviceId(deviceInfo.getDeviceId())
            .deviceName(deviceInfo.getDeviceName())
            .deviceType(deviceInfo.getDeviceType())
            .ipAddress(deviceInfo.getIpAddress())
            .userAgent(deviceInfo.getUserAgent())
            .lastUsedAt(Instant.now())
            .build();

    refreshToken = refreshTokenRepository.save(refreshToken);
    return refreshToken.getToken().toString();
}
```

---

## 🎯 Recommendation for IT Job Platform

### ✅ Recommended: Multi-Device (Current Implementation)

**Lý do:**
1. **User Experience:** Job seekers cần access từ nhiều thiết bị
   - Mobile: Xem jobs khi đi làm
   - Laptop: Apply jobs, update CV
   - Tablet: Browse companies

2. **Employer Use Case:** HR managers dùng nhiều thiết bị
   - Office desktop: Review applications
   - Mobile: Quick responses
   - Laptop: Remote work

3. **Industry Standard:** LinkedIn, Indeed, Glassdoor đều multi-device

4. **No Security Critical Operations:** 
   - Không phải banking/payment
   - Không có transaction tài chính
   - Job application data không nhạy cảm như financial data

### 💡 Future Enhancement: Add Device Management

Cho phép user quản lý sessions:

**API Endpoints:**
```
GET  /api/auth/sessions          # List all active devices
POST /api/auth/sessions/{id}/revoke  # Logout specific device
POST /api/auth/sessions/revoke-all   # Logout all except current
```

**UI:**
```
Your Active Sessions:
┌─────────────────────────────────────┐
│ 📱 iPhone 15 Pro                    │
│ Last active: 2 minutes ago          │
│ Location: Ho Chi Minh City          │
│ [Revoke] [Current Device]           │
├─────────────────────────────────────┤
│ 💻 MacBook Pro                      │
│ Last active: 1 day ago              │
│ Location: Hanoi                     │
│ [Revoke]                            │
├─────────────────────────────────────┤
│ 🖥️ Windows PC                       │
│ Last active: 3 days ago             │
│ Location: Da Nang                   │
│ [Revoke]                            │
└─────────────────────────────────────┘
[Revoke All Other Sessions]
```

---

## 📊 Comparison Table

| Feature | Single-Device | Multi-Device | Limited Multi-Device |
|---------|---------------|--------------|---------------------|
| **UX** | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Security** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Complexity** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Suitable for Job Platform** | ❌ | ✅ | ✅ |
| **Active Tokens** | 1 | Unlimited | N (configurable) |
| **Auto Logout Old Device** | ✅ Always | ❌ Never | ✅ When exceeds limit |

---

## 🔐 Security Considerations

### Multi-Device Risks:

1. **Stolen Token:** Nếu 1 token bị đánh cắp
   - Attacker có access đến account
   - Token rotation giúp giảm risk
   - Token có expiry (7 days)

2. **Forgotten Logout:** User quên logout
   - Token tồn tại cho đến khi expire
   - Solution: Short expiry time hoặc idle timeout

3. **Too Many Devices:** User login quá nhiều thiết bị
   - Solution: Limited multi-device strategy

### Mitigations:

```java
// 1. Shorter token expiry
jwt.refresh-token-duration=259200  # 3 days instead of 7

// 2. Idle timeout - revoke unused tokens
@Scheduled(cron = "0 0 0 * * *")  // Run daily
public void revokeIdleTokens() {
    Instant idleThreshold = Instant.now().minus(30, ChronoUnit.DAYS);
    
    List<RefreshToken> idleTokens = 
        refreshTokenRepository.findByLastUsedAtBeforeAndRevokedFalse(idleThreshold);
    
    idleTokens.forEach(token -> {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    });
}

// 3. Suspicious activity detection
if (request.getIpAddress() differs from token.ipAddress by country) {
    // Send email alert
    // Require re-authentication
}
```

---

## ✅ Current Implementation Summary

**Strategy:** Multi-Device (Unlimited)

**Code Location:** `AuthenticationServiceImpl.authenticate()`

**Behavior:**
- ✅ User có thể login nhiều thiết bị
- ✅ Mỗi device có token riêng
- ✅ Tất cả tokens active đồng thời
- ✅ Token rotation on refresh (per device)
- ✅ Revoke specific token on logout

**To Switch to Single-Device:**
Uncomment dòng này trong `authenticate()`:
```java
refreshTokenService.revokeAllUserTokens(user);
```

---

**Recommendation: GIỮ NGUYÊN multi-device strategy hiện tại! ✅**

Last Updated: June 27, 2026
