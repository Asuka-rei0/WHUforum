ALTER TABLE posts
  ADD COLUMN treehole_expected_visibility VARCHAR(32) NULL,
  ADD COLUMN treehole_review_status VARCHAR(32) NULL;

CREATE INDEX idx_posts_type_treehole_review_status
  ON posts (type, treehole_review_status);
