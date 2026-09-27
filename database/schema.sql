CREATE DATABASE IF NOT EXISTS online_assessment
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE online_assessment;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_email UNIQUE (email),
    INDEX idx_user_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assessments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    duration_minutes INT NOT NULL,
    total_marks DOUBLE NOT NULL,
    passing_marks DOUBLE NOT NULL,
    default_negative_marks DOUBLE NOT NULL,
    start_at DATETIME(6),
    end_at DATETIME(6),
    attempts_allowed INT NOT NULL DEFAULT 1,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_assessment_published (published),
    INDEX idx_assessment_window (start_at, end_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    marks DOUBLE NOT NULL,
    negative_marks DOUBLE NOT NULL,
    explanation VARCHAR(2000),
    display_order INT NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_question_assessment (assessment_id),
    CONSTRAINT fk_question_assessment FOREIGN KEY (assessment_id)
        REFERENCES assessments(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS question_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_text VARCHAR(1000) NOT NULL,
    correct BOOLEAN NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_option_question (question_id),
    CONSTRAINT fk_option_question FOREIGN KEY (question_id)
        REFERENCES questions(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    assessment_id BIGINT NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    submitted_at DATETIME(6),
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_attempt_user (user_id),
    INDEX idx_attempt_assessment (assessment_id),
    INDEX idx_attempt_status (status),
    CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_attempt_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    marked_for_review BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attempt_question (attempt_id, question_id),
    INDEX idx_answer_attempt (attempt_id),
    CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS answer_selected_options (
    answer_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    PRIMARY KEY (answer_id, option_id),
    CONSTRAINT fk_selected_answer FOREIGN KEY (answer_id) REFERENCES answers(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assessment_id BIGINT NOT NULL,
    total_questions INT NOT NULL,
    attempted_questions INT NOT NULL,
    unattempted_questions INT NOT NULL,
    correct_answers INT NOT NULL,
    wrong_answers INT NOT NULL,
    total_marks DOUBLE NOT NULL,
    obtained_marks DOUBLE NOT NULL,
    negative_marks DOUBLE NOT NULL,
    percentage DOUBLE NOT NULL,
    passed BOOLEAN NOT NULL,
    time_taken_seconds BIGINT NOT NULL,
    submitted_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_attempt (attempt_id),
    INDEX idx_result_assessment_score (assessment_id, obtained_marks),
    CONSTRAINT fk_result_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id),
    CONSTRAINT fk_result_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_result_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id)
) ENGINE=InnoDB;
