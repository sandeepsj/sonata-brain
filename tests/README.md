# Test Suite Documentation

## Folder Structure
```
tests/
├── api-tests/                    # API endpoint tests
│   ├── student_api_tests.sh      # Student Controller tests
│   ├── course_api_tests.sh       # Course Controller tests (to be created)
│   ├── payment_api_tests.sh      # Payment Controller tests (to be created)
│   ├── fee_structure_api_tests.sh # Fee Structure tests (to be created)
│   └── run_all_tests.sh          # Main test runner
├── test-data/                    # Test data templates
│   ├── students.json             # Sample student data
│   └── test_setup.sh             # Common test data setup
└── reports/                      # Test execution reports
    └── test_results.html         # Generated test reports
```

## Student API Test Suite

The Student API test suite (`student_api_tests.sh`) includes **42 comprehensive test cases** covering:

### Test Categories:

#### 1. **Student Creation Tests (9 tests)**
- Create students with complete data
- Create students with minimal data  
- Create students with special characters
- Validation failures (empty names, null values, invalid JSON)

#### 2. **Student Retrieval Tests (3 tests)**
- Get student by valid ID
- Get non-existent student (404 error)
- Get with invalid ID format

#### 3. **Student Search Tests (10 tests)**
- Search without parameters (get all)
- Search by individual fields (name, school, pinCode, contactNo, className, admissionDate)
- Search with multiple parameters
- Search with non-matching criteria
- Search with invalid date format

#### 4. **Student Update Tests (5 tests)**
- Update with complete data
- Update non-existent student (404 error)
- Update with empty name (validation failure)
- Update with partial data
- Update with invalid JSON

#### 5. **Edge Cases and Special Scenarios (8 tests)**
- Future birth dates
- Very long names
- Large pin codes
- URL encoding in search parameters
- Special characters in search
- Boundary value testing

#### 6. **Workflow and Performance Tests (7 tests)**
- Complete student lifecycle (create → retrieve → search → update)
- Concurrent student creation
- Bulk student creation (10 students)
- Large dataset search performance

## Key Features:

✅ **Pass/Fail Reporting** - Each test shows clear ✅ PASS or ❌ FAIL status
✅ **HTTP Status Validation** - Checks expected vs actual status codes  
✅ **Response Validation** - Uses `jq` to validate JSON response structure
✅ **Dynamic ID Handling** - Extracts and uses actual student IDs from creation responses
✅ **Color-coded Output** - Green for pass, red for fail, blue for info
✅ **Detailed Logging** - Shows request/response data for debugging
✅ **Success Rate Calculation** - Shows overall system health percentage
✅ **Error Handling** - Graceful handling of missing data or failed operations

## Usage:

```bash
# Run student API tests
cd /home/sandeepsj/Developer/sonata-brain
./tests/api-tests/student_api_tests.sh

# Show help
./tests/api-tests/student_api_tests.sh --help

# Run with verbose output
./tests/api-tests/student_api_tests.sh --verbose
```

## Expected Output:

The script will show:
1. Server status check
2. Test data creation with IDs
3. Each test case with pass/fail status
4. Final summary with success rate
5. Overall system health assessment

Example:
```
🎓 Student API Test Suite
========================

✅ Server is running

🏗️ Creating Test Students...
[1] Testing: Create Test Student 1
✅ PASS - HTTP 200 (Expected: 200)

[2] Testing: Create Test Student 2  
✅ PASS - HTTP 200 (Expected: 200)

...

═══ STUDENT API TEST SUMMARY ═══
Total Tests: 42
Passed: 40
Failed: 2
Success Rate: 95%
✅ Student API health: EXCELLENT
```

The test suite is ready to run and will thoroughly validate all Student Controller functionality!
