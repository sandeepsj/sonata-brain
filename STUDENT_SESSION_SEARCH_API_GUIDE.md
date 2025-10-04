# STUDENT SESSION SEARCH APIS - COMPLETE GUIDE

All APIs return both ACTIVE and INACTIVE sessions with full details including joining date and disjoining date.

## API ENDPOINTS

### 1. FLEXIBLE SEARCH API (Recommended)
GET /api/student-sessions/search

Query Parameters (all optional, but at least one required):
- course: Filter by course name
- year: Filter by term year
- termNumber: Filter by term number

Examples:

# Search by course only
curl -X GET "http://localhost:8080/api/student-sessions/search?course=VOCAL"

# Search by year and term (all courses)
curl -X GET "http://localhost:8080/api/student-sessions/search?year=2024&termNumber=3"

# Search by course and year (all terms)
curl -X GET "http://localhost:8080/api/student-sessions/search?course=VOCAL&year=2024"

# Search by course, year, and term (most specific)
curl -X GET "http://localhost:8080/api/student-sessions/search?course=VOCAL&year=2024&termNumber=3"

---

### 2. GET STUDENTS BY COURSE
GET /api/student-sessions/students/course/{courseName}

Returns all students who have ever enrolled in a specific course.

curl -X GET "http://localhost:8080/api/student-sessions/students/course/VOCAL"

---

### 3. GET STUDENTS BY COURSE AND YEAR
GET /api/student-sessions/students/course/{courseName}/year/{year}

Returns students enrolled in a course during any term in a specific year.

curl -X GET "http://localhost:8080/api/student-sessions/students/course/VOCAL/year/2024"

---

### 4. GET STUDENTS BY COURSE, YEAR, AND TERM
GET /api/student-sessions/students/course/{courseName}/year/{year}/term/{termNumber}

Returns students enrolled in a course during a specific term.

curl -X GET "http://localhost:8080/api/student-sessions/students/course/VOCAL/year/2024/term/3"

---

### 5. GET STUDENTS BY YEAR AND TERM (All Courses)
GET /api/student-sessions/students/year/{year}/term/{termNumber}

Returns all students enrolled during a specific term across all courses.

curl -X GET "http://localhost:8080/api/student-sessions/students/year/2024/term/3"

---

## RESPONSE FORMAT

All endpoints return an array of StudentSession objects:

[
  {
    "id": 27,
    "student": {
      "admissionNo": 4,
      "studentName": "John Doe",
      "guardianName": "Jane Doe",
      "contactNumber": "1234567890"
    },
    "course": {
      "id": 1,
      "name": "VOCAL"
    },
    "dateOfJoining": "2024-11-01T13:47:00",
    "dateOfDisjoining": null
  },
  {
    "id": 15,
    "student": {
      "admissionNo": 3,
      "studentName": "Alice Smith",
      "guardianName": "Bob Smith",
      "contactNumber": "9876543210"
    },
    "course": {
      "id": 1,
      "name": "VOCAL"
    },
    "dateOfJoining": "2024-01-15T10:00:00",
    "dateOfDisjoining": "2024-09-30T18:00:00"
  }
]

NOTE: dateOfDisjoining is null for ACTIVE sessions and has a timestamp for INACTIVE/past sessions.

---

## COMMON USE CASES

# Get all VOCAL students enrolled in 2024
curl -X GET "http://localhost:8080/api/student-sessions/search?course=VOCAL&year=2024"

# Get all students enrolled in term 3 of 2024 (any course)
curl -X GET "http://localhost:8080/api/student-sessions/search?year=2024&termNumber=3"

# Get VOCAL students specifically in term 3 of 2024
curl -X GET "http://localhost:8080/api/student-sessions/search?course=VOCAL&year=2024&termNumber=3"

# Get all students who have ever taken VOCAL
curl -X GET "http://localhost:8080/api/student-sessions/students/course/VOCAL"

# Get all PIANO students from 2024
curl -X GET "http://localhost:8080/api/student-sessions/students/course/PIANO/year/2024"

# Get all students from term 1 of 2025
curl -X GET "http://localhost:8080/api/student-sessions/students/year/2025/term/1"

---

## KEY FEATURES

✅ Returns both ACTIVE and INACTIVE sessions
✅ Includes joining date and disjoining date for all sessions
✅ Supports flexible filtering by course, year, and term
✅ Can combine filters in any way
✅ Shows complete historical enrollment data
✅ Useful for reporting, billing, and student tracking

