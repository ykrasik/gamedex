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

    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    release_date DATE NULL,
    critic_score DECIMAL(9, 1) NULL,
    user_score DECIMAL(9, 1) NULL,

    provider_data VARCHAR(8192) NOT NULL
);

CREATE TABLE IF NOT EXISTS images(
    game_id INT PRIMARY KEY REFERENCES games(id) ON DELETE CASCADE,
    thumbnail BLOB NULL,
    poster BLOB NULL,
    screenshot1 BLOB NULL,
    screenshot2 BLOB NULL,
    screenshot3 BLOB NULL,
    screenshot4 BLOB NULL,
    screenshot5 BLOB NULL,
    screenshot6 BLOB NULL,
    screenshot7 BLOB NULL,
    screenshot8 BLOB NULL,
    screenshot9 BLOB NULL,
    screenshot10 BLOB NULL,
    image_data VARCHAR(8192) NOT NULL
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