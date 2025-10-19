-- temporary (please change to suit ur needs)
CREATE TABLE IF NOT EXISTS students (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    class_group TEXT,
    email TEXT,
    phone TEXT,
    enrollment_date TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_name TEXT NOT NULL,
    session_date TEXT NOT NULL DEFAULT (DATE('now')), -- auto “today”
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    location TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN',              -- toggle OPEN/CLOSED
    created_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE IF NOT EXISTS attendance_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER NOT NULL,
    student_id TEXT NOT NULL,
    status TEXT NOT NULL,
    marked_at TEXT,
    method TEXT,
    confidence REAL,
    notes TEXT,
    UNIQUE (session_id, student_id),
    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

-- temporary (please change to suit ur needs)
CREATE TABLE IF NOT EXISTS face_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id TEXT NOT NULL,
    storage_type TEXT NOT NULL DEFAULT 'FILE',
    data BLOB,
    file_path TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);
