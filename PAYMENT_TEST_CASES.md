# Payment System Test Cases - cURL Commands

## Prerequisites
- Application running on localhost:8080
- Database should have sample data for students, courses, fee structures, and student sessions

## 1. PAYMENT API TEST CASES

### 1.1 Create Regular Payment (Successful)
```bash
# Create a regular payment for a student with term ID and discounts
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term 3 Fee Payment",
    "amountPaid": 9000.00,
    "termId": 1,
    "courseName": "Computer Science",
    "remarks": "Regular term fee payment with merit scholarship",
    "discounts": [
      {
        "discountAmount": 1000.00,
        "label": "Merit Scholarship",
        "remarks": "Academic excellence discount"
      }
    ]
  }'
```

### 1.2 Create Regular Payment with Multiple Discounts
```bash
# Create a regular payment with multiple discounts
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 2,
    "label": "Term 2 Fee Payment",
    "amountPaid": 12500.00,
    "termId": 2,
    "courseName": "Mathematics",
    "remarks": "Payment with multiple discounts",
    "discounts": [
      {
        "discountAmount": 1500.00,
        "label": "Merit Scholarship",
        "remarks": "Academic performance"
      },
      {
        "discountAmount": 1000.00,
        "label": "Sports Quota",
        "remarks": "Athletic achievement"
      }
    ]
  }'
```

### 1.3 Create Additional Payment (Non-term payment)
```bash
# Create additional payment for late fee
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Late Fee",
    "amountPaid": 450.00,
    "courseName": "Computer Science",
    "remarks": "Late submission penalty",
    "discounts": [
      {
        "discountAmount": 50.00,
        "label": "First-time waiver",
        "remarks": "Partial waiver for first offense"
      }
    ]
  }'
```

### 1.4 Create Additional Payment without Course
```bash
# Create additional payment without course name (admission fee, etc.)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 3,
    "label": "Admission Fee",
    "amountPaid": 2000.00,
    "remarks": "One-time admission processing fee"
  }'
```

### 1.5 Create Payment with Custom Date
```bash
# Create payment with specific date
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Library Fine",
    "amountPaid": 800.00,
    "date": "2025-09-20T10:30:00",
    "remarks": "Book return delay fine",
    "discounts": [
      {
        "discountAmount": 200.00,
        "label": "Good behavior discount",
        "remarks": "First-time offender"
      }
    ]
  }'
```

## 2. VALIDATION TEST CASES (Expected Failures)

### 2.1 Regular Payment - Missing Term ID
```bash
# Should fail: Regular payment without termId
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 10000.00,
    "courseName": "Computer Science"
  }'
```

### 2.2 Regular Payment - Missing Course Name
```bash
# Should fail: Regular payment without course name
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 10000.00,
    "termId": 1
  }'
```

### 2.3 Invalid Student ID
```bash
# Should fail: Non-existent student
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 999999,
    "label": "Test Payment",
    "amountPaid": 10000.00
  }'
```

### 2.4 Invalid Course Name
```bash
# Should fail: Non-existent course
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 10000.00,
    "termId": 1,
    "courseName": "NonExistentCourse"
  }'
```

### 2.5 Invalid Term ID
```bash
# Should fail: Non-existent term/fee structure
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Term Fee Payment",
    "amountPaid": 10000.00,
    "termId": 999999,
    "courseName": "Computer Science"
  }'
```

### 2.6 Additional Payment with Term ID
```bash
# Should fail: Additional payment with termId (not allowed)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Late Fee",
    "amountPaid": 500.00,
    "termId": 1,
    "remarks": "This should fail"
  }'
```

### 2.7 Missing Required Fields
```bash
# Should fail: Missing required fields
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1
  }'
```

### 2.8 Negative Amount
```bash
# Should fail: Negative amount
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Invalid Payment",
    "amountPaid": -1000.00
  }'
```

## 3. PAYMENT RETRIEVAL TEST CASES

### 3.1 Get All Payments
```bash
curl -X GET http://localhost:8080/api/payments
```

### 3.2 Get Payment by ID
```bash
# Replace {id} with actual payment ID
curl -X GET http://localhost:8080/api/payments/1
```

### 3.3 Get Payments by Student ID
```bash
# Replace {studentId} with actual student admission number
curl -X GET http://localhost:8080/api/payments/student/1
```

### 3.4 Get Payments by Student ID and Payment Type
```bash
# Get regular payments for student
curl -X GET http://localhost:8080/api/payments/student/1/type/REGULAR

# Get additional payments for student
curl -X GET http://localhost:8080/api/payments/student/1/type/ADDITIONAL
```

### 3.5 Get Payments by Term ID
```bash
# Replace {termId} with actual term/fee structure ID
curl -X GET http://localhost:8080/api/payments/term/1
```

### 3.6 Get Active Payments Only
```bash
curl -X GET http://localhost:8080/api/payments/active
```

### 3.7 Get Inactive Payments Only
```bash
curl -X GET http://localhost:8080/api/payments/inactive
```

### 3.8 Get Active Payments by Student ID
```bash
# Replace {studentId} with actual student admission number
curl -X GET http://localhost:8080/api/payments/student/1/active
```

### 3.9 Get Payments by Student ID and Status
```bash
# Get active payments for student
curl -X GET http://localhost:8080/api/payments/student/1/status/true

# Get inactive payments for student
curl -X GET http://localhost:8080/api/payments/student/1/status/false
```

## 4. PAYMENT DEACTIVATION TEST CASES (Only Update Operation Allowed)

### 4.1 Deactivate Active Payment (Success)
```bash
# First create a payment, then deactivate it using the returned ID
# Step 1: Create payment
PAYMENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Test Payment for Deactivation",
    "amountPaid": 1000.00,
    "remarks": "Payment to be deactivated"
  }')

echo "Created payment: $PAYMENT_RESPONSE"

# Step 2: Extract payment ID (if jq is available)
# PAYMENT_ID=$(echo $PAYMENT_RESPONSE | jq -r '.id')

# Step 3: Deactivate payment (replace 1 with actual payment ID)
curl -X PATCH http://localhost:8080/api/payments/1/deactivate
```

### 4.2 Try to Deactivate Already Inactive Payment (Should Fail - 400 Bad Request)
```bash
# Try to deactivate the same payment again
curl -i -X PATCH http://localhost:8080/api/payments/1/deactivate
```

### 4.3 Try to Deactivate Non-existent Payment (Should Fail - 404 Not Found)
```bash
curl -i -X PATCH http://localhost:8080/api/payments/999999/deactivate
```

### 4.4 Verify Payment Status After Deactivation
```bash
# Check that isActive is now false
curl -X GET http://localhost:8080/api/payments/1 | jq '.isActive'
```

### 4.5 Complete Payment Lifecycle Test
```bash
#!/bin/bash
# Complete workflow test

echo "Step 1: Creating payment..."
PAYMENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Lifecycle Test Payment",
    "amountPaid": 1800.00,
    "discounts": [{
      "discountAmount": 200.00,
      "label": "Test Discount",
      "remarks": "Testing lifecycle"
    }]
  }')

echo "Payment created: $PAYMENT_RESPONSE"

echo "Step 2: Checking active payments count..."
ACTIVE_COUNT_BEFORE=$(curl -s -X GET http://localhost:8080/api/payments/active | jq '. | length')
echo "Active payments before deactivation: $ACTIVE_COUNT_BEFORE"

echo "Step 3: Deactivating payment (replace 1 with actual ID)..."
curl -s -X PATCH http://localhost:8080/api/payments/1/deactivate

echo "Step 4: Checking active payments count after deactivation..."
ACTIVE_COUNT_AFTER=$(curl -s -X GET http://localhost:8080/api/payments/active | jq '. | length')
echo "Active payments after deactivation: $ACTIVE_COUNT_AFTER"

echo "Step 5: Checking inactive payments..."
curl -s -X GET http://localhost:8080/api/payments/inactive | jq '. | length'

echo "Step 6: Attempting to deactivate again (should fail)..."
curl -i -X PATCH http://localhost:8080/api/payments/1/deactivate
```

## 5. VALIDATION TEST CASES FOR FORBIDDEN OPERATIONS

### 5.1 Attempt Regular Update Operation (Should Fail - 404 Method Not Found)
```bash
# These operations are no longer supported
curl -i -X PUT http://localhost:8080/api/payments/1 \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "label": "Updated Term Fee Payment",
    "amountPaid": 11000.00
  }'
```

### 5.2 Attempt Delete Operation (Should Fail - 404 Method Not Found)
```bash
# Delete operations are not supported
curl -i -X DELETE http://localhost:8080/api/payments/1
```

### 5.3 Invalid HTTP Methods on Deactivate Endpoint
```bash
# These should all fail with 405 Method Not Allowed
curl -i -X GET http://localhost:8080/api/payments/1/deactivate
curl -i -X POST http://localhost:8080/api/payments/1/deactivate \
  -H "Content-Type: application/json" -d '{}'
curl -i -X PUT http://localhost:8080/api/payments/1/deactivate \
  -H "Content-Type: application/json" -d '{}'
curl -i -X DELETE http://localhost:8080/api/payments/1/deactivate
```

## 6. DISCOUNT API TEST CASES (Read-Only)

### 6.1 Get All Discounts
```bash
curl -X GET http://localhost:8080/api/discounts
```

### 6.2 Get Discount by ID
```bash
# Replace {id} with actual discount ID
curl -X GET http://localhost:8080/api/discounts/1
```

### 6.3 Get Discounts by Payment ID
```bash
# Replace {paymentId} with actual payment ID
curl -X GET http://localhost:8080/api/discounts/payment/1
```

### 6.4 Verify Discount Delete is Not Allowed (Should Fail - 404 Method Not Found)
```bash
# Delete operations are no longer supported for discounts
curl -i -X DELETE http://localhost:8080/api/discounts/1
```

### 6.5 Verify Discounts Remain After Payment Deactivation
```bash
# Create payment with discounts, deactivate it, then check discounts still exist
# Step 1: Create payment with discounts (use response from previous tests)
# Step 2: Deactivate payment
# Step 3: Verify discounts still accessible
curl -X GET http://localhost:8080/api/discounts/payment/1
```

## 11. UPDATED API ENDPOINTS SUMMARY

### Available Operations:

#### Payment Creation
- `POST /api/payments` - Create new payment (only write operation)

#### Payment Retrieval
- `GET /api/payments` - Get all payments
- `GET /api/payments/{id}` - Get payment by ID
- `GET /api/payments/student/{studentId}` - Get payments by student
- `GET /api/payments/student/{studentId}/type/{paymentType}` - Get by student and type
- `GET /api/payments/term/{termId}` - Get payments by term
- `GET /api/payments/active` - Get all active payments
- `GET /api/payments/inactive` - Get all inactive payments
- `GET /api/payments/student/{studentId}/active` - Get active payments by student
- `GET /api/payments/student/{studentId}/status/{isActive}` - Get payments by student and status

#### Payment Status Management
- `PATCH /api/payments/{id}/deactivate` - **Deactivate payment (only update allowed)**

#### Discount Retrieval (Read-Only)
- `GET /api/discounts` - Get all discounts
- `GET /api/discounts/{id}` - Get discount by ID
- `GET /api/discounts/payment/{paymentId}` - Get discounts for payment

### Removed Operations:
- ❌ `PUT /api/payments/{id}` - Update payment (no longer supported)
- ❌ `DELETE /api/payments/{id}` - Delete payment (no longer supported)  
- ❌ `DELETE /api/discounts/{id}` - Delete discount (no longer supported)

## 12. BUSINESS RULES ENFORCED

1. ✅ **Payments are immutable** after creation except for deactivation
2. ✅ **One-way deactivation** - isActive can only go from true → false
3. ✅ **No reactivation** - Once deactivated, cannot be made active again
4. ✅ **Discounts are permanent** - Cannot be deleted or modified once created
5. ✅ **Historical integrity** - All payment records remain in database for audit purposes
6. ✅ **Default active status** - New payments are created with isActive = true
