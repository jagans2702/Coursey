package com.example.coursey.core.database

object CourseTable {
    const val NAME = "courses"
    const val ID = "id"
    const val TITLE = "title"
    const val INSTRUCTOR = "instructor"
    const val PROGRESS = "progress"
    const val LESSONS_COUNT = "lessons_count"
    const val POSITION = "position"

    const val CREATE = """
        CREATE TABLE $NAME (
            $ID INTEGER PRIMARY KEY NOT NULL,
            $TITLE TEXT NOT NULL,
            $INSTRUCTOR TEXT NOT NULL,
            $PROGRESS INTEGER NOT NULL DEFAULT 0,
            $LESSONS_COUNT INTEGER NOT NULL DEFAULT 0,
            $POSITION INTEGER NOT NULL DEFAULT 0
        )
    """
}

object LessonTable {
    const val NAME = "lessons"
    const val COURSE_ID = "course_id"
    const val ID = "id"
    const val TITLE = "title"
    const val COMPLETED = "completed"
    const val POSITION = "position"

    const val CREATE = """
        CREATE TABLE $NAME (
            $COURSE_ID INTEGER NOT NULL,
            $ID INTEGER NOT NULL,
            $TITLE TEXT NOT NULL,
            $COMPLETED INTEGER NOT NULL DEFAULT 0,
            $POSITION INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY ($COURSE_ID, $ID),
            FOREIGN KEY ($COURSE_ID) REFERENCES ${CourseTable.NAME}(${CourseTable.ID}) ON DELETE CASCADE
        )
    """
}
