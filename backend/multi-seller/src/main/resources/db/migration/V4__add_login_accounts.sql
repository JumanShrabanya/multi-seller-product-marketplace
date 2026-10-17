ALTER TABLE seller ADD COLUMN email VARCHAR(200);
ALTER TABLE seller ADD COLUMN password VARCHAR(100);

UPDATE seller SET email = 'buildright@example.com', password = 'buildright123' WHERE id = 1;
UPDATE seller SET email = 'metro@example.com', password = 'metro123' WHERE id = 2;
UPDATE seller SET email = 'rajasthan@example.com', password = 'rajasthan123' WHERE id = 3;
UPDATE seller SET email = 'newhorizon@example.com', password = 'newhorizon123' WHERE id = 4;
UPDATE seller SET email = 'clearance@example.com', password = 'clearance123' WHERE id = 5;

ALTER TABLE seller ALTER COLUMN email SET NOT NULL;
ALTER TABLE seller ALTER COLUMN password SET NOT NULL;
ALTER TABLE seller ADD CONSTRAINT uq_seller_email UNIQUE (email);

CREATE TABLE buyer (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);

INSERT INTO buyer (name, email, password) VALUES
    ('Priya Sharma', 'priya@example.com', 'priya123'),
    ('Amit Verma', 'amit@example.com', 'amit123');
