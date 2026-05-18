#
#  @FileName : db_connection.py
#  @Description : AI server common DB connection module
#

import os
from pathlib import Path
from urllib.parse import urlparse

import pg8000.native

conn = None
db_config = None


def _find_properties_path():
    env_path = os.getenv("DB_PROPERTIES_PATH")
    if env_path:
        return Path(env_path)

    project_root = Path(__file__).resolve().parents[2]
    return project_root / "src" / "main" / "resources" / "application-local.properties"


def _load_properties(path):
    properties = {}

    with path.open(encoding="utf-8") as file:
        for raw_line in file:
            line = raw_line.strip()
            if not line or line.startswith("#") or line.startswith("!"):
                continue

            indexes = [index for index in (line.find("="), line.find(":")) if index != -1]
            if not indexes:
                continue

            separator_index = min(indexes)
            key = line[:separator_index].strip()
            value = line[separator_index + 1:].strip()
            properties[key] = value

    return properties


def _parse_jdbc_postgresql_url(jdbc_url):
    if not jdbc_url.startswith("jdbc:"):
        raise ValueError("spring.datasource.url must be a JDBC URL.")

    parsed = urlparse(jdbc_url.removeprefix("jdbc:"))
    if parsed.scheme != "postgresql":
        raise ValueError("Only PostgreSQL JDBC URLs are supported.")

    return {
        "host": parsed.hostname,
        "port": parsed.port or 5432,
        "database": parsed.path.lstrip("/"),
    }


def _load_db_config():
    properties_path = _find_properties_path()
    properties = _load_properties(properties_path)

    jdbc_url = os.getenv("DB_URL") or properties.get("spring.datasource.url")
    user = os.getenv("DB_USER") or properties.get("spring.datasource.username")
    password = os.getenv("DB_PASSWORD") or properties.get("spring.datasource.password")

    if not jdbc_url or not user or not password:
        raise ValueError(
            "DB settings are missing. Check spring.datasource.url, "
            "spring.datasource.username, and spring.datasource.password."
        )

    config = _parse_jdbc_postgresql_url(jdbc_url)
    config.update({"user": user, "password": password})
    return config


def get_connection():
    """
    Return a reused PostgreSQL connection.
    """
    global conn, db_config
    try:
        if db_config is None:
            db_config = _load_db_config()
        if conn is None:
            conn = pg8000.native.Connection(**db_config)
        return conn
    except Exception as e:
        print(f"DB connection failed: {e}")
        return None
