-- ========================================
-- MIGRATION: Align status CHECK constraints with backend enums
-- Run on existing database (no data loss)
-- Backend enums:
--   JobStatus:        open, closed, draft, expired, rejected
--   ApplicationStatus: pending, reviewing, approved, rejected, withdrawn
--   CompanyStatus:     active, pending, rejected, suspended  (unchanged)
--   ReviewStatus:      pending, approved, rejected           (unchanged)
-- ========================================

-- 1. jobs.status: add 'rejected'
ALTER TABLE jobs
    DROP CONSTRAINT IF EXISTS jobs_status_check;

ALTER TABLE jobs
    ADD CONSTRAINT jobs_status_check
    CHECK (status IN ('draft', 'open', 'closed', 'expired', 'rejected'));

-- 2. applications.status: replace old set with backend enum values
--    (old values: pending, reviewed, interview, offered, accepted, rejected, withdrawn)
ALTER TABLE applications
    DROP CONSTRAINT IF EXISTS applications_status_check;

ALTER TABLE applications
    ADD CONSTRAINT applications_status_check
    CHECK (status IN ('pending', 'reviewing', 'approved', 'rejected', 'withdrawn'));

-- NOTE: existing rows using old status values (reviewed, interview, offered, accepted)
-- must be migrated before/after this ALTER. Example:
--   UPDATE applications SET status = 'reviewing' WHERE status = 'reviewed';
--   UPDATE applications SET status = 'approved'  WHERE status IN ('interview', 'offered', 'accepted');

-- ========================================
-- MIGRATION: Add slug, is_deleted, deleted_at to blogs table
-- ========================================

ALTER TABLE blogs
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS idx_blogs_slug ON blogs(slug);

-- ========================================
-- MIGRATION: Ensure all slug columns have UNIQUE INDEX
-- ========================================

-- jobs.slug already has UNIQUE constraint from schema
CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_slug ON jobs(slug);
CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_slug ON companies(slug);

-- ========================================
-- MIGRATION: Add file_public_id to attachments (Cloudinary public_id)
-- Stored so files can be deleted from Cloudinary when a post/comment is removed
-- ========================================

ALTER TABLE attachments
    ADD COLUMN IF NOT EXISTS file_public_id VARCHAR(500);
