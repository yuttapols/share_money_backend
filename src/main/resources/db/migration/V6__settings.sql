CREATE TABLE settings
(
    key    VARCHAR(100) PRIMARY KEY,
    value  VARCHAR(500) NOT NULL
);

INSERT INTO settings (key, value) VALUES ('installment_choices', '2,3,6,9,12');
