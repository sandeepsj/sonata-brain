#!/bin/bash

# Payment System Comprehensive Test Script
# Creates test data and runs all payment API tests with pass/fail reporting

BASE_URL="http://localhost:8080"
PASSED=0
FAILED=0
TOTAL=0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
    local test_passed=false

    if [ "$http_status" = "$expected_status" ]; then
        if [ -z "$validate_response" ] || eval "$validate_response"; then
            test_passed=true
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
    if [ ${#response} -gt 200 ]; then
        echo "Response: ${response:0:200}..."
    else
        echo "Response: $response"
    fi
}

# Function to create test data
create_test_data() {
    echo -e "${YELLOW}🏗️  Creating Test Data...${NC}"

    # Create test courses
    echo "Creating test courses..."
    curl -s -X POST "$BASE_URL/api/courses" \
        -H "Content-Type: application/json" \
        -d '{"name": "Mathematics"}' > /dev/null

    curl -s -X POST "$BASE_URL/api/courses" \
        -H "Content-Type: application/json" \
        -d '{"name": "Physics"}' > /dev/null

    curl -s -X POST "$BASE_URL/api/courses" \
        -H "Content-Type: application/json" \
        -d '{"name": "Chemistry"}' > /dev/null

    echo "Courses created: Mathematics, Physics, Chemistry"

    # Create test students
    echo "Creating test students..."
    curl -s -X POST "$BASE_URL/api/students" \
        -H "Content-Type: application/json" \
        -d '{
            "studentName": "Test Student 1",
            "contactNo": "9876543210",
            "dob": "2010-05-15",
            "className": "10A",
            "school": "Test School",
            "address": "Test Address 1",
            "pinCode": 560001,
            "remarks": "Test student for payment testing"
        }' > /dev/null

    curl -s -X POST "$BASE_URL/api/students" \
        -H "Content-Type: application/json" \
        -d '{
            "studentName": "Test Student 2",
            "contactNo": "9876543211",
            "dob": "2011-03-20",
            "className": "9B",
            "school": "Test School",
            "address": "Test Address 2",
            "pinCode": 560002,
            "remarks": "Second test student"
        }' > /dev/null

    echo "Students created: Test Student 1, Test Student 2"

    # Create fee structures
    echo "Creating fee structures..."
    curl -s -X POST "$BASE_URL/api/fee-structures" \
        -H "Content-Type: application/json" \
        -d '{
            "courseName": "Mathematics",
            "year": 2025,
            "term": 1,
            "amount": 10000.00
        }' > /dev/null

    curl -s -X POST "$BASE_URL/api/fee-structures" \
        -H "Content-Type: application/json" \
        -d '{
            "courseName": "Physics",
            "year": 2025,
            "term": 1,
            "amount": 12000.00
        }' > /dev/null

    echo "Fee structures created for Mathematics (₹10,000) and Physics (₹12,000)"

    # Create student sessions
    echo "Creating student sessions..."
    curl -s -X POST "$BASE_URL/api/student-sessions" \
        -H "Content-Type: application/json" \
        -d '{
            "studentId": 1,
            "courseName": "Mathematics",
            "dateOfJoining": "2025-01-01T00:00:00"
        }' > /dev/null

    curl -s -X POST "$BASE_URL/api/student-sessions" \
        -H "Content-Type: application/json" \
        -d '{
            "studentId": 2,
            "courseName": "Physics",
            "dateOfJoining": "2025-01-01T00:00:00"
        }' > /dev/null

    echo "Student sessions created"
    echo -e "${GREEN}✅ Test data setup completed${NC}"
}

# Function to cleanup test data (optional)
cleanup_test_data() {
    echo -e "\n${YELLOW}🧹 Cleaning up test data...${NC}"
    # Note: Since delete operations are not allowed, this is just for reference
    echo "Note: Cleanup skipped as delete operations are not supported"
}

# Main test execution
main() {
    echo -e "${BLUE}🚀 Payment System Comprehensive Test Suite${NC}"
    echo -e "${BLUE}===========================================${NC}"
    echo "Testing all Payment, Deactivation, and Discount APIs"
    echo "Base URL: $BASE_URL"
    echo ""

    # Check if server is running
    echo "Checking server status..."
    server_status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/payments")
    if [ "$server_status" != "200" ]; then
        echo -e "${RED}❌ Server is not running or not responding. Please start the application first.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Server is running${NC}"

    # Create test data
    create_test_data

    echo -e "\n${YELLOW}📝 Starting API Tests...${NC}"

    # PAYMENT CREATION TESTS
    echo -e "\n${BLUE}═══ PAYMENT CREATION TESTS ═══${NC}"

    # Test 1: Simple Additional Payment
    run_test "Create Additional Payment" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Late Fee\",
            \"amountPaid\": 500.00,
            \"remarks\": \"Test additional payment\"
        }'" \
        "echo '$response' | jq -e '.id and .paymentType == \"ADDITIONAL\" and .isActive == true'"

    # Test 2: Additional Payment with Discounts
    run_test "Additional Payment with Discounts" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Library Fine\",
            \"amountPaid\": 800.00,
            \"discounts\": [{
                \"discountAmount\": 200.00,
                \"label\": \"First-time waiver\",
                \"remarks\": \"Partial waiver\"
            }]
        }'" \
        "echo '$response' | jq -e '.discountAmount == 200 and .finalAmount == 600'"

    # Test 3: Zero Amount Payment with Discounts (NEW FEATURE)
    run_test "Zero Amount Payment with Full Discount" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Full Scholarship Payment\",
            \"amountPaid\": 0.00,
            \"discounts\": [{
                \"discountAmount\": 1000.00,
                \"label\": \"Full Scholarship\",
                \"remarks\": \"100% scholarship - zero cash payment\"
            }]
        }'" \
        "echo '$response' | jq -e '.amountPaid == 0 and .discountAmount == 1000'"

    # Test 4: Regular Payment
    run_test "Create Regular Payment" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Term Fee Payment\",
            \"amountPaid\": 8000.00,
            \"termId\": 1,
            \"courseName\": \"Mathematics\",
            \"discounts\": [{
                \"discountAmount\": 1000.00,
                \"label\": \"Merit Scholarship\",
                \"remarks\": \"Academic excellence\"
            }]
        }'" \
        "echo '$response' | jq -e '.paymentType == \"REGULAR\" and .feeStructure.id == 1'"

    # Test 5: Regular Payment - Partial Amount
    run_test "Regular Payment - Second Installment" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Term Fee - Second Installment\",
            \"amountPaid\": 1000.00,
            \"termId\": 1,
            \"courseName\": \"Mathematics\",
            \"remarks\": \"Second installment payment\"
        }'" \
        "echo '$response' | jq -e '.paymentType == \"REGULAR\"'"

    # VALIDATION FAILURE TESTS
    echo -e "\n${BLUE}═══ VALIDATION FAILURE TESTS ═══${NC}"

    # Test 6: Zero Amount without Discounts (Should Fail)
    run_test "Zero Amount without Discounts (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Invalid Zero Payment\",
            \"amountPaid\": 0.00
        }'" \
        "echo '$response' | jq -e '.error == \"Validation Failed\"'"

    # Test 7: Overpayment Test (Should Fail) - NEW FEATURE
    run_test "Overpayment Validation (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Overpayment Test\",
            \"amountPaid\": 5000.00,
            \"termId\": 1,
            \"courseName\": \"Mathematics\",
            \"discounts\": [{
                \"discountAmount\": 6000.00,
                \"label\": \"Large Discount\",
                \"remarks\": \"This should cause overpayment\"
            }]
        }'" \
        "echo '$response' | jq -e '.message | contains(\"exceed the fee structure amount\")'"

    # Test 8: Invalid Student ID
    run_test "Invalid Student ID (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 999999,
            \"label\": \"Invalid Student Test\",
            \"amountPaid\": 1000.00
        }'" \
        "echo '$response' | jq -e '.message | contains(\"Student not found\")'"

    # Test 9: Invalid Course Name
    run_test "Invalid Course Name (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Invalid Course Test\",
            \"amountPaid\": 1000.00,
            \"termId\": 1,
            \"courseName\": \"NonExistentCourse\"
        }'" \
        "echo '$response' | jq -e '.message | contains(\"Course not found\")'"

    # Test 10: Additional Payment with TermId (Should Fail)
    run_test "Additional Payment with TermId (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Invalid Additional Payment\",
            \"amountPaid\": 500.00,
            \"termId\": 1
        }'" \
        "echo '$response' | jq -e '.message | contains(\"Term ID should not be provided\")'"

    # Test 11: Zero Discount Amount (Should Fail) - NEW VALIDATION
    run_test "Zero Discount Amount (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Invalid Zero Discount\",
            \"amountPaid\": 1000.00,
            \"discounts\": [{
                \"discountAmount\": 0.00,
                \"label\": \"Zero Discount\",
                \"remarks\": \"This should fail - zero discount not allowed\"
            }]
        }'" \
        "echo '$response' | jq -e '.message | contains(\"Discount amount must be positive\") or .fieldErrors.\"discounts[0].discountAmount\" | contains(\"positive\")'"

    # Test 12: Negative Discount Amount (Should Fail)
    run_test "Negative Discount Amount (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Invalid Negative Discount\",
            \"amountPaid\": 1000.00,
            \"discounts\": [{
                \"discountAmount\": -100.00,
                \"label\": \"Negative Discount\",
                \"remarks\": \"This should fail - negative discount not allowed\"
            }]
        }'" \
        "echo '$response' | jq -e '.message | contains(\"Discount amount must be positive\") or .fieldErrors.\"discounts[0].discountAmount\" | contains(\"positive\")'"

    # RETRIEVAL TESTS
    echo -e "\n${BLUE}═══ PAYMENT RETRIEVAL TESTS ═══${NC}"

    # Test 13: Get All Payments
    run_test "Get All Payments" "200" \
        "curl -s -X GET '$BASE_URL/api/payments'" \
        "echo '$response' | jq -e 'type == \"array\" and length > 0'"

    # Test 14: Get Payment by ID
    run_test "Get Payment by ID" "200" \
        "curl -s -X GET '$BASE_URL/api/payments/1'" \
        "echo '$response' | jq -e '.id == 1'"

    # Test 15: Get Payments by Student
    run_test "Get Payments by Student" "200" \
        "curl -s -X GET '$BASE_URL/api/payments/student/1'" \
        "echo '$response' | jq -e 'type == \"array\" and length > 0'"

    # Test 16: Get Active Payments
    run_test "Get Active Payments" "200" \
        "curl -s -X GET '$BASE_URL/api/payments/active'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 17: Get Payments by Type
    run_test "Get Regular Payments for Student" "200" \
        "curl -s -X GET '$BASE_URL/api/payments/student/1/type/REGULAR'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # PAYMENT DEACTIVATION TESTS
    echo -e "\n${BLUE}═══ PAYMENT DEACTIVATION TESTS ═══${NC}"

    # Test 18: Deactivate Payment
    run_test "Deactivate Payment" "200" \
        "curl -s -X PATCH '$BASE_URL/api/payments/1/deactivate'" \
        "echo '$response' | jq -e '.isActive == false'"

    # Test 19: Try to Deactivate Already Inactive Payment
    run_test "Deactivate Already Inactive Payment (Should Fail)" "400" \
        "curl -s -X PATCH '$BASE_URL/api/payments/1/deactivate'" \
        "echo '$response' | jq -e '.message | contains(\"already deactivated\")'"

    # Test 20: Deactivate Non-existent Payment
    run_test "Deactivate Non-existent Payment (Should Fail)" "404" \
        "curl -s -X PATCH '$BASE_URL/api/payments/999999/deactivate'" \
        "echo '$response' | jq -e '.message | contains(\"not found\")'"

    # Test 21: Verify Inactive Payment in Lists
    run_test "Get Inactive Payments" "200" \
        "curl -s -X GET '$BASE_URL/api/payments/inactive'" \
        "echo '$response' | jq -e 'type == \"array\" and length > 0'"

    # DISCOUNT TESTS
    echo -e "\n${BLUE}═══ DISCOUNT API TESTS ═══${NC}"

    # Test 22: Get All Discounts
    run_test "Get All Discounts" "200" \
        "curl -s -X GET '$BASE_URL/api/discounts'" \
        "echo '$response' | jq -e 'type == \"array\" and length > 0'"

    # Test 23: Get Discounts by Payment ID
    run_test "Get Discounts by Payment ID" "200" \
        "curl -s -X GET '$BASE_URL/api/discounts/payment/2'" \
        "echo '$response' | jq -e 'type == \"array\"'"

    # Test 24: Get Discount by ID
    run_test "Get Discount by ID" "200" \
        "curl -s -X GET '$BASE_URL/api/discounts/1'" \
        "echo '$response' | jq -e '.id == 1'"

    # FORBIDDEN OPERATIONS TESTS
    echo -e "\n${BLUE}═══ FORBIDDEN OPERATIONS TESTS ═══${NC}"

    # Test 25: Try to Update Payment (Should Fail)
    run_test "Update Payment (Should Fail - Method Not Allowed)" "404" \
        "curl -s -X PUT '$BASE_URL/api/payments/1' -H 'Content-Type: application/json' -d '{
            \"label\": \"Updated Payment\"
        }'" \
        ""

    # Test 26: Try to Delete Payment (Should Fail)
    run_test "Delete Payment (Should Fail - Method Not Allowed)" "404" \
        "curl -s -X DELETE '$BASE_URL/api/payments/1'" \
        ""

    # Test 27: Try to Delete Discount (Should Fail)
    run_test "Delete Discount (Should Fail - Method Not Allowed)" "404" \
        "curl -s -X DELETE '$BASE_URL/api/discounts/1'" \
        ""

    # ADVANCED SCENARIO TESTS
    echo -e "\n${BLUE}═══ ADVANCED SCENARIO TESTS ═══${NC}"

    # Test 28: Multiple Payments for Same Term (Testing Amount Limits)
    run_test "Create Second Payment for Same Term" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 2,
            \"label\": \"First Term Payment\",
            \"amountPaid\": 6000.00,
            \"termId\": 2,
            \"courseName\": \"Physics\"
        }'" \
        "echo '$response' | jq -e '.paymentType == \"REGULAR\"'"

    # Test 29: Payment that Would Exceed Limit
    run_test "Payment Exceeding Term Limit (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 2,
            \"label\": \"Overpayment Test\",
            \"amountPaid\": 7000.00,
            \"termId\": 2,
            \"courseName\": \"Physics\"
        }'" \
        "echo '$response' | jq -e '.message | contains(\"exceed the fee structure amount\")'"

    # Test 30: Zero Payment with Multiple Discounts
    run_test "Zero Payment with Multiple Discounts" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 2,
            \"label\": \"Full Scholarship - Multiple Sources\",
            \"amountPaid\": 0.00,
            \"discounts\": [
                {
                    \"discountAmount\": 3000.00,
                    \"label\": \"Merit Scholarship\",
                    \"remarks\": \"Academic excellence\"
                },
                {
                    \"discountAmount\": 2000.00,
                    \"label\": \"Sports Quota\",
                    \"remarks\": \"Athletic achievement\"
                }
            ]
        }'" \
        "echo '$response' | jq -e '.amountPaid == 0 and .discountAmount == 5000'"

    # Test 31: Complex Regular Payment with Amount + Discount Validation
    run_test "Complex Regular Payment with Amount+Discount Check" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 2,
            \"label\": \"Remaining Term Fee\",
            \"amountPaid\": 1000.00,
            \"termId\": 2,
            \"courseName\": \"Physics\",
            \"discounts\": [{
                \"discountAmount\": 1000.00,
                \"label\": \"Final Discount\",
                \"remarks\": \"Completing the term payment\"
            }]
        }'" \
        "echo '$response' | jq -e '.paymentType == \"REGULAR\"'"

    # Test 32: Payment that Would Exceed with Discounts (Should Fail)
    run_test "Payment+Discounts Exceeding Limit (Should Fail)" "400" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 2,
            \"label\": \"Over-limit Payment\",
            \"amountPaid\": 1000.00,
            \"termId\": 2,
            \"courseName\": \"Physics\",
            \"discounts\": [{
                \"discountAmount\": 5000.00,
                \"label\": \"Large Discount\",
                \"remarks\": \"This should exceed the limit\"
            }]
        }'" \
        "echo '$response' | jq -e '.message | contains(\"exceed the fee structure amount\")'"

    # EDGE CASE TESTS
    echo -e "\n${BLUE}═══ EDGE CASE TESTS ═══${NC}"

    # Test 33: Large Amount Payment
    run_test "Large Amount Payment" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Large Amount Test\",
            \"amountPaid\": 99999.99
        }'" \
        "echo '$response' | jq -e '.amountPaid == 99999.99'"

    # Test 34: Special Characters in Fields
    run_test "Special Characters in Fields" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Special Chars: !@#$%^&*()\",
            \"amountPaid\": 100.00,
            \"remarks\": \"Testing special characters\"
        }'" \
        "echo '$response' | jq -e '.label | contains(\"Special Chars\")'"

    # Test 35: Payment with Future Date
    run_test "Payment with Future Date" "201" \
        "curl -s -X POST '$BASE_URL/api/payments' -H 'Content-Type: application/json' -d '{
            \"studentId\": 1,
            \"label\": \"Future Dated Payment\",
            \"amountPaid\": 500.00,
            \"date\": \"2025-12-31T23:59:59\",
            \"remarks\": \"Payment with future date\"
        }'" \
        "echo '$response' | jq -e '.date | contains(\"2025-12-31\")'"

    # COMPREHENSIVE WORKFLOW TESTS
    echo -e "\n${BLUE}═══ WORKFLOW TESTS ═══${NC}"

    # Test 36: Complete Payment Lifecycle
    echo -e "\n${YELLOW}Running Complete Payment Lifecycle Test...${NC}"

    # Create payment for lifecycle test
    lifecycle_response=$(curl -s -X POST "$BASE_URL/api/payments" \
        -H "Content-Type: application/json" \
        -d '{
            "studentId": 1,
            "label": "Lifecycle Test Payment",
            "amountPaid": 750.00,
            "discounts": [{
                "discountAmount": 250.00,
                "label": "Lifecycle Discount",
                "remarks": "Testing complete lifecycle"
            }]
        }')

    lifecycle_payment_id=$(echo "$lifecycle_response" | jq -r '.id')

    if [ "$lifecycle_payment_id" != "null" ] && [ "$lifecycle_payment_id" != "" ]; then
        echo "Lifecycle payment created with ID: $lifecycle_payment_id"

        # Test deactivation
        run_test "Lifecycle - Deactivate Payment" "200" \
            "curl -s -X PATCH '$BASE_URL/api/payments/$lifecycle_payment_id/deactivate'" \
            "echo '$response' | jq -e '.isActive == false'"

        # Test that discounts still exist
        run_test "Lifecycle - Verify Discounts After Deactivation" "200" \
            "curl -s -X GET '$BASE_URL/api/discounts/payment/$lifecycle_payment_id'" \
            "echo '$response' | jq -e 'type == \"array\" and length > 0'"
    else
        echo -e "${RED}❌ Could not create lifecycle payment${NC}"
        FAILED=$((FAILED + 2))
        TOTAL=$((TOTAL + 2))
    fi

    # REPORTING
    echo -e "\n${BLUE}═══ TEST SUMMARY ═══${NC}"
    echo -e "Total Tests: ${BLUE}$TOTAL${NC}"
    echo -e "Passed: ${GREEN}$PASSED${NC}"
    echo -e "Failed: ${RED}$FAILED${NC}"

    if [ $FAILED -eq 0 ]; then
        echo -e "\n${GREEN}🎉 ALL TESTS PASSED! Payment system is working correctly.${NC}"
        exit 0
    else
        echo -e "\n${YELLOW}⚠️  Some tests failed. Check the output above for details.${NC}"

        # Calculate success rate
        success_rate=$((PASSED * 100 / TOTAL))
        echo -e "Success Rate: ${BLUE}${success_rate}%${NC}"

        if [ $success_rate -ge 80 ]; then
            echo -e "${GREEN}✅ Overall system health: GOOD${NC}"
        elif [ $success_rate -ge 60 ]; then
            echo -e "${YELLOW}⚠️  Overall system health: FAIR${NC}"
        else
            echo -e "${RED}❌ Overall system health: POOR${NC}"
        fi

        exit 1
    fi
}

# Help function
show_help() {
    echo "Payment System Test Script"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --help, -h     Show this help message"
    echo "  --no-setup     Skip test data creation"
    echo "  --cleanup      Run cleanup after tests (if supported)"
    echo ""
    echo "Features tested:"
    echo "  ✓ Payment creation (additional and regular)"
    echo "  ✓ Zero amount payments with discounts"
    echo "  ✓ Amount + discount validation against fee structure"
    echo "  ✓ Payment deactivation (one-way)"
    echo "  ✓ Discount management"
    echo "  ✓ Validation failures and error handling"
    echo "  ✓ Edge cases and workflows"
}

# Parse command line arguments
case "$1" in
    --help|-h)
        show_help
        exit 0
        ;;
    --no-setup)
        echo "Skipping test data creation..."
        # Skip create_test_data call in main
        ;;
    --cleanup)
        cleanup_test_data
        exit 0
        ;;
    *)
        main
        ;;
esac
