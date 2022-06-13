CREATE SCHEMA idm;

CREATE TABLE idm.token_status
(
    id	INT	NOT NULL PRIMARY KEY,
    value	VARCHAR(32)	NOT NULL
);

CREATE TABLE idm.user_status
(
    id	INT	NOT NULL PRIMARY KEY,
    value	VARCHAR(32)	NOT NULL
);

CREATE TABLE idm.role
(
    id	INT	NOT NULL PRIMARY KEY,
    name	VARCHAR(32)	NOT NULL,
    description	VARCHAR(128)	NOT NULL,
    precedence	INT	NOT NULL
);

CREATE TABLE idm.user
(
    id	INT	NOT NULL PRIMARY KEY AUTO_INCREMENT,
    email	VARCHAR(32)	NOT NULL UNIQUE,
    user_status_id	INT	NOT NULL,
    salt	CHAR(8)	NOT NULL,
    hashed_password	CHAR(88)	NOT NULL,
    FOREIGN KEY (user_status_id)
        REFERENCES idm.user_status (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE idm.refresh_token
(
    id	INT	NOT NULL PRIMARY KEY AUTO_INCREMENT,
    token	CHAR(36)	NOT NULL UNIQUE,
    user_id	INT	NOT NULL,
    token_status_id	INT	NOT NULL,
    expire_time	TIMESTAMP	NOT NULL,
    max_life_time	TIMESTAMP	NOT NULL,
    FOREIGN KEY (user_id)
        REFERENCES idm.user (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (token_status_id)
        REFERENCES idm.token_status (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE idm.user_role
(
    user_id	INT	NOT NULL,
    role_id	INT	NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id)
        REFERENCES idm.user (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (role_id)
        REFERENCES idm.role (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE SCHEMA movies;

CREATE TABLE movies.genre
(
    id	INT	NOT NULL PRIMARY KEY,
    name	VARCHAR(32)	NOT NULL
);

CREATE TABLE movies.person
(
    id	INT	NOT NULL PRIMARY KEY,
    name	VARCHAR(128)	NOT NULL,
    birthday	DATE	NULL,
    biography	VARCHAR(8192)	NULL,
    birthplace	VARCHAR(128)	NULL,
    popularity	DECIMAL	NULL,
    profile_path	VARCHAR(32)	NULL
);

CREATE TABLE movies.movie
(
    id	INT	NOT NULL PRIMARY KEY,
    title	VARCHAR(128)	NOT NULL,
    year	INT	NOT NULL,
    director_id	INT	NOT NULL,
    rating	DECIMAL	NOT NULL DEFAULT 0.0,
    num_votes	INT	NOT NULL DEFAULT 0,
    budget	BIGINT	NULL,
    revenue	BIGINT	NULL,
    overview	VARCHAR(8192)	NULL,
    backdrop_path	VARCHAR(32)	NULL,
    poster_path	VARCHAR(32)	NULL,
    hidden	BOOLEAN	NOT NULL DEFAULT FALSE,
    FOREIGN KEY (director_id)
        REFERENCES movies.person (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE TABLE movies.movie_person
(
    movie_id	INT	NOT NULL,
    person_id	INT	NOT NULL,
    PRIMARY KEY (movie_id, person_id),
    FOREIGN KEY (movie_id)
        REFERENCES movies.movie (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (person_id)
        REFERENCES movies.person (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE movies.movie_genre
(
    movie_id	INT	NOT NULL,
    genre_id	INT	NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id)
        REFERENCES movies.movie (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (genre_id)
        REFERENCES movies.genre (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE SCHEMA billing;

CREATE TABLE billing.cart
(
    user_id	INT	NOT NULL,
    movie_id	INT	NOT NULL,
    quantity	INT	NOT NULL,
    PRIMARY KEY (user_id, movie_id),
    FOREIGN KEY (user_id) REFERENCES idm.user (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies.movie (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE billing.sale
(
    id	INT	NOT NULL PRIMARY KEY AUTO_INCREMENT,
    user_id	INT	NOT NULL,
    total DECIMAL(19,4)	NOT NULL,
    order_date	TIMESTAMP	NOT NULL,
    FOREIGN KEY (user_id)
        REFERENCES idm.user (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE billing.sale_item
(
    sale_id	INT	NOT NULL,
    movie_id	INT	NOT NULL,
    quantity	INT	NOT NULL,
    PRIMARY KEY (sale_id, movie_id),
    FOREIGN KEY (sale_id) REFERENCES billing.sale (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies.movie (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE TABLE billing.movie_price
(
    movie_id	INT	NOT NULL PRIMARY KEY,
    unit_price	DECIMAL(19,4)	NOT NULL,
    premium_discount	INT	NOT NULL,
    CHECK (premium_discount BETWEEN 0 AND 25),
    FOREIGN KEY (movie_id) REFERENCES movies.movie (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);