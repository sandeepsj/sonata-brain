#!/bin/bash

# Master Test Runner - Runs all API test suites
# Usage: ./run_all_tests.sh [--module=student|course|payment|all]

BASE_DIR="/home/sandeepsj/Developer/sonata-brain"
TEST_DIR="$BASE_DIR/tests/api-tests"
REPORTS_DIR="$BASE_DIR/tests/reports"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Create reports directory if it doesn't exist
mkdir -p "$REPORTS_DIR"

# Function to run individual test suite
run_test_suite() {
    local suite_name="$1"
    local script_path="$2"

    echo -e "\n${PURPLE}🚀 Running $suite_name Test Suite${NC}"
    echo -e "${PURPLE}$(printf '=%.0s' {1..50})${NC}"

    if [ -f "$script_path" ]; then
        if bash "$script_path"; then
            echo -e "${GREEN}✅ $suite_name tests PASSED${NC}"
            return 0
        else
            echo -e "${RED}❌ $suite_name tests FAILED${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠️  $suite_name test script not found: $script_path${NC}"
        return 1
    fi
}

# Function to check server status
check_server() {
    echo -e "${BLUE}🔍 Checking server status...${NC}"

    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/students/search" | grep -q "200"; then
        echo -e "${GREEN}✅ Server is running and responding${NC}"
        return 0
    else
        echo -e "${RED}❌ Server is not running or not responding${NC}"
        echo "Please start the application with: ./mvnw spring-boot:run"
        return 1
    fi
}

# Main execution function
main() {
    local module="${1:-all}"
    local total_suites=0
    local passed_suites=0

    echo -e "${BLUE}🧪 Sonata Brain API Test Suite Runner${NC}"
    echo -e "${BLUE}===================================${NC}"
    echo "Test Directory: $TEST_DIR"
    echo "Reports Directory: $REPORTS_DIR"
    echo "Module: $module"
    echo ""

    # Check server status first
    if ! check_server; then
        exit 1
    fi

    echo -e "\n${YELLOW}📋 Available Test Suites:${NC}"

    # Run tests based on module selection
    case "$module" in
        "student"|"all")
            total_suites=$((total_suites + 1))
            if run_test_suite "Student API" "$TEST_DIR/student_api_tests.sh"; then
                passed_suites=$((passed_suites + 1))
            fi
            ;;
    esac

    case "$module" in
        "course"|"all")
            total_suites=$((total_suites + 1))
            if run_test_suite "Course API" "$TEST_DIR/course_api_tests.sh"; then
                passed_suites=$((passed_suites + 1))
            fi
            ;;
    esac

    case "$module" in
        "payment"|"all")
            total_suites=$((total_suites + 1))
            if run_test_suite "Payment API" "$TEST_DIR/payment_api_tests.sh"; then
                passed_suites=$((passed_suites + 1))
            fi
            ;;
    esac

    case "$module" in
        "fee-structure"|"all")
            total_suites=$((total_suites + 1))
            if run_test_suite "Fee Structure API" "$TEST_DIR/fee_structure_api_tests.sh"; then
                passed_suites=$((passed_suites + 1))
            fi
            ;;
    esac

    # Generate summary report
    echo -e "\n${PURPLE}═══ OVERALL TEST SUMMARY ═══${NC}"
    echo -e "Test Suites Run: ${BLUE}$total_suites${NC}"
    echo -e "Suites Passed: ${GREEN}$passed_suites${NC}"
    echo -e "Suites Failed: ${RED}$((total_suites - passed_suites))${NC}"

    if [ $passed_suites -eq $total_suites ]; then
        echo -e "\n${GREEN}🎉 ALL TEST SUITES PASSED!${NC}"
        echo -e "${GREEN}✅ Sonata Brain API system is healthy${NC}"
        exit 0
    else
        success_rate=$((passed_suites * 100 / total_suites))
        echo -e "\nOverall Success Rate: ${BLUE}${success_rate}%${NC}"

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
    echo "Sonata Brain API Test Suite Runner"
    echo ""
    echo "Usage: $0 [--module=MODULE]"
    echo ""
    echo "Modules:"
    echo "  student      Run only Student API tests"
    echo "  course       Run only Course API tests"
    echo "  payment      Run only Payment API tests"
    echo "  fee-structure Run only Fee Structure API tests"
    echo "  all          Run all test suites (default)"
    echo ""
    echo "Examples:"
    echo "  $0                      # Run all tests"
    echo "  $0 --module=student     # Run only student tests"
    echo "  $0 --module=payment     # Run only payment tests"
    echo ""
    echo "Prerequisites:"
    echo "  - Application must be running on localhost:8080"
    echo "  - jq must be installed for JSON parsing"
    echo "  - curl must be available"
}

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        --module=*)
            MODULE="${arg#*=}"
            shift
            ;;
        --help|-h)
            show_help
            exit 0
            ;;
        *)
            echo "Unknown option: $arg"
            show_help
            exit 1
            ;;
    esac
done

# Run main with module parameter
main "${MODULE:-all}"
