-- ========================================
-- SEED DATA FOR ITJOB DATABASE (PostgreSQL)
-- ========================================

-- ========================================
-- 1. INSERT ROLES (OAuth2: WHO the user is)
-- ========================================
INSERT INTO roles (name, description)
VALUES 
('USER', 'Regular user - Job seeker'),
('EMPLOYER', 'Employer - Can post jobs and manage company'),
('ADMIN', 'System administrator - Full access')
ON CONFLICT (name) DO NOTHING;

-- ========================================
-- 2. INSERT PERMISSIONS (WHAT they can access + HOW)
-- ========================================
INSERT INTO permissions (name, description)
VALUES 
-- General permissions (no specific resource)
('read', 'Read access to all resources'),
('write', 'Write access to all resources'),
('admin', 'Full administrative access'),

-- Job permissions
('read:jobs', 'Read job listings'),
('write:jobs', 'Create and update job listings'),
('delete:jobs', 'Delete job listings'),
('admin:jobs', 'Full job management including approval'),

-- Company permissions
('read:companies', 'Read company information'),
('write:companies', 'Create and update company information'),
('delete:companies', 'Delete companies'),
('admin:companies', 'Full company management including verification'),

-- Application permissions
('read:applications', 'Read job applications'),
('write:applications', 'Submit and manage applications'),
('delete:applications', 'Delete applications'),

-- User permissions
('read:users', 'Read user profiles'),
('write:users', 'Update user profiles'),
('delete:users', 'Delete user accounts'),
('admin:users', 'Full user management'),

-- Review permissions
('read:reviews', 'Read company reviews'),
('write:reviews', 'Write and update reviews'),
('delete:reviews', 'Delete reviews'),
('admin:reviews', 'Moderate reviews')
ON CONFLICT (name) DO NOTHING;

-- ========================================
-- 3. ASSIGN PERMISSIONS TO ROLES
-- ========================================

-- USER permissions
INSERT INTO role_permissions (role_name, permission_name)
VALUES 
('USER', 'read'),
('USER', 'read:jobs'),
('USER', 'read:companies'),
('USER', 'read:applications'),
('USER', 'read:reviews'),
('USER', 'write:applications'),
('USER', 'write:users'),
('USER', 'write:reviews');

-- EMPLOYER permissions
INSERT INTO role_permissions (role_name, permission_name)
VALUES 
('EMPLOYER', 'read'),
('EMPLOYER', 'read:jobs'),
('EMPLOYER', 'read:companies'),
('EMPLOYER', 'read:applications'),
('EMPLOYER', 'read:users'),
('EMPLOYER', 'write:jobs'),
('EMPLOYER', 'write:companies'),
('EMPLOYER', 'write:applications'),
('EMPLOYER', 'write:users'),
('EMPLOYER', 'delete:jobs');

-- ADMIN permissions (all)
INSERT INTO role_permissions (role_name, permission_name)
SELECT 'ADMIN', name FROM permissions;

-- ========================================
-- 4. INSERT USERS
-- ========================================
INSERT INTO users (full_name, email, password, phone, gender, date_of_birth, avatar, cover_image, cv_url, address)
VALUES
('Nguyễn Minh Huy', 'hr.fpt@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000001', 'male', '1999-04-12', 'https://i.pravatar.cc/150?img=11', 'https://picsum.photos/seed/u11/1200/400', 'https://drive.google.com/file/d/cv_nguyen_minh_huy.pdf', '123 Nguyễn Văn Linh, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh'),
('Trần Thảo Vy', 'hr.vng@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000002', 'female', '2000-08-21', 'https://i.pravatar.cc/150?img=12', 'https://picsum.photos/seed/u12/1200/400', 'https://drive.google.com/file/d/cv_tran_thao_vy.pdf', '456 Lê Văn Việt, Phường Tăng Nhơn Phú A, Quận 9, TP. Hồ Chí Minh'),
('Lê Quốc Bảo', 'hr.tiki@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000003', 'male', '1998-01-05', 'https://i.pravatar.cc/150?img=13', 'https://picsum.photos/seed/u13/1200/400', 'https://drive.google.com/file/d/cv_le_quoc_bao.pdf', '789 Cộng Hòa, Phường 13, Quận Tân Bình, TP. Hồ Chí Minh'),
('Phạm Ngọc Anh', 'hr.momo@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000004', 'female', '1997-11-30', 'https://i.pravatar.cc/150?img=14', 'https://picsum.photos/seed/u14/1200/400', 'https://drive.google.com/file/d/cv_pham_ngoc_anh.pdf', '321 Hoàng Văn Thái, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh'),
('Võ Hoàng Long', 'hr.base@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000005', 'male', '1996-06-18', 'https://i.pravatar.cc/150?img=15', 'https://picsum.photos/seed/u15/1200/400', 'https://drive.google.com/file/d/cv_vo_hoang_long.pdf', '654 Võ Văn Ngân, Phường Linh Chiểu, Thành phố Thủ Đức, TP. Hồ Chí Minh'),
('Đặng Thu Trang', 'hr.grab@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000006', 'female', '1999-02-14', 'https://i.pravatar.cc/150?img=16', 'https://picsum.photos/seed/u16/1200/400', 'https://drive.google.com/file/d/cv_dang_thu_trang.pdf', '987 Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh'),
('Nguyễn Gia Hân', 'hr.shopee@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000007', 'other', '2001-09-09', 'https://i.pravatar.cc/150?img=17', 'https://picsum.photos/seed/u17/1200/400', 'https://drive.google.com/file/d/cv_nguyen_gia_han.pdf', '147 Pasteur, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh'),
('Tạ Văn Sơn', 'hr.tcb@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000008', 'male', '1995-12-02', 'https://i.pravatar.cc/150?img=18', 'https://picsum.photos/seed/u18/1200/400', 'https://drive.google.com/file/d/cv_ta_van_son.pdf', '258 Trần Hưng Đạo, Phường Nguyễn Cư Trinh, Quận 1, TP. Hồ Chí Minh'),
('Bùi Mỹ Linh', 'hr.viettel@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000009', 'female', '1998-03-25', 'https://i.pravatar.cc/150?img=19', 'https://picsum.photos/seed/u19/1200/400', 'https://drive.google.com/file/d/cv_bui_my_linh.pdf', '369 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh'),
('Admin System', 'admin@demo.com', '$2a$12$XRcYOwPoThhwZlCGE1XPouOQCSQvHQQxTWQEtJd.WJ4wXPdGQoEk6', '0901000010', 'male', '1990-01-01', 'https://i.pravatar.cc/150?img=20', 'https://picsum.photos/seed/u20/1200/400', 'https://drive.google.com/file/d/cv_admin_system.pdf', '100 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh');

-- ========================================
-- 6. ASSIGN ROLES TO USERS
-- ========================================

-- Admin user (last user)
INSERT INTO user_roles (user_id, role_name)
SELECT u.id, 'ADMIN'
FROM users u
WHERE u.email = 'admin@demo.com';

-- Employer users (first 9 users) - they have both EMPLOYER and USER roles
INSERT INTO user_roles (user_id, role_name)
SELECT u.id, 'EMPLOYER'
FROM users u
WHERE u.email LIKE 'hr.%@demo.com';

INSERT INTO user_roles (user_id, role_name)
SELECT u.id, 'USER'
FROM users u
WHERE u.email LIKE 'hr.%@demo.com';

-- ========================================
-- 7. INSERT COMPANIES
-- ========================================
INSERT INTO companies (name, slug, email, phone, company_size, industry, nationality, founded_year, work_modes, employment_types, avatar, cover_image, website, description, benefits, address, status, verified_at, created_by_user_id)
SELECT 
    'FPT Software',
    'fpt-software',
    'contact@fptsoftware.com',
    '0283730-3838',
    '1000+',
    'Information Technology',
    'Việt Nam',
    1999,
    '["on-site", "remote", "hybrid"]',
    '["full-time", "part-time", "contract"]',
    'https://res.cloudinary.com/dumprllvt/image/upload/v1765443034/FPT_Software_Logo_nqovmm.png',
    'https://res.cloudinary.com/dumprllvt/image/upload/v1765444439/fpt-cover_to3i1t.jpg',
    'https://fptsoftware.com',
    'Công ty phần mềm hàng đầu Việt Nam, chuyên cung cấp dịch vụ công nghệ thông tin và giải pháp chuyển đổi số.',
    'Bảo hiểm sức khỏe, Thưởng hiệu suất, Du lịch hàng năm, Đào tạo nâng cao kỹ năng',
    'Tòa nhà FPT, Lô L29B-31B-33B, Đường Tân Thuận, Khu Chế Xuất Tân Thuận, Phường Tân Thuận Đông, Quận 7, TP. Hồ Chí Minh',
    'active',
    TIMESTAMP '2020-01-01 00:00:00',
    (SELECT id FROM users WHERE email = 'hr.fpt@demo.com');

INSERT INTO companies (name, slug, email, phone, company_size, industry, nationality, founded_year, work_modes, employment_types, avatar, cover_image, website, description, benefits, address, status, verified_at, created_by_user_id)
SELECT 
    'VNG Corporation',
    'vng-corporation',
    'hr@vng.com.vn',
    '0283997-8888',
    '1000+',
    'Internet & Technology',
    'Việt Nam',
    2004,
    '["on-site", "hybrid"]',
    '["full-time"]',
    'https://res.cloudinary.com/dumprllvt/image/upload/v1765443033/VNG_Corp._logo.svg_v0pxg4.png',
    'https://res.cloudinary.com/dumprllvt/image/upload/v1765444439/vng-cover_yvzdvc.jpg',
    'https://vng.com.vn',
    'Tập đoàn công nghệ internet hàng đầu Việt Nam với các sản phẩm như Zalo, ZaloPay, 123Go.',
    'Lương thưởng cạnh tranh, Môi trường sáng tạo, Team building định kỳ, Chế độ nghỉ phép linh hoạt',
    'Z06 Đường số 13, Khu phố 3, Phường Tân Thuận Đông, Quận 7, TP. Hồ Chí Minh',
    'active',
    TIMESTAMP '2020-02-15 00:00:00',
    (SELECT id FROM users WHERE email = 'hr.vng@demo.com');

INSERT INTO companies (name, slug, email, phone, company_size, industry, nationality, founded_year, work_modes, employment_types, avatar, cover_image, website, description, benefits, address, status, verified_at, created_by_user_id)
SELECT 
    'Tiki Corporation',
    'tiki-corporation',
    'careers@tiki.vn',
    '1900-6035',
    '501-1000',
    'E-commerce',
    'Việt Nam',
    2010,
    '["on-site"]',
    '["full-time", "part-time"]',
    'https://res.cloudinary.com/dumprllvt/image/upload/v1765443033/Logo_Tiki_zstbqk.png',
    'https://res.cloudinary.com/dumprllvt/image/upload/v1765444439/tiki-cover_keknu7.jpg',
    'https://tiki.vn',
    'Sàn thương mại điện tử lớn nhất Việt Nam, cung cấp dịch vụ mua sắm trực tuyến đa dạng.',
    'Bảo hiểm đầy đủ, Thưởng KPI, Giảm giá mua hàng, Văn phòng hiện đại',
    '52 Út Tịch, Phường 4, Quận Tân Bình, TP. Hồ Chí Minh',
    'active',
    TIMESTAMP '2019-06-01 00:00:00',
    (SELECT id FROM users WHERE email = 'hr.tiki@demo.com');

INSERT INTO companies (name, slug, email, phone, company_size, industry, nationality, founded_year, work_modes, employment_types, avatar, cover_image, website, description, benefits, address, status, verified_at, created_by_user_id)
SELECT 
    'Momo Technology',
    'momo-technology',
    'recruitment@momo.vn',
    '1900-545-436',
    '201-500',
    'Fintech',
    'Việt Nam',
    2007,
    '["on-site", "remote"]',
    '["full-time", "internship"]',
    'https://res.cloudinary.com/dumprllvt/image/upload/v1765443033/MoMo-Symbol_qz1msk.png',
    'https://res.cloudinary.com/dumprllvt/image/upload/v1765444438/momo-cover_qp6fsv.webp',
    'https://momo.vn',
    'Ví điện tử và nền tảng thanh toán di động hàng đầu tại Việt Nam.',
    'Lương cao, Cổ phiếu công ty, Bảo hiểm cao cấp, Làm việc linh hoạt',
    'Lầu 6, Tòa nhà Phú Mỹ Hưng Tower, 08 Hoàng Văn Thái, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh',
    'active',
    TIMESTAMP '2021-01-10 00:00:00',
    (SELECT id FROM users WHERE email = 'hr.momo@demo.com');

-- ========================================
-- 7. INSERT COMPANY MEMBERS
-- ========================================
INSERT INTO company_members (company_id, user_id, status, joined_at)
SELECT 
    c.id,
    u.id,
    'active',
    TIMESTAMP '2020-01-15 09:00:00'
FROM companies c
CROSS JOIN users u
WHERE c.name = 'FPT Software'
AND u.email = 'hr.fpt@demo.com';

INSERT INTO company_members (company_id, user_id, status, joined_at)
SELECT 
    c.id,
    u.id,
    'active',
    TIMESTAMP '2020-03-20 10:30:00'
FROM companies c
CROSS JOIN users u
WHERE c.name = 'VNG Corporation'
AND u.email = 'hr.vng@demo.com';

INSERT INTO company_members (company_id, user_id, status, joined_at)
SELECT 
    c.id,
    u.id,
    'active',
    TIMESTAMP '2019-06-10 08:45:00'
FROM companies c
CROSS JOIN users u
WHERE c.name = 'Tiki Corporation'
AND u.email = 'hr.tiki@demo.com';

INSERT INTO company_members (company_id, user_id, status, joined_at)
SELECT 
    c.id,
    u.id,
    'active',
    TIMESTAMP '2021-02-01 09:15:00'
FROM companies c
CROSS JOIN users u
WHERE c.name = 'Momo Technology'
AND u.email = 'hr.momo@demo.com';

-- ========================================
-- 10. INSERT JOBS
-- ========================================
INSERT INTO jobs (company_id, title, slug, description, type, level, experience, quantity, salary_min, salary_max, salary_currency, salary_type, is_negotiable, work_location, benefits, requirements, deadline, status, created_by)
SELECT 
    c.id,
    'Senior Frontend Developer (ReactJS)',
    'senior-frontend-developer-reactjs-fpt',
    'Phát triển ứng dụng web với ReactJS, NextJS. Làm việc trong môi trường Agile, tham gia các dự án quốc tế.',
    'full-time',
    'senior',
    '3-5 years',
    2,
    30000000,
    50000000,
    'VND',
    'monthly',
    TRUE,
    'Tòa nhà FPT, Lô L29B-31B-33B, Đường Tân Thuận, Khu Chế Xuất Tân Thuận, Phường Tân Thuận Đông, Quận 7, TP. Hồ Chí Minh',
    'Lương tháng 13, Thưởng dự án, Review lương 2 lần/năm',
    'Thành thạo ReactJS, NextJS, TypeScript, Redux. Kinh nghiệm làm việc với RESTful API. Có khả năng làm việc nhóm tốt.',
    DATE '2025-12-31',
    'open',
    (SELECT id FROM users WHERE email = 'hr.fpt@demo.com')
FROM companies c WHERE c.name = 'FPT Software';

INSERT INTO jobs (company_id, title, slug, description, type, level, experience, quantity, salary_min, salary_max, salary_currency, salary_type, is_negotiable, work_location, benefits, requirements, deadline, status, created_by)
SELECT 
    c.id,
    'Backend Developer (Java Spring Boot)',
    'backend-developer-java-spring-boot-vng',
    'Xây dựng API RESTful với Spring Boot, MySQL. Tham gia phát triển các sản phẩm như Zalo, ZaloPay.',
    'full-time',
    'middle',
    '2-3 years',
    3,
    25000000,
    40000000,
    'VND',
    'monthly',
    TRUE,
    'Z06 Đường số 13, Khu phố 3, Phường Tân Thuận Đông, Quận 7, TP. Hồ Chí Minh',
    'Bảo hiểm cao cấp, Laptop MacBook Pro, Cơ hội thăng tiến',
    'Kinh nghiệm Java Spring Boot 2+ năm. Hiểu biết về Microservices, Docker. Có kinh nghiệm với MySQL, Redis.',
    DATE '2025-11-30',
    'open',
    (SELECT id FROM users WHERE email = 'hr.vng@demo.com')
FROM companies c WHERE c.name = 'VNG Corporation';

INSERT INTO jobs (company_id, title, slug, description, type, level, experience, quantity, salary_min, salary_max, salary_currency, salary_type, is_negotiable, work_location, benefits, requirements, deadline, status, created_by)
SELECT 
    c.id,
    'DevOps Engineer',
    'devops-engineer-tiki',
    'Quản lý hạ tầng AWS/Azure, CI/CD pipeline. Đảm bảo hệ thống hoạt động ổn định 24/7.',
    'full-time',
    'senior',
    '3-5 years',
    1,
    35000000,
    55000000,
    'VND',
    'monthly',
    FALSE,
    '52 Út Tịch, Phường 4, Quận Tân Bình, TP. Hồ Chí Minh',
    'Lương cạnh tranh, Thưởng hiệu suất cao, Môi trường năng động',
    'Kinh nghiệm Docker, Kubernetes, Jenkins. Thành thạo AWS hoặc Azure. Có khả năng scripting (Bash, Python).',
    DATE '2025-12-15',
    'open',
    (SELECT id FROM users WHERE email = 'hr.tiki@demo.com')
FROM companies c WHERE c.name = 'Tiki Corporation';

INSERT INTO jobs (company_id, title, slug, description, type, level, experience, quantity, salary_min, salary_max, salary_currency, salary_type, is_negotiable, work_location, benefits, requirements, deadline, status, created_by)
SELECT 
    c.id,
    'Mobile Developer (Flutter)',
    'mobile-developer-flutter-momo',
    'Phát triển ứng dụng di động đa nền tảng với Flutter. Tham gia xây dựng ứng dụng MoMo với hàng triệu người dùng.',
    'full-time',
    'junior',
    '1-2 years',
    2,
    18000000,
    30000000,
    'VND',
    'monthly',
    TRUE,
    'Lầu 6, Tòa nhà Phú Mỹ Hưng Tower, 08 Hoàng Văn Thái, Phường Tân Phú, Quận 7, TP. Hồ Chí Minh',
    'Cổ phiếu ESOP, Bảo hiểm PVI, Nghỉ phép 15 ngày/năm',
    'Kinh nghiệm Flutter/Dart 1+ năm. Hiểu biết về State Management (Provider, Bloc). Có khả năng làm việc với RESTful API.',
    DATE '2025-11-25',
    'open',
    (SELECT id FROM users WHERE email = 'hr.momo@demo.com')
FROM companies c WHERE c.name = 'Momo Technology';

-- Remote job example
INSERT INTO jobs (company_id, title, slug, description, type, level, experience, quantity, salary_min, salary_max, salary_currency, salary_type, is_negotiable, work_location, benefits, requirements, deadline, status, created_by)
SELECT 
    c.id,
    'Full Stack Developer (Remote)',
    'full-stack-developer-remote-fpt',
    'Phát triển full-stack với React + Node.js. Làm việc remote 100%, flexible working hours.',
    'full-time',
    'middle',
    '2-4 years',
    3,
    25000000,
    45000000,
    'VND',
    'monthly',
    TRUE,
    'Remote (Làm việc từ xa)',
    'Làm việc remote, Flexible hours, Laptop + Monitor, Thưởng hiệu suất',
    'Kinh nghiệm React, Node.js, MongoDB. Có khả năng tự quản lý công việc. Giao tiếp tiếng Anh tốt.',
    DATE '2025-12-20',
    'open',
    (SELECT id FROM users WHERE email = 'hr.fpt@demo.com')
FROM companies c WHERE c.name = 'FPT Software';

-- ========================================
-- 11. INSERT SKILLS
-- ========================================
INSERT INTO skills (name)
VALUES 
('ReactJS'),
('Java Spring Boot'),
('Python'),
('Docker'),
('AWS'),
('Flutter'),
('Figma'),
('SQL'),
('Machine Learning'),
('Agile/Scrum');

-- ========================================
-- 12. INSERT BLOG CATEGORIES
-- ========================================
INSERT INTO blog_categories (name)
VALUES 
('Phỏng vấn'),
('Học tập'),
('Tìm việc'),
('Nghề nghiệp');

-- ========================================
-- 13. INSERT REVIEWS
-- ========================================
INSERT INTO reviews (user_id, company_id, rating, salary_rating, culture_rating, management_rating, work_life_balance_rating, title, pros, cons, advice, is_verified_employee, work_position, work_duration, status, is_anonymous)
SELECT 
    (SELECT id FROM users WHERE email = 'hr.vng@demo.com'),
    (SELECT id FROM companies WHERE name = 'FPT Software'),
    5,
    4,
    5,
    5,
    4,
    'Môi trường làm việc tuyệt vời',
    'Lương thưởng tốt, đồng nghiệp thân thiện, nhiều cơ hội học hỏi',
    'Áp lực công việc cao vào cuối dự án',
    'Nên cải thiện work-life balance cho nhân viên',
    TRUE,
    'Senior Developer',
    '2-3 years',
    'approved',
    FALSE;

INSERT INTO reviews (user_id, company_id, rating, salary_rating, culture_rating, management_rating, work_life_balance_rating, title, pros, cons, advice, is_verified_employee, work_position, work_duration, status, is_anonymous)
SELECT 
    (SELECT id FROM users WHERE email = 'hr.tiki@demo.com'),
    (SELECT id FROM companies WHERE name = 'VNG Corporation'),
    4,
    5,
    4,
    4,
    3,
    'Công ty công nghệ hàng đầu',
    'Lương cao, sản phẩm có tầm ảnh hưởng lớn, văn hóa sáng tạo',
    'Làm việc nhiều giờ, đôi khi phải OT',
    'Cần có thêm hoạt động team building',
    TRUE,
    'Backend Engineer',
    '1-2 years',
    'approved',
    FALSE;

-- ========================================
-- 14. INSERT COMPANY IMAGES
-- ========================================
INSERT INTO company_images (company_id, image_url, image_type, caption, display_order)
SELECT 
    id,
    'https://picsum.photos/seed/fpt-office1/800/600',
    'office',
    'Văn phòng làm việc hiện đại',
    1
FROM companies WHERE name = 'FPT Software';

INSERT INTO company_images (company_id, image_url, image_type, caption, display_order)
SELECT 
    id,
    'https://picsum.photos/seed/fpt-team1/800/600',
    'team',
    'Đội ngũ nhân viên trẻ trung, năng động',
    2
FROM companies WHERE name = 'FPT Software';

INSERT INTO company_images (company_id, image_url, image_type, caption, display_order)
SELECT 
    id,
    'https://picsum.photos/seed/vng-office1/800/600',
    'office',
    'Không gian làm việc sáng tạo',
    1
FROM companies WHERE name = 'VNG Corporation';

-- ========================================
-- 15. INSERT SAVED JOBS
-- ========================================
INSERT INTO saved_jobs (user_id, job_id)
SELECT 
    u.id,
    j.id
FROM users u
CROSS JOIN jobs j
WHERE u.email = 'hr.vng@demo.com'
AND j.title = 'Senior Frontend Developer (ReactJS)'
LIMIT 1;

-- ========================================
-- 16. INSERT NOTIFICATIONS
-- ========================================
INSERT INTO notifications (user_id, type, title, message, related_job_id, is_read, action_url)
SELECT 
    u.id,
    'new_job',
    'Công việc mới phù hợp với bạn',
    'FPT Software đang tuyển dụng Senior Frontend Developer (ReactJS)',
    j.id,
    FALSE,
    '/jobs/' || j.slug
FROM users u
CROSS JOIN jobs j
WHERE u.email = 'hr.vng@demo.com'
AND j.title = 'Senior Frontend Developer (ReactJS)'
LIMIT 1;

INSERT INTO notifications (user_id, type, title, message, related_company_id, is_read, action_url)
SELECT 
    u.id,
    'company_update',
    'Công ty bạn theo dõi có cập nhật mới',
    'VNG Corporation vừa đăng 3 vị trí tuyển dụng mới',
    c.id,
    FALSE,
    '/companies/' || c.slug
FROM users u
CROSS JOIN companies c
WHERE u.email = 'hr.fpt@demo.com'
AND c.name = 'VNG Corporation'
LIMIT 1;

-- ========================================
-- SUCCESS MESSAGE
-- ========================================
DO $$
BEGIN
    RAISE NOTICE 'Seed data inserted successfully!';
    RAISE NOTICE 'Total users: %', (SELECT COUNT(*) FROM users);
    RAISE NOTICE 'Total companies: %', (SELECT COUNT(*) FROM companies);
    RAISE NOTICE 'Total jobs: %', (SELECT COUNT(*) FROM jobs);
    RAISE NOTICE 'Total roles: %', (SELECT COUNT(*) FROM roles);
    RAISE NOTICE 'Total permissions: %', (SELECT COUNT(*) FROM permissions);
END $$;
