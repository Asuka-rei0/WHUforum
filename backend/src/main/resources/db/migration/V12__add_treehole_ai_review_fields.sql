ALTER TABLE posts
  ADD COLUMN treehole_risk_level VARCHAR(8) NULL,
  ADD COLUMN treehole_risk_reason VARCHAR(1000) NULL,
  ADD COLUMN treehole_recommended_action VARCHAR(1000) NULL,
  ADD COLUMN treehole_reviewed_at DATETIME(6) NULL;

CREATE INDEX idx_posts_treehole_risk_level ON posts (treehole_risk_level);
