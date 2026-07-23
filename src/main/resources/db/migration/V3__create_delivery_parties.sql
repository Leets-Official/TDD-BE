CREATE TABLE delivery_parties
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    food_category_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    min_participants INT NOT NULL,
    max_participants INT NOT NULL,
    order_expected_at DATETIME(6) NOT NULL,
    status VARCHAR(50) NOT NULL,
    closed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
