-- ========================================
-- ADD TEST ACCOUNTS FOR EASY TESTING
-- Password for all: Demo@123
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhkO
-- ========================================

-- Insert test users
INSERT INTO users (full_name, email, password, phone, gender, date_of_birth, avatar, cover_image, address)
VALUES 
('Admin User', 'admin@itjob.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhkO', '0900000001', 'male', '1990-01-01', 'https://i.pravatar.cc/150?img=1', 'https://picsum.photos/seed/admin/1200/400', '100 Admin Street, District 1, Ho Chi Minh City'),
('Employer User', 'employer@itjob.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhkO', '0900000002', 'female', '1992-05-15', 'https://i.pravatar.cc/150?img=2', 'https://picsum.photos/seed/employer/1200/400', '200 Employer Street, District 2, Ho Chi Minh City'),
('Regular User', 'user@itjob.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhkO', '0900000003', 'other', '1995-08-20', 'https://i.pravatar.cc/150?img=3', 'https://picsum.photos/seed/user/1200/400', '300 User Street, District 3, Ho Chi Minh City')
ON CONFLICT (email) DO NOTHING;

-- Assign ADMIN role
INSERT INTO user_roles (user_id, role_name)
SELECT u.id, 'ADMIN'
FROM users u
WHERE u.email = 'admin@itjob.com'
ON CONFLICT DO NOTHING;

-- Assign EMPLOYER role (also USER role for employers)
INSERT INTO user_roles (user_id, role_name)
SELECT u.id, 'EMPLOYER'
FROM users u
WHERE u.email = 'employer@itjob.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_name)
SELECT u.id, 'USER'
FROM users u
WHERE u.email = 'employer@itjob.com'
ON CONFLICT DO NOTHING;

-- Assign USER role
INSERT INTO user_roles (user_id, role_name)
SELECT u.id, 'USER'
FROM users u
WHERE u.email = 'user@itjob.com'
ON CONFLICT DO NOTHING;

-- Verify
SELECT 
    u.email,
    u.full_name,
    STRING_AGG(ur.role_name, ', ' ORDER BY ur.role_name) as roles
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.email LIKE '%@itjob.com'
GROUP BY u.email, u.full_name
ORDER BY u.email;
