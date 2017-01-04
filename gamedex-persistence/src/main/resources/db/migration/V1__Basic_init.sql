CREATE TABLE IF NOT EXISTS libraries(
    id IDENTITY PRIMARY KEY,
    path VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    platform INT NOT NULL
);

CREATE TABLE IF NOT EXISTS games(
    id IDENTITY PRIMARY KEY,
    path VARCHAR(255) NOT NULL UNIQUE,
    last_modified DATETIME NOT NULL,
    library_id INT NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,

    data VARCHAR(16384) NOT NULL
);

CREATE TABLE IF NOT EXISTS images(
    game_id INT PRIMARY KEY REFERENCES games(id) ON DELETE CASCADE,

    thumbnail BLOB NULL,
    thumbnail_url VARCHAR(256) NULL,

    poster BLOB NULL,
    poster_url VARCHAR(256) NULL,

    screenshot1 BLOB NULL,
    screenshot1_url VARCHAR(256) NULL,

    screenshot2 BLOB NULL,
    screenshot2_url VARCHAR(256) NULL,

    screenshot3 BLOB NULL,
    screenshot3_url VARCHAR(256) NULL,

    screenshot4 BLOB NULL,
    screenshot4_url VARCHAR(256) NULL,

    screenshot5 BLOB NULL,
    screenshot5_url VARCHAR(256) NULL,

    screenshot6 BLOB NULL,
    screenshot6_url VARCHAR(256) NULL,

    screenshot7 BLOB NULL,
    screenshot7_url VARCHAR(256) NULL,

    screenshot8 BLOB NULL,
    screenshot8_url VARCHAR(256) NULL,

    screenshot9 BLOB NULL,
    screenshot9_url VARCHAR(256) NULL,

    screenshot10 BLOB NULL,
    screenshot10_url VARCHAR(256) NULL,
);

CREATE TABLE IF NOT EXISTS genres(
    id IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS game_genres(
    id IDENTITY PRIMARY KEY,
    game_id INT NOT NULL REFERENCES games(id),
    genre_id INT NOT NULL REFERENCES genres(id)
);
CREATE UNIQUE INDEX game_genres_unique ON game_genres(game_id, genre_id);

CREATE TABLE IF NOT EXISTS excluded_paths(
    id IDENTITY PRIMARY KEY,
    path VARCHAR(255) NOT NULL UNIQUE
);