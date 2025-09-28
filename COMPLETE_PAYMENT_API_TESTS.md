# Payment System API Test Cases - Complete cURL Sheet

## Prerequisites
- Application running on localhost:8080
- Ensure you have valid test data: students, courses, fee structures, and student sessions

## 🚀 PAYMENT CREATION APIS

### 1.1 Create Additional Payment (Simple - Most Likely to Work)
```bash
# Test basic additional payment creation
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Late Fee",
    "amountPaid": 500.00,
    "remarks": "Test additional payment"
  }'
```

### 1.2 Create Additional Payment with Discounts
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Library Fine",
    "amountPaid": 800.00,
    "remarks": "Book return delay fine",
    "discounts": [
      {
        "discountAmount": 200.00,
        "label": "First-time waiver",
        "remarks": "Partial waiver for first offense"
      }
    ]
  }'
```

### 1.3 Create Additional Payment with Course Name
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Exam Re-attempt Fee",
    "amountPaid": 1500.00,
    "courseName": "Mathematics",
    "remarks": "Re-examination fee",
    "discounts": [
      {
        "discountAmount": 300.00,
        "label": "Student discount",
        "remarks": "Regular student discount"
      }
    ]
  }'
```

### 1.4 Create Additional Payment with Multiple Discounts
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 2,
    "label": "Activity Fee",
    "amountPaid": 2000.00,
    "remarks": "Sports and cultural activities",
    "discounts": [
      {
        "discountAmount": 300.00,
        "label": "Sports Achievement",
        "remarks": "Winner of inter-school competition"
      },
      {
        "discountAmount": 200.00,
        "label": "Early Bird",
        "remarks": "Paid before deadline"
      }
    ]
  }'
```

### 1.5 Create Regular Payment (Requires Valid Term and Course)
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 8000.00,
    "termId": 1,
    "courseName": "Mathematics",
    "remarks": "Regular term fee payment",
    "discounts": [
      {
        "discountAmount": 1000.00,
        "label": "Merit Scholarship",
        "remarks": "Academic excellence discount"
      }
    ]
  }'
```

### 1.6 Create Regular Payment with Partial Amount
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 2,
    "label": "Term Fee - First Installment",
    "amountPaid": 5000.00,
    "termId": 2,
    "courseName": "Physics",
    "remarks": "First installment of term fee"
  }'
```

### 1.7 Create Payment with Custom Date
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Backdated Payment",
    "amountPaid": 1200.00,
    "date": "2025-09-15T10:30:00",
    "remarks": "Payment made earlier but recorded now"
  }'
```

## ❌ VALIDATION FAILURE TEST CASES

### 2.1 Invalid Student ID
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 999999,
    "label": "Invalid Student Test",
    "amountPaid": 1000.00
  }'
```

### 2.2 Missing Required Fields
```bash
# Missing label
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "amountPaid": 1000.00
  }'

# Missing amountPaid
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Test Payment"
  }'

# Missing studentId
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "label": "Test Payment",
    "amountPaid": 1000.00
  }'
```

### 2.3 Invalid Amount Values
```bash
# Negative amount
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Negative Amount Test",
    "amountPaid": -500.00
  }'

# Zero amount
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Zero Amount Test",
    "amountPaid": 0.00
  }'
```

### 2.4 Regular Payment Validation Failures
```bash
# Regular payment without termId
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 10000.00,
    "courseName": "Mathematics"
  }'

# Regular payment without courseName
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 10000.00,
    "termId": 1
  }'

# Invalid course name
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 10000.00,
    "termId": 1,
    "courseName": "NonExistentCourse"
  }'

# Invalid term ID
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 10000.00,
    "termId": 999999,
    "courseName": "Mathematics"
  }'
```

### 2.5 Additional Payment with Term ID (Should Fail)
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Late Fee",
    "amountPaid": 500.00,
    "termId": 1,
    "remarks": "This should fail - additional payment with termId"
  }'
```

### 2.6 Overpayment Test (Should Fail if Total Exceeds Fee Structure)
```bash
# Try to create a payment that would exceed the fee structure amount
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Overpayment Test",
    "amountPaid": 999999.00,
    "termId": 1,
    "courseName": "Mathematics",
    "remarks": "This should fail if total exceeds fee structure amount"
  }'
```

## 📄 PAYMENT RETRIEVAL APIS

### 3.1 Get All Payments
```bash
curl -i -X GET http://localhost:8080/api/payments
```

### 3.2 Get Payment by ID
```bash
# Replace {id} with actual payment ID from previous tests
curl -i -X GET http://localhost:8080/api/payments/1
curl -i -X GET http://localhost:8080/api/payments/2
curl -i -X GET http://localhost:8080/api/payments/3

# Test non-existent payment ID
curl -i -X GET http://localhost:8080/api/payments/999999
```

### 3.3 Get Payments by Student ID
```bash
# Get all payments for student 1
curl -i -X GET http://localhost:8080/api/payments/student/1

# Get all payments for student 2
curl -i -X GET http://localhost:8080/api/payments/student/2

# Test non-existent student
curl -i -X GET http://localhost:8080/api/payments/student/999999
```

### 3.4 Get Payments by Student and Type
```bash
# Get regular payments for student 1
curl -i -X GET http://localhost:8080/api/payments/student/1/type/REGULAR

# Get additional payments for student 1
curl -i -X GET http://localhost:8080/api/payments/student/1/type/ADDITIONAL

# Test with different student
curl -i -X GET http://localhost:8080/api/payments/student/2/type/REGULAR
curl -i -X GET http://localhost:8080/api/payments/student/2/type/ADDITIONAL
```

### 3.5 Get Payments by Term ID
```bash
# Get all payments for term 1
curl -i -X GET http://localhost:8080/api/payments/term/1

# Get all payments for term 2
curl -i -X GET http://localhost:8080/api/payments/term/2

# Test non-existent term
curl -i -X GET http://localhost:8080/api/payments/term/999999
```

### 3.6 Get Active/Inactive Payments
```bash
# Get all active payments
curl -i -X GET http://localhost:8080/api/payments/active

# Get all inactive payments
curl -i -X GET http://localhost:8080/api/payments/inactive
```

### 3.7 Get Payments by Student and Status
```bash
# Get active payments for student 1
curl -i -X GET http://localhost:8080/api/payments/student/1/active

# Get payments by student and specific status
curl -i -X GET http://localhost:8080/api/payments/student/1/status/true
curl -i -X GET http://localhost:8080/api/payments/student/1/status/false

# Test with different students
curl -i -X GET http://localhost:8080/api/payments/student/2/active
```

## 🔄 PAYMENT DEACTIVATION APIS

### 4.1 Successful Payment Deactivation
```bash
# Step 1: Create a payment first
PAYMENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Payment to Deactivate",
    "amountPaid": 1000.00,
    "remarks": "This payment will be deactivated"
  }')

echo "Created payment: $PAYMENT_RESPONSE"

# Step 2: Extract payment ID (manually or with jq)
# PAYMENT_ID=$(echo $PAYMENT_RESPONSE | jq -r '.id')

# Step 3: Deactivate the payment (replace 1 with actual ID)
curl -i -X PATCH http://localhost:8080/api/payments/1/deactivate
```

### 4.2 Try to Deactivate Non-existent Payment
```bash
curl -i -X PATCH http://localhost:8080/api/payments/999999/deactivate
```

### 4.3 Try to Deactivate Already Inactive Payment
```bash
# First deactivate a payment, then try again
curl -i -X PATCH http://localhost:8080/api/payments/1/deactivate
# Run the same command again - should fail
curl -i -X PATCH http://localhost:8080/api/payments/1/deactivate
```

### 4.4 Verify Payment Status After Deactivation
```bash
# Check that isActive is now false
curl -i -X GET http://localhost:8080/api/payments/1 | jq '.isActive'

# Verify payment appears in inactive list
curl -i -X GET http://localhost:8080/api/payments/inactive

# Verify payment doesn't appear in active list
curl -i -X GET http://localhost:8080/api/payments/active
```

### 4.5 Invalid HTTP Methods on Deactivate Endpoint
```bash
# These should fail with 405 Method Not Allowed
curl -i -X GET http://localhost:8080/api/payments/1/deactivate
curl -i -X POST http://localhost:8080/api/payments/1/deactivate -H "Content-Type: application/json" -d '{}'
curl -i -X PUT http://localhost:8080/api/payments/1/deactivate -H "Content-Type: application/json" -d '{}'
curl -i -X DELETE http://localhost:8080/api/payments/1/deactivate
```

## 💰 DISCOUNT APIS

### 5.1 Get All Discounts
```bash
curl -i -X GET http://localhost:8080/api/discounts
```

### 5.2 Get Discount by ID
```bash
# Test with various discount IDs (replace with actual IDs from your data)
curl -i -X GET http://localhost:8080/api/discounts/1
curl -i -X GET http://localhost:8080/api/discounts/2
curl -i -X GET http://localhost:8080/api/discounts/3

# Test non-existent discount ID
curl -i -X GET http://localhost:8080/api/discounts/999999
```

### 5.3 Get Discounts by Payment ID
```bash
# Get discounts for specific payments (replace with actual payment IDs)
curl -i -X GET http://localhost:8080/api/discounts/payment/1
curl -i -X GET http://localhost:8080/api/discounts/payment/2
curl -i -X GET http://localhost:8080/api/discounts/payment/3

# Test non-existent payment ID
curl -i -X GET http://localhost:8080/api/discounts/payment/999999
```

### 5.4 Verify Discounts After Payment Creation
```bash
# Create payment with discounts, then verify discounts exist
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Payment with Discounts Test",
    "amountPaid": 3000.00,
    "discounts": [
      {
        "discountAmount": 500.00,
        "label": "Merit Scholarship",
        "remarks": "Academic performance"
      },
      {
        "discountAmount": 300.00,
        "label": "Sports Quota",
        "remarks": "Athletic achievement"
      }
    ]
  }'

# Then check if discounts were created (replace payment ID)
curl -X GET http://localhost:8080/api/discounts/payment/1
```

### 5.5 Verify Discounts Remain After Payment Deactivation
```bash
# Step 1: Create payment with discounts
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Payment to Test Discount Persistence",
    "amountPaid": 2000.00,
    "discounts": [
      {
        "discountAmount": 400.00,
        "label": "Test Discount",
        "remarks": "Testing discount persistence after deactivation"
      }
    ]
  }'

# Step 2: Deactivate the payment (replace with actual ID)
curl -X PATCH http://localhost:8080/api/payments/1/deactivate

# Step 3: Verify discounts still exist and are accessible
curl -X GET http://localhost:8080/api/discounts/payment/1
```

## 🚫 FORBIDDEN OPERATIONS TEST CASES

### 6.1 Verify Update Operations Are Not Allowed
```bash
# These should return 404 - Method Not Found
curl -i -X PUT http://localhost:8080/api/payments/1 \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Updated Payment",
    "amountPaid": 1500.00
  }'

curl -i -X PATCH http://localhost:8080/api/payments/1 \
  -H "Content-Type: application/json" \
  -d '{
    "label": "Updated Label"
  }'
```

### 6.2 Verify Delete Operations Are Not Allowed
```bash
# These should return 404 - Method Not Found
curl -i -X DELETE http://localhost:8080/api/payments/1
curl -i -X DELETE http://localhost:8080/api/discounts/1
```

## 🧪 ADVANCED TEST SCENARIOS

### 7.1 Complete Payment Lifecycle Test
```bash
#!/bin/bash
echo "=== Payment Lifecycle Test ==="

# Step 1: Check initial counts
echo "Step 1: Getting initial payment counts..."
TOTAL_BEFORE=$(curl -s http://localhost:8080/api/payments | jq '. | length')
ACTIVE_BEFORE=$(curl -s http://localhost:8080/api/payments/active | jq '. | length')
INACTIVE_BEFORE=$(curl -s http://localhost:8080/api/payments/inactive | jq '. | length')

echo "Initial counts - Total: $TOTAL_BEFORE, Active: $ACTIVE_BEFORE, Inactive: $INACTIVE_BEFORE"

# Step 2: Create new payment
echo "Step 2: Creating new payment..."
PAYMENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Lifecycle Test Payment",
    "amountPaid": 1500.00,
    "discounts": [{
      "discountAmount": 100.00,
      "label": "Test Discount",
      "remarks": "Lifecycle test discount"
    }]
  }')

echo "Payment created: $PAYMENT_RESPONSE"

# Step 3: Verify counts increased
echo "Step 3: Verifying payment counts after creation..."
TOTAL_AFTER_CREATE=$(curl -s http://localhost:8080/api/payments | jq '. | length')
ACTIVE_AFTER_CREATE=$(curl -s http://localhost:8080/api/payments/active | jq '. | length')

echo "After creation - Total: $TOTAL_AFTER_CREATE, Active: $ACTIVE_AFTER_CREATE"

# Step 4: Deactivate payment (replace 1 with actual ID)
echo "Step 4: Deactivating payment..."
curl -s -X PATCH http://localhost:8080/api/payments/1/deactivate

# Step 5: Verify final counts
echo "Step 5: Verifying final counts..."
TOTAL_FINAL=$(curl -s http://localhost:8080/api/payments | jq '. | length')
ACTIVE_FINAL=$(curl -s http://localhost:8080/api/payments/active | jq '. | length')
INACTIVE_FINAL=$(curl -s http://localhost:8080/api/payments/inactive | jq '. | length')

echo "Final counts - Total: $TOTAL_FINAL, Active: $ACTIVE_FINAL, Inactive: $INACTIVE_FINAL"

# Step 6: Verify discounts still exist
echo "Step 6: Checking discounts for deactivated payment..."
curl -s http://localhost:8080/api/discounts/payment/1
```

### 7.2 Multiple Payments for Same Term Test
```bash
# Test the overpayment validation
echo "Creating first payment for term..."
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "First Term Payment",
    "amountPaid": 6000.00,
    "termId": 1,
    "courseName": "Mathematics"
  }'

echo "Creating second payment for same term..."
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Second Term Payment",
    "amountPaid": 3000.00,
    "termId": 1,
    "courseName": "Mathematics"
  }'

echo "Attempting third payment that might exceed limit..."
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Third Term Payment - Should Fail",
    "amountPaid": 5000.00,
    "termId": 1,
    "courseName": "Mathematics"
  }'
```

### 7.3 Discount Calculation Verification
```bash
# Create payment with multiple discounts and verify calculations
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Discount Calculation Test",
    "amountPaid": 5000.00,
    "discounts": [
      {
        "discountAmount": 1000.00,
        "label": "Scholarship",
        "remarks": "Academic merit"
      },
      {
        "discountAmount": 500.00,
        "label": "Early Payment",
        "remarks": "Paid before deadline"
      },
      {
        "discountAmount": 200.00,
        "label": "Sibling Discount",
        "remarks": "Multiple children in school"
      }
    ]
  }' | jq '{id: .id, amountPaid: .amountPaid, discountAmount: .discountAmount, finalAmount: .finalAmount}'

# Verify individual discounts
curl -X GET http://localhost:8080/api/discounts/payment/1 | jq '[.[] | {label: .label, amount: .discountAmount}]'
```

## 🔧 EDGE CASES AND STRESS TESTS

### 8.1 Large Amount Values
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Large Amount Test",
    "amountPaid": 999999.99
  }'
```

### 8.2 Special Characters in Text Fields
```bash
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Special Chars: !@#$%^&*()_+-=[]{}|;:,.<>?",
    "amountPaid": 1000.00,
    "remarks": "Testing special characters: \"quotes\", \u0027apostrophes\u0027, \\backslashes\\"
  }'
```

### 8.3 Empty and Null Values
```bash
# Empty strings
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "",
    "amountPaid": 1000.00
  }'

# Null values in optional fields
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Null Test",
    "amountPaid": 1000.00,
    "remarks": null,
    "courseName": null
  }'
```

### 8.4 Concurrent Payment Creation
```bash
# Create multiple payments simultaneously
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "label": "Concurrent 1", "amountPaid": 1000.00}' &

curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "label": "Concurrent 2", "amountPaid": 1000.00}' &

curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "label": "Concurrent 3", "amountPaid": 1000.00}' &

wait
echo "All concurrent requests completed"
```

## 📊 DATA VERIFICATION TESTS

### 9.1 Check Response Headers and Status Codes
```bash
# Verify correct HTTP status codes
curl -i -X GET http://localhost:8080/api/payments | grep "HTTP"
curl -i -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "label": "Test", "amountPaid": 1000.00}' | grep "HTTP"
```

### 9.2 Response Format Validation
```bash
# Verify JSON structure
curl -s -X GET http://localhost:8080/api/payments | jq 'type'
curl -s -X GET http://localhost:8080/api/payments/1 | jq 'keys'
curl -s -X GET http://localhost:8080/api/discounts | jq 'type'
```

### 9.3 Pagination and Large Dataset Tests
```bash
# Create multiple payments to test performance
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/payments \
    -H "Content-Type: application/json" \
    -d "{
      \"studentId\": 1,
      \"label\": \"Bulk Payment $i\",
      \"amountPaid\": $(( $i * 100 )).00,
      \"remarks\": \"Performance test payment $i\"
    }" > /dev/null
  echo "Created payment $i"
done

echo "Getting all payments..."
curl -s -X GET http://localhost:8080/api/payments | jq '. | length'
```

## 🛠️ DEBUGGING AND DATA EXPLORATION

### 10.1 Check Available Data
```bash
# Use debug endpoints to see what data exists
curl -i -X GET http://localhost:8080/api/debug/courses
curl -i -X GET http://localhost:8080/api/debug/fee-structures  
curl -i -X GET http://localhost:8080/api/debug/student-sessions
curl -i -X GET http://localhost:8080/api/debug/student-sessions/student/1
```

### 10.2 Test Logging System
```bash
# Test the logging system
curl -i -X POST http://localhost:8080/api/logging/test

# Check current logging strategy
curl -i -X GET http://localhost:8080/api/logging/strategy

# Switch logging strategy
curl -i -X POST http://localhost:8080/api/logging/strategy/kafka
```

## 📝 QUICK REFERENCE COMMANDS

### Essential Test Sequence (Run these first)
```bash
# 1. Check server status
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/payments
echo " - Server status"

# 2. Create simple additional payment
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "label": "Quick Test", "amountPaid": 500.00}'

# 3. Check all payments
curl -X GET http://localhost:8080/api/payments

# 4. Test deactivation
curl -X PATCH http://localhost:8080/api/payments/1/deactivate

# 5. Check discounts
curl -X GET http://localhost:8080/api/discounts
```

### Debug Failed Payments
```bash
# If payment creation fails, check:
curl -X GET http://localhost:8080/api/debug/courses | jq '.[] | .name'
curl -X GET http://localhost:8080/api/debug/fee-structures | jq '.[] | {id: .id, course: .course.name, year: .year, term: .term, amount: .amount}'
```

## 📋 EXPECTED RESPONSES

### Successful Payment Creation (201 Created)
```json
{
  "id": 1,
  "student": {...},
  "date": "2025-09-28T...",
  "label": "Test Payment",
  "amountPaid": 1000.00,
  "discountAmount": 100.00,
  "paymentType": "ADDITIONAL",
  "isActive": true,
  "finalAmount": 900.00,
  ...
}
```

### Validation Error (400 Bad Request)
```json
{
  "error": "Validation Failed",
  "message": "Course not found: Computer Science",
  "timestamp": "2025-09-28T...",
  "status": 400
}
```

### Payment Not Found (404 Not Found)
```json
{
  "error": "Payment Not Found", 
  "message": "Payment not found with ID: 999999",
  "timestamp": "2025-09-28T...",
  "status": 404
}
```

## Notes:
- Replace placeholder IDs with actual values from your database
- Some tests require existing data (students, courses, fee structures)
- Monitor console logs for detailed debugging information
- Use `jq` for better JSON parsing if available
- The `-i` flag shows response headers including status codes
