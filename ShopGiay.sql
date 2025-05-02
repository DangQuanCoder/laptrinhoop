CREATE DATABASE ShopGiay;
Use ShopGiay;


CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(50),
    name VARCHAR(100),
    address VARCHAR(255),
    phone VARCHAR(20),
    is_admin BOOLEAN
);

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    price DOUBLE,
    stock INT
);

CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    total DOUBLE,
    FOREIGN KEY (username) REFERENCES users(username)
);

CREATE TABLE order_items (
    order_id INT,
    product_id INT,
    quantity INT,
    price DOUBLE,
    PRIMARY KEY (order_id, product_id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

SHOW tables;

SELECT * FROM users;
