#!/bin/bash

# Student API Test Suite
# Comprehensive tests for all Student-related endpoints

BASE_URL="http://localhost:8080"
PASSED=0
FAILED=0
TOTAL=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Test data variables
STUDENT_ID_1=""
STUDENT_ID_2=""
STUDENT_ID_3=""

# Function to run test and report result
run_test() {
    local test_name="$1"
    local expected_status="$2"
    local curl_command="$3"
    local validate_response="$4"

    TOTAL=$((TOTAL + 1))
    echo -e "\n${BLUE}[$TOTAL] Testing: $test_name${NC}"

    # Execute curl command and capture response and HTTP status
    response=$(eval "$curl_command" 2>/dev/null)
    http_status=$(eval "$curl_command -w '%{http_code}'" -o /dev/null -s 2>/dev/null)

    # Check if test passed
    if [ "$http_status" = "$expected_status" ]; then
        if [ -z "$validate_response" ] || eval "$validate_response"; then
            PASSED=$((PASSED + 1))
            echo -e "${GREEN}✅ PASS${NC} - HTTP $http_status (Expected: $expected_status)"
        else
            FAILED=$((FAILED + 1))
            echo -e "${RED}❌ FAIL${NC} - HTTP $http_status but response validation failed"
        fi
    else
        FAILED=$((FAILED + 1))
        echo -e "${RED}❌ FAIL${NC} - HTTP $http_status (Expected: $expected_status)"
    fi

    # Show response for debugging (truncated if too long)
    if [ ${#response} -gt 300 ]; then
        echo "Response: ${response:0:300}..."
    else
        echo "Response: $response"
    fi

    # Return response for further use
    echo "$response"
}

# Function to extract student ID from response
extract_student_id() {
    echo "$1" | jq -r '.admissionNo // .id // empty'
}

# Function to create test data and extract IDs
create_test_students() {
    echo -e "${YELLOW}🏗️  Creating Test Students...${NC}"

    # Create Student 1
    student1_response=$(run_test "Create Test Student 1" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Alice Johnson\",
            \"contactNo\": \"9876543210\",
            \"dob\": \"2010-05-15\",
            \"className\": \"10A\",
            \"school\": \"Green Valley High School\",
            \"address\": \"123 Main Street, Bangalore\",
            \"pinCode\": 560001,
            \"remarks\": \"Test student 1 for API testing\"
        }'" \
        "echo '$response' | jq -e '.studentName == \"Alice Johnson\" and .admissionNo'")

    STUDENT_ID_1=$(extract_student_id "$student1_response")

    # Create Student 2
    student2_response=$(run_test "Create Test Student 2" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Bob Smith\",
            \"contactNo\": \"9876543211\",
            \"dob\": \"2011-03-20\",
            \"className\": \"9B\",
            \"school\": \"Blue Ridge School\",
            \"address\": \"456 Park Avenue, Chennai\",
            \"pinCode\": 600001,
            \"remarks\": \"Test student 2 for API testing\"
        }'" \
        "echo '$response' | jq -e '.studentName == \"Bob Smith\" and .admissionNo'")

    STUDENT_ID_2=$(extract_student_id "$student2_response")

    # Create Student 3 with minimal data
    student3_response=$(run_test "Create Test Student 3 (Minimal Data)" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Carol Davis\"
        }'" \
        "echo '$response' | jq -e '.studentName == \"Carol Davis\" and .admissionNo'")

    STUDENT_ID_3=$(extract_student_id "$student3_response")

    echo -e "${GREEN}✅ Test students created with IDs: $STUDENT_ID_1, $STUDENT_ID_2, $STUDENT_ID_3${NC}"
}

main() {
    echo -e "${PURPLE}🎓 Student API Test Suite${NC}"
    echo -e "${PURPLE}========================${NC}"
    echo "Testing all Student Controller endpoints"
    echo "Base URL: $BASE_URL"
    echo ""

    # Check if server is running
    echo "Checking server status..."
    server_status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/students/search")
    if [ "$server_status" != "200" ]; then
        echo -e "${RED}❌ Server is not running or not responding. Please start the application first.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Server is running${NC}"

    # STUDENT CREATION TESTS
    echo -e "\n${BLUE}═══ STUDENT CREATION TESTS ═══${NC}"

    # Create test students and capture their IDs
    create_test_students

    # Test 4: Create Student with All Fields
    run_test "Create Student with All Fields" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"David Wilson\",
            \"contactNo\": \"9876543212\",
            \"dob\": \"2012-07-10\",
            \"className\": \"8C\",
            \"school\": \"Sunshine Academy\",
            \"address\": \"789 Oak Road, Mumbai\",
            \"pinCode\": 400001,
            \"remarks\": \"Student with complete information\"
        }'" \
        "echo '$response' | jq -e '.studentName == \"David Wilson\" and .contactNo == \"9876543212\"'"

    # Test 5: Create Student with Special Characters
    run_test "Create Student with Special Characters" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"José María O\u0027Connor\",
            \"contactNo\": \"9876543213\",
            \"school\": \"St. Mary\u0027s International School\",
            \"address\": \"Plot #123, Sector-5, Phase-II\",
            \"remarks\": \"Testing special chars: !@#$%^&*()_+-=\"
        }'" \
        "echo '$response' | jq -e '.studentName | contains(\"José\")'"

    # VALIDATION FAILURE TESTS
    echo -e "\n${BLUE}═══ STUDENT CREATION VALIDATION TESTS ═══${NC}"

    # Test 6: Create Student with Empty Name (Should Fail)
    run_test "Create Student with Empty Name (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"\",
            \"contactNo\": \"9876543214\"
        }'" \
        ""

    # Test 7: Create Student with Only Whitespace Name (Should Fail)
    run_test "Create Student with Whitespace Name (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"   \",
            \"contactNo\": \"9876543215\"
        }'" \
        ""

    # Test 8: Create Student with Null Name (Should Fail)
    run_test "Create Student with Null Name (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"contactNo\": \"9876543216\"
        }'" \
        ""

    # Test 9: Create Student with Invalid JSON (Should Fail)
    run_test "Create Student with Invalid JSON (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Invalid JSON\",
            \"contactNo\": 9876543217,
        }'" \
        ""

    # STUDENT RETRIEVAL TESTS
    echo -e "\n${BLUE}═══ STUDENT RETRIEVAL TESTS ═══${NC}"

    # Test 10: Get Student by ID
    if [ -n "$STUDENT_ID_1" ]; then
        run_test "Get Student by ID" "200" \
            "curl -s -X GET '$BASE_URL/api/students/$STUDENT_ID_1'" \
            "echo '$response' | jq -e '.admissionNo == $STUDENT_ID_1 and .studentName == \"Alice Johnson\"'"
    else
        echo -e "${YELLOW}⚠️  Skipping Get Student by ID test - no valid student ID available${NC}"
        TOTAL=$((TOTAL + 1))
        FAILED=$((FAILED + 1))
    fi

    # Test 11: Get Non-existent Student (Should Fail)
    run_test "Get Non-existent Student (Should Fail)" "404" \
        "curl -s -X GET '$BASE_URL/api/students/999999'" \
        ""

    # Test 12: Get Student with Invalid ID Format (Should Fail)
    run_test "Get Student with Invalid ID Format (Should Fail)" "400" \
        "curl -s -X GET '$BASE_URL/api/students/invalid-id'" \
        ""

    # STUDENT SEARCH TESTS
    echo -e "\n${BLUE}═══ STUDENT SEARCH TESTS ═══${NC}"

    # Test 13: Search All Students (No Parameters)
    run_test "Search All Students" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search'" \
        "echo '$response' | jq -e 'type == \"array\" and length > 0'"

    # Test 14: Search by Name
    run_test "Search Students by Name" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?name=Alice'" \
        "echo '$response' | jq -e 'type == \"array\" and (. | length == 0 or (.[0].studentName | contains(\"Alice\")))'"

    # Test 15: Search by School
    run_test "Search Students by School" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?school=Green%20Valley'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 16: Search by Pin Code
    run_test "Search Students by Pin Code" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?pinCode=560001'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 17: Search by Contact Number
    run_test "Search Students by Contact" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?contactNo=9876543210'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 18: Search by Class Name
    run_test "Search Students by Class" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?className=10A'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 19: Search by Admission Date
    run_test "Search Students by Admission Date" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?admissionDate=2025-09-28'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 20: Search with Multiple Parameters
    run_test "Search with Multiple Parameters" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?name=Alice&school=Green%20Valley&pinCode=560001'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 21: Search with Non-matching Criteria
    run_test "Search with Non-matching Criteria" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?name=NonExistentStudent'" \
        "echo '$response' | jq -e 'type == \"array\" and length == 0'"

    # Test 22: Search with Invalid Date Format
    run_test "Search with Invalid Date Format" "400" \
        "curl -s -X GET '$BASE_URL/api/students/search?admissionDate=invalid-date'" \
        ""

    # STUDENT UPDATE TESTS
    echo -e "\n${BLUE}═══ STUDENT UPDATE TESTS ═══${NC}"

    # Test 23: Update Student Successfully
    if [ -n "$STUDENT_ID_1" ]; then
        run_test "Update Student Successfully" "200" \
            "curl -s -X PUT '$BASE_URL/api/students/$STUDENT_ID_1' -H 'Content-Type: application/json' -d '{
                \"studentName\": \"Alice Johnson Updated\",
                \"contactNo\": \"9876543299\",
                \"dob\": \"2010-05-15\",
                \"className\": \"10A\",
                \"school\": \"Green Valley High School\",
                \"address\": \"123 Main Street Updated, Bangalore\",
                \"pinCode\": 560002,
                \"remarks\": \"Updated student information\"
            }'" \
            "echo '$response' | jq -e '.studentName == \"Alice Johnson Updated\" and .contactNo == \"9876543299\"'"
    else
        echo -e "${YELLOW}⚠️  Skipping Update Student test - no valid student ID${NC}"
        TOTAL=$((TOTAL + 1))
        FAILED=$((FAILED + 1))
    fi

    # Test 24: Update Non-existent Student (Should Fail)
    run_test "Update Non-existent Student (Should Fail)" "404" \
        "curl -s -X PUT '$BASE_URL/api/students/999999' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Non-existent Student\",
            \"contactNo\": \"9999999999\"
        }'" \
        ""

    # Test 25: Update Student with Empty Name (Should Fail)
    if [ -n "$STUDENT_ID_2" ]; then
        run_test "Update Student with Empty Name (Should Fail)" "400" \
            "curl -s -X PUT '$BASE_URL/api/students/$STUDENT_ID_2' -H 'Content-Type: application/json' -d '{
                \"studentName\": \"\",
                \"contactNo\": \"9876543211\"
            }'" \
            ""
    else
        echo -e "${YELLOW}⚠️  Skipping Update with Empty Name test - no valid student ID${NC}"
        TOTAL=$((TOTAL + 1))
        FAILED=$((FAILED + 1))
    fi

    # Test 26: Update Student with Partial Data
    if [ -n "$STUDENT_ID_2" ]; then
        run_test "Update Student with Partial Data" "200" \
            "curl -s -X PUT '$BASE_URL/api/students/$STUDENT_ID_2' -H 'Content-Type: application/json' -d '{
                \"studentName\": \"Bob Smith Updated\",
                \"remarks\": \"Partially updated student\"
            }'" \
            "echo '$response' | jq -e '.studentName == \"Bob Smith Updated\"'"
    else
        echo -e "${YELLOW}⚠️  Skipping Partial Update test - no valid student ID${NC}"
        TOTAL=$((TOTAL + 1))
        FAILED=$((FAILED + 1))
    fi

    # Test 27: Update with Invalid JSON (Should Fail)
    if [ -n "$STUDENT_ID_3" ]; then
        run_test "Update with Invalid JSON (Should Fail)" "400" \
            "curl -s -X PUT '$BASE_URL/api/students/$STUDENT_ID_3' -H 'Content-Type: application/json' -d '{
                \"studentName\": \"Invalid Update\",
                \"contactNo\": 9876543213,
            }'" \
            ""
    else
        echo -e "${YELLOW}⚠️  Skipping Invalid JSON Update test - no valid student ID${NC}"
        TOTAL=$((TOTAL + 1))
        FAILED=$((FAILED + 1))
    fi

    # EDGE CASES AND SPECIAL SCENARIOS
    echo -e "\n${BLUE}═══ EDGE CASES AND SPECIAL SCENARIOS ═══${NC}"

    # Test 28: Create Student with Future DOB
    run_test "Create Student with Future DOB" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Future Student\",
            \"dob\": \"2030-01-01\",
            \"remarks\": \"Student with future birth date\"
        }'" \
        "echo '$response' | jq -e '.dob == \"2030-01-01\"'"

    # Test 29: Create Student with Very Long Name
    run_test "Create Student with Long Name" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Extremely Long Student Name That Goes On And On And On For Testing Purposes\",
            \"remarks\": \"Testing very long names\"
        }'" \
        "echo '$response' | jq -e '.studentName | length > 50'"

    # Test 30: Create Student with Large Pin Code
    run_test "Create Student with Large Pin Code" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Pin Code Test Student\",
            \"pinCode\": 999999,
            \"remarks\": \"Testing large pin code\"
        }'" \
        "echo '$response' | jq -e '.pinCode == 999999'"

    # Test 31: Search with URL Encoding
    run_test "Search with URL Encoded Parameters" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?name=Alice%20Johnson&school=Green%20Valley%20High%20School'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 32: Search with Special Characters in Parameters
    run_test "Search with Special Characters" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?name=José'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # COMPREHENSIVE WORKFLOW TESTS
    echo -e "\n${BLUE}═══ COMPREHENSIVE WORKFLOW TESTS ═══${NC}"

    # Test 33: Complete Student Lifecycle
    echo -e "\n${YELLOW}Running Complete Student Lifecycle Test...${NC}"

    # Create student for lifecycle test
    lifecycle_response=$(curl -s -X POST "$BASE_URL/api/students" \
        -H "Content-Type: application/json" \
        -d '{
            "studentName": "Lifecycle Test Student",
            "contactNo": "9999999999",
            "dob": "2010-01-01",
            "className": "Test Class",
            "school": "Test School",
            "address": "Test Address",
            "pinCode": 123456,
            "remarks": "For lifecycle testing"
        }')

    lifecycle_student_id=$(extract_student_id "$lifecycle_response")

    if [ -n "$lifecycle_student_id" ] && [ "$lifecycle_student_id" != "null" ]; then
        echo "Lifecycle student created with ID: $lifecycle_student_id"

        # Test retrieval
        run_test "Lifecycle - Retrieve Created Student" "200" \
            "curl -s -X GET '$BASE_URL/api/students/$lifecycle_student_id'" \
            "echo '$response' | jq -e '.admissionNo == $lifecycle_student_id'"

        # Test search by name
        run_test "Lifecycle - Search by Name" "200" \
            "curl -s -X GET '$BASE_URL/api/students/search?name=Lifecycle'" \
            "echo '$response' | jq -e 'type == \"array\" and length > 0'"

        # Test update
        run_test "Lifecycle - Update Student" "200" \
            "curl -s -X PUT '$BASE_URL/api/students/$lifecycle_student_id' -H 'Content-Type: application/json' -d '{
                \"studentName\": \"Lifecycle Test Student Updated\",
                \"contactNo\": \"8888888888\",
                \"remarks\": \"Updated in lifecycle test\"
            }'" \
            "echo '$response' | jq -e '.studentName == \"Lifecycle Test Student Updated\"'"

        # Test search updated student
        run_test "Lifecycle - Search Updated Student" "200" \
            "curl -s -X GET '$BASE_URL/api/students/search?name=Updated'" \
            "echo '$response' | jq -e 'type == \"array\" and length > 0'"
    else
        echo -e "${RED}❌ Could not create lifecycle student${NC}"
        FAILED=$((FAILED + 4))
        TOTAL=$((TOTAL + 4))
    fi

    # CONCURRENT OPERATIONS TEST
    echo -e "\n${BLUE}═══ CONCURRENT OPERATIONS TEST ═══${NC}"

    # Test 34: Create Multiple Students Concurrently
    echo "Creating multiple students concurrently..."

    curl -s -X POST "$BASE_URL/api/students" \
        -H "Content-Type: application/json" \
        -d '{"studentName": "Concurrent Student 1", "contactNo": "1111111111"}' &

    curl -s -X POST "$BASE_URL/api/students" \
        -H "Content-Type: application/json" \
        -d '{"studentName": "Concurrent Student 2", "contactNo": "2222222222"}' &

    curl -s -X POST "$BASE_URL/api/students" \
        -H "Content-Type: application/json" \
        -d '{"studentName": "Concurrent Student 3", "contactNo": "3333333333"}' &

    wait

    # Verify concurrent creations
    run_test "Verify Concurrent Student Creations" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?name=Concurrent'" \
        "echo '$response' | jq -e 'type == \"array\" and length >= 3'"

    # PERFORMANCE AND STRESS TESTS
    echo -e "\n${BLUE}═══ PERFORMANCE TESTS ═══${NC}"

    # Test 35: Bulk Student Creation
    echo "Creating 10 students in sequence..."
    bulk_success=0
    for i in {1..10}; do
        bulk_response=$(curl -s -X POST "$BASE_URL/api/students" \
            -H "Content-Type: application/json" \
            -d "{
                \"studentName\": \"Bulk Student $i\",
                \"contactNo\": \"555000000$i\",
                \"className\": \"Bulk Class\",
                \"remarks\": \"Bulk creation test $i\"
            }")

        if echo "$bulk_response" | jq -e '.admissionNo' > /dev/null 2>&1; then
            bulk_success=$((bulk_success + 1))
        fi
    done

    if [ $bulk_success -eq 10 ]; then
        PASSED=$((PASSED + 1))
        echo -e "${GREEN}✅ PASS${NC} - Bulk creation: $bulk_success/10 students created"
    else
        FAILED=$((FAILED + 1))
        echo -e "${RED}❌ FAIL${NC} - Bulk creation: Only $bulk_success/10 students created"
    fi
    TOTAL=$((TOTAL + 1))

    # Test 36: Search Performance with Large Dataset
    run_test "Search Performance with Large Dataset" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search?className=Bulk%20Class'" \
        "echo '$response' | jq -e 'type == \"array\" and length >= 10'"

    # BOUNDARY VALUE TESTS
    echo -e "\n${BLUE}═══ BOUNDARY VALUE TESTS ═══${NC}"

    # Test 37: Student with Minimum Pin Code
    run_test "Create Student with Min Pin Code" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Min Pin Student\",
            \"pinCode\": 100000,
            \"remarks\": \"Testing minimum pin code\"
        }'" \
        "echo '$response' | jq -e '.pinCode == 100000'"

    # Test 38: Student with Maximum Pin Code
    run_test "Create Student with Max Pin Code" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Max Pin Student\",
            \"pinCode\": 999999,
            \"remarks\": \"Testing maximum pin code\"
        }'" \
        "echo '$response' | jq -e '.pinCode == 999999'"

    # Test 39: Student with Very Old DOB
    run_test "Create Student with Very Old DOB" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Old DOB Student\",
            \"dob\": \"1990-01-01\",
            \"remarks\": \"Testing very old birth date\"
        }'" \
        "echo '$response' | jq -e '.dob == \"1990-01-01\"'"

    # Test 40: Student with Recent DOB
    run_test "Create Student with Recent DOB" "200" \
        "curl -s -X POST '$BASE_URL/api/students' -H 'Content-Type: application/json' -d '{
            \"studentName\": \"Recent DOB Student\",
            \"dob\": \"2024-12-31\",
            \"remarks\": \"Testing recent birth date\"
        }'" \
        "echo '$response' | jq -e '.dob == \"2024-12-31\"'"

    # FINAL VERIFICATION TESTS
    echo -e "\n${BLUE}═══ FINAL VERIFICATION TESTS ═══${NC}"

    # Test 41: Verify All Created Students Exist
    run_test "Verify All Students in Search" "200" \
        "curl -s -X GET '$BASE_URL/api/students/search'" \
        "echo '$response' | jq -e 'type == \"array\" and length >= 15'"

    # Test 42: Verify Updated Student Data
    if [ -n "$STUDENT_ID_1" ]; then
        run_test "Verify Updated Student Data" "200" \
            "curl -s -X GET '$BASE_URL/api/students/$STUDENT_ID_1'" \
            "echo '$response' | jq -e '.studentName | contains(\"Updated\") and .contactNo == \"9876543299\"'"
    else
        echo -e "${YELLOW}⚠️  Skipping Verification test - no valid student ID${NC}"
        TOTAL=$((TOTAL + 1))
        FAILED=$((FAILED + 1))
    fi

    # REPORTING
    echo -e "\n${PURPLE}═══ STUDENT API TEST SUMMARY ═══${NC}"
    echo -e "Total Tests: ${BLUE}$TOTAL${NC}"
    echo -e "Passed: ${GREEN}$PASSED${NC}"
    echo -e "Failed: ${RED}$FAILED${NC}"

    if [ $FAILED -eq 0 ]; then
        echo -e "\n${GREEN}🎉 ALL STUDENT API TESTS PASSED!${NC}"
        echo -e "${GREEN}✅ Student Controller is working correctly${NC}"
        exit 0
    else
        echo -e "\n${YELLOW}⚠️  Some student API tests failed${NC}"

        # Calculate success rate
        success_rate=$((PASSED * 100 / TOTAL))
        echo -e "Success Rate: ${BLUE}${success_rate}%${NC}"

        if [ $success_rate -ge 90 ]; then
            echo -e "${GREEN}✅ Student API health: EXCELLENT${NC}"
        elif [ $success_rate -ge 80 ]; then
            echo -e "${GREEN}✅ Student API health: GOOD${NC}"
        elif [ $success_rate -ge 70 ]; then
            echo -e "${YELLOW}⚠️  Student API health: FAIR${NC}"
        else
            echo -e "${RED}❌ Student API health: POOR${NC}"
        fi

        exit 1
    fi
}

# Help function
show_help() {
    echo "Student API Test Suite"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --help, -h     Show this help message"
    echo "  --verbose, -v  Show detailed output"
    echo ""
    echo "Student APIs tested:"
    echo "  ✓ POST /api/students - Create student"
    echo "  ✓ GET /api/students/{id} - Get student by ID"
    echo "  ✓ GET /api/students/search - Search students with filters"
    echo "  ✓ PUT /api/students/{id} - Update student"
    echo ""
    echo "Test categories:"
    echo "  ✓ Student creation (valid and invalid data)"
    echo "  ✓ Student retrieval by ID"
    echo "  ✓ Student search with various filters"
    echo "  ✓ Student updates (full and partial)"
    echo "  ✓ Validation failures and error handling"
    echo "  ✓ Edge cases and boundary values"
    echo "  ✓ Concurrent operations"
    echo "  ✓ Performance tests"
}

# Parse command line arguments
case "$1" in
    --help|-h)
        show_help
        exit 0
        ;;
    --verbose|-v)
        set -x
        main
        ;;
    *)
        main
        ;;
esac
