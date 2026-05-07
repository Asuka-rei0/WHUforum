-- Add logical delete support for comments and point_histories tables

-- Add deleted_at column to comments table
ALTER TABLE comments ADD COLUMN deleted_at DATETIME(6) NULL;

-- Add deleted_at column to point_histories table  
ALTER TABLE point_histories ADD COLUMN deleted_at DATETIME(6) NULL;

-- Add index for better performance on logical delete queries
CREATE INDEX idx_comments_deleted_at ON comments(deleted_at);
CREATE INDEX idx_point_histories_deleted_at ON point_histories(deleted_at);
