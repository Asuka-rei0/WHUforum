CREATE TABLE treehole_intervention_cases (
  id BIGINT NOT NULL AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  note VARCHAR(1000) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  closed_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_treehole_cases_post (post_id)
);

CREATE INDEX idx_treehole_cases_post ON treehole_intervention_cases (post_id);
CREATE INDEX idx_treehole_cases_status ON treehole_intervention_cases (status);

CREATE TABLE treehole_intervention_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  case_id BIGINT NOT NULL,
  post_id BIGINT NOT NULL,
  admin_id BIGINT NULL,
  action VARCHAR(40) NOT NULL,
  note VARCHAR(1000) NULL,
  from_status VARCHAR(32) NULL,
  to_status VARCHAR(32) NULL,
  from_review_status VARCHAR(32) NULL,
  to_review_status VARCHAR(32) NULL,
  from_post_status VARCHAR(32) NULL,
  to_post_status VARCHAR(32) NULL,
  from_visible_scope VARCHAR(32) NULL,
  to_visible_scope VARCHAR(32) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id)
);

CREATE INDEX idx_treehole_records_case ON treehole_intervention_records (case_id);
CREATE INDEX idx_treehole_records_post ON treehole_intervention_records (post_id);
CREATE INDEX idx_treehole_records_action ON treehole_intervention_records (action);
CREATE INDEX idx_treehole_records_created_at ON treehole_intervention_records (created_at);
