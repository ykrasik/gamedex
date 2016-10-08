CREATE TABLE IF NOT EXISTS libraries(
  id IDENTITY PRIMARY KEY,
  path VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  platform INT NOT NULL
);

CREATE TABLE IF NOT EXISTS Games(
  id INT AUTO_INCREMENT NOT NULL,
  path VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  release_date DATE NULL,
  description VARCHAR(255) NULL,
  critic_score DECIMAL(9, 1) NULL,
  user_score DECIMAL(9, 1) NULL,
  metacritic_url VARCHAR(255) NOT NULL,
  giantbomb_url VARCHAR(255) NULL,
  thumbnail BLOB NULL,
  poster BLOB NULL,
  last_modified DATETIME NOT NULL,
  library_id INT NOT NULL REFERENCES Libraries(id) ON DELETE CASCADE,
  CONSTRAINT pk_Games PRIMARY KEY (id)
);
CREATE UNIQUE INDEX Games_path_unique ON Games(path);

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