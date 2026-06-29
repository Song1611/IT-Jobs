# 🔄 Refresh Token Rotation - Implementation Explained

## ⚠️ Vấn Đề Trước Đây

### ❌ Implementation Cũ (SAI):

```java
@Override
public String createRefreshToken(User user) {
    // Revoke ALL tokens ở đây
    revokeAllUserTokens(user);
    
    // Tạo token mới
    RefreshToken token = ...
    return token;
}

@Override
public AuthenticationResponse refreshToken(RefreshRequest request) {
    verifyRefreshToken(request.getRefreshToken());
    
    // Gọi createRefreshToken → revoke ALL
    // Nhưng chưa revoke token cụ thể đang dùng!
    String newToken = createRefreshToken(user);
    
    return response;
}
```

### 🐛 Bug Là Gì?

**Scenario:**
1. User login → Token A được tạo
2. Refresh với Token A → Token B được tạo
3. **Token A VẪN VALID** (chưa bị revoke trước khi tạo B)
4. Có thể refresh với Token A lần nữa → Token C

**Kết quả:** Multiple active tokens → Không phải token rotation thực sự!

---

## ✅ Implementation Đúng

### 📋 Flow Đúng:

```
Login:
  1. Verify username/password
  2. Revoke ALL old tokens của user
  3. Create new refresh token
  4. Return JWT + Refresh Token

Refresh:
  1. Verify refresh token (check valid, not revoked, not expired)
  2. Revoke CHÍNH refresh token vừa dùng
  3. Create new refresh token
  4. Return new JWT + new Refresh Token

Logout:
  1. Revoke refresh token được gửi lên
```

---

## 🎯 Code Implementation

### 1. **RefreshTokenService.createRefreshToken()**

```java
@Override
@Transactional
public String createRefreshToken(User user) {
    log.debug("Creating refresh token for user: {}", user.getEmail());
    
    // KHÔNG revoke ở đây - để caller quyết định
    RefreshToken refreshToken = RefreshToken.builder()
            .username(user.getEmail())
            .expiryTime(Instant.now().plus(refreshTokenDuration, ChronoUnit.SECONDS))
            .revoked(false)
            .build();

    refreshToken = refreshTokenRepository.save(refreshToken);
    
    log.debug("Refresh token created successfully");
    return refreshToken.getToken().toString();
}
```

**✅ Lý do:** Method này chỉ tạo token, không quyết định revoke logic.

---

### 2. **AuthenticationService.authenticate() - LOGIN**

```java
@Override
@Transactional
public AuthenticationResponse authenticate(AuthenticationRequest request) {
    log.debug("Authenticating user: {}", request.getUsername());
    
    User user = userRepository.findByEmail(request.getUsername())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    // Verify password
    boolean matched = passwordEncoder.matches(request.getPassword(), user.getPassword());
    if (!matched) {
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    // ✅ REVOKE ALL old tokens when user logs in
    refreshTokenService.revokeAllUserTokens(user);
    
    // Create new tokens
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = refreshTokenService.createRefreshToken(user);

    log.debug("User authenticated successfully");
    
    return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .authenticated(true)
            .build();
}
```

**✅ Lý do:** Khi login, revoke tất cả tokens cũ để chỉ có 1 session active.

---

### 3. **AuthenticationService.refreshToken() - REFRESH** 🔑

```java
@Override
@Transactional
public AuthenticationResponse refreshToken(RefreshRequest request) {
    log.debug("Refreshing access token");
    
    // 1. Verify refresh token
    RefreshToken refreshToken = 
        refreshTokenService.verifyRefreshToken(request.getRefreshToken());
    
    // 2. Get user
    User user = userRepository.findByEmail(refreshToken.getUsername())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    // 3. ✅ REVOKE old refresh token FIRST (QUAN TRỌNG!)
    refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    
    // 4. Generate new access token
    String accessToken = jwtService.generateAccessToken(user);
    
    // 5. Create new refresh token
    String newRefreshToken = refreshTokenService.createRefreshToken(user);
    
    log.debug("Token refreshed successfully");
    
    return AuthenticationResponse.builder()
            .accessToken(accessToken)
            .refreshToken(newRefreshToken)
            .authenticated(true)
            .build();
}
```

**✅ Lý do:** 
- Revoke token cũ TRƯỚC khi tạo token mới
- Token cũ không thể reuse
- True rotation: 1 token in → 1 token out, old token invalid

---

### 4. **AuthenticationService.logout() - LOGOUT**

```java
@Override
@Transactional
public void logout(LogoutRequest request) {
    log.debug("Logging out user");
    
    // Revoke the refresh token
    refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    
    log.debug("User logged out successfully");
}
```

---

## 🔍 Database State Examples

### Scenario 1: Login

**Before:**
```sql
-- User có 2 tokens cũ từ sessions trước
user_id | token_a | revoked=false
user_id | token_b | revoked=false
```

**After Login:**
```sql
-- Tokens cũ bị revoke
user_id | token_a | revoked=true   ✅
user_id | token_b | revoked=true   ✅

-- Token mới được tạo
user_id | token_c | revoked=false  ✅ (only active token)
```

---

### Scenario 2: Refresh Token

**Before:**
```sql
user_id | token_c | revoked=false
```

**Request:** `POST /refresh` with `token_c`

**Process:**
1. Verify `token_c` → OK
2. Revoke `token_c` → `revoked=true`
3. Create `token_d`

**After:**
```sql
user_id | token_c | revoked=true   ✅ (can't reuse)
user_id | token_d | revoked=false  ✅ (new active token)
```

**❌ Nếu thử refresh lại với `token_c`:**
```
verifyRefreshToken(token_c)
  → check revoked = true
  → throw UNAUTHENTICATED
```

---

## 🛡️ Security Benefits

### 1. **Prevent Token Replay**
- Token cũ không thể reuse sau khi refresh
- Nếu attacker đánh cắp token cũ → không dùng được

### 2. **Detect Token Theft**
- Nếu refresh với revoked token → có thể có người khác đã dùng token
- Server có thể revoke ALL tokens của user (force re-login)

### 3. **Single Active Session** (Optional)
- Login mới → revoke tất cả tokens cũ
- User chỉ có 1 session active tại 1 thời điểm

---

## 📊 Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         LOGIN                                │
└─────────────────────────────────────────────────────────────┘
  │
  ├─ Verify username/password
  │
  ├─ Revoke ALL old tokens
  │     UPDATE refresh_token 
  │     SET revoked = true 
  │     WHERE username = 'user@email.com' 
  │       AND revoked = false
  │
  ├─ Create new refresh token (Token A)
  │     INSERT INTO refresh_token (token, username, ...)
  │
  └─ Return JWT + Token A


┌─────────────────────────────────────────────────────────────┐
│                        REFRESH                               │
└─────────────────────────────────────────────────────────────┘
  │
  ├─ Verify Token A
  │     SELECT * FROM refresh_token WHERE token = 'Token A'
  │     Check: revoked = false, expiry_time > now()
  │
  ├─ Revoke Token A
  │     UPDATE refresh_token 
  │     SET revoked = true 
  │     WHERE token = 'Token A'
  │
  ├─ Create new Token B
  │     INSERT INTO refresh_token (token, username, ...)
  │
  └─ Return new JWT + Token B


┌─────────────────────────────────────────────────────────────┐
│                        LOGOUT                                │
└─────────────────────────────────────────────────────────────┘
  │
  ├─ Revoke Token B
  │     UPDATE refresh_token 
  │     SET revoked = true 
  │     WHERE token = 'Token B'
  │
  └─ Success
```

---

## 🧪 Testing Token Rotation

### Test 1: Normal Flow

```bash
# 1. Login
curl -X POST /api/auth/login -d '{"username":"admin@itjob.com","password":"Demo@123"}'
# Response: { accessToken: "jwt1", refreshToken: "token_a" }

# 2. Refresh with token_a
curl -X POST /api/auth/refresh -d '{"refreshToken":"token_a"}'
# Response: { accessToken: "jwt2", refreshToken: "token_b" }

# 3. Try refresh with token_a again (should FAIL)
curl -X POST /api/auth/refresh -d '{"refreshToken":"token_a"}'
# Response: 401 UNAUTHENTICATED (token_a revoked)

# 4. Refresh with token_b (should SUCCEED)
curl -X POST /api/auth/refresh -d '{"refreshToken":"token_b"}'
# Response: { accessToken: "jwt3", refreshToken: "token_c" }
```

### Test 2: Check Database

```sql
-- After each step above, check:
SELECT token, revoked, created_at 
FROM refresh_token 
WHERE username = 'admin@itjob.com'
ORDER BY created_at DESC;

-- Should see:
-- token_c | false | (latest)
-- token_b | true  | (revoked after step 4)
-- token_a | true  | (revoked after step 2)
```

---

## 💡 Advanced: Detect Token Theft

### Implementation (Optional):

```java
@Override
public RefreshToken verifyRefreshToken(String token) {
    UUID uuid = UUID.fromString(token);
    
    RefreshToken refreshToken = refreshTokenRepository.findByToken(uuid)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

    if (refreshToken.isRevoked()) {
        // ⚠️ Someone tried to use revoked token
        // This might indicate token theft!
        
        log.error("⚠️ Attempted reuse of revoked token: {} for user: {}", 
                  token, refreshToken.getUsername());
        
        // Optional: Revoke ALL tokens for this user (force re-login)
        User user = userRepository.findByEmail(refreshToken.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        revokeAllUserTokens(user);
        
        // Optional: Send security alert email to user
        
        throw new AppException(ErrorCode.TOKEN_THEFT_DETECTED);
    }
    
    // Continue with normal validation...
}
```

---

## ✅ Checklist

- [x] `createRefreshToken()` không tự revoke tokens
- [x] `authenticate()` revoke ALL old tokens trước khi tạo mới
- [x] `refreshToken()` revoke token cũ TRƯỚC khi tạo mới
- [x] `logout()` revoke token khi logout
- [x] `verifyRefreshToken()` check revoked status
- [x] Token rotation: 1 in → 1 out, old invalid
- [x] Database có đúng 1 active token per user (sau login)
- [x] Không thể reuse revoked token

---

## 🎯 Summary

### ✅ Đúng:
```
Refresh với Token A:
  1. Verify Token A
  2. Revoke Token A        ← QUAN TRỌNG
  3. Create Token B
  4. Token A → invalid
  5. Token B → active
```

### ❌ Sai:
```
Refresh với Token A:
  1. Verify Token A
  2. Create Token B
  3. Token A → still valid  ← SAI!
  4. Token B → active
  (Both tokens valid = NOT rotation)
```

---

**🔒 True Token Rotation = Old token must be revoked BEFORE creating new token!**

Last Updated: June 27, 2026
