# Updated Payment System Test Cases - Including isActive Functionality

## New Test Cases for Payment Deactivation

### 1. PAYMENT DEACTIVATION TESTS

#### 1.1 Deactivate Active Payment (Success)
```bash
# First create a payment, then deactivate it using the returned ID
# Step 1: Create payment
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "originalAmount": 1000.00,
    "label": "Test Payment for Deactivation",
    "amountPaid": 1000.00
  }'

# Step 2: Deactivate payment (replace {id} with actual payment ID from step 1)
curl -X PATCH http://localhost:8080/api/payments/1/deactivate
```

#### 1.2 Try to Deactivate Already Inactive Payment (Should Fail)
```bash
# Try to deactivate the same payment again - should return 400 Bad Request
curl -X PATCH http://localhost:8080/api/payments/1/deactivate
```

#### 1.3 Try to Deactivate Non-existent Payment (Should Fail)
```bash
# Should return 404 Not Found
curl -X PATCH http://localhost:8080/api/payments/999999/deactivate
```

### 2. ACTIVE/INACTIVE PAYMENT FILTERING TESTS

#### 2.1 Get All Active Payments
```bash
curl -X GET http://localhost:8080/api/payments/active
```

#### 2.2 Get All Inactive Payments
```bash
curl -X GET http://localhost:8080/api/payments/inactive
```

#### 2.3 Get Active Payments for Specific Student
```bash
curl -X GET http://localhost:8080/api/payments/student/1/active
```

#### 2.4 Get Payments by Student and Status
```bash
# Get active payments for student 1
curl -X GET http://localhost:8080/api/payments/student/1/status/true

# Get inactive payments for student 1
curl -X GET http://localhost:8080/api/payments/student/1/status/false
```

### 3. COMPLETE WORKFLOW TEST

#### 3.1 Complete Payment Lifecycle Test
```bash
# Step 1: Create a payment
echo "Creating payment..."
PAYMENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "originalAmount": 2000.00,
    "label": "Lifecycle Test Payment",
    "amountPaid": 1800.00,
    "discounts": [{
      "discountAmount": 200.00,
      "label": "Test Discount",
      "remarks": "Testing lifecycle"
    }]
  }')

echo "Payment created: $PAYMENT_RESPONSE"

# Step 2: Extract payment ID (assuming jq is available)
PAYMENT_ID=$(echo $PAYMENT_RESPONSE | jq -r '.id')
echo "Payment ID: $PAYMENT_ID"

# Step 3: Verify payment is active
echo "Checking if payment is active..."
curl -s -X GET http://localhost:8080/api/payments/$PAYMENT_ID | jq '.isActive'

# Step 4: Get all active payments (should include our payment)
echo "Getting all active payments..."
curl -s -X GET http://localhost:8080/api/payments/active

# Step 5: Deactivate the payment
echo "Deactivating payment..."
curl -s -X PATCH http://localhost:8080/api/payments/$PAYMENT_ID/deactivate

# Step 6: Verify payment is now inactive
echo "Checking if payment is now inactive..."
curl -s -X GET http://localhost:8080/api/payments/$PAYMENT_ID | jq '.isActive'

# Step 7: Try to deactivate again (should fail)
echo "Trying to deactivate again (should fail)..."
curl -i -X PATCH http://localhost:8080/api/payments/$PAYMENT_ID/deactivate

# Step 8: Get all inactive payments (should include our payment)
echo "Getting all inactive payments..."
curl -s -X GET http://localhost:8080/api/payments/inactive
```

### 4. VALIDATION TESTS (Expected Failures)

#### 4.1 Invalid HTTP Methods on Deactivate Endpoint
```bash
# These should all fail with 405 Method Not Allowed
curl -X GET http://localhost:8080/api/payments/1/deactivate
curl -X POST http://localhost:8080/api/payments/1/deactivate
curl -X PUT http://localhost:8080/api/payments/1/deactivate
curl -X DELETE http://localhost:8080/api/payments/1/deactivate
```

#### 4.2 Verify No Other Updates Are Allowed
```bash
# Try to update other fields - these endpoints should not exist (404)
curl -X PUT http://localhost:8080/api/payments/1 \
  -H "Content-Type: application/json" \
  -d '{"label": "Updated Label"}'

curl -X PATCH http://localhost:8080/api/payments/1 \
  -H "Content-Type: application/json" \
  -d '{"originalAmount": 5000.00}'
```

### 5. EDGE CASES

#### 5.1 Deactivate Payment with Discounts
```bash
# Create payment with discounts, then deactivate
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "originalAmount": 5000.00,
    "label": "Payment with Multiple Discounts",
    "amountPaid": 4000.00,
    "discounts": [
      {
        "discountAmount": 500.00,
        "label": "Merit Scholarship",
        "remarks": "Academic excellence"
      },
      {
        "discountAmount": 500.00,
        "label": "Sports Quota",
        "remarks": "Athletic achievement"
      }
    ]
  }'

# Then deactivate using the returned ID
curl -X PATCH http://localhost:8080/api/payments/{id}/deactivate
```

#### 5.2 Check Deactivated Payment Still Shows Discounts
```bash
# After deactivation, verify payment and its discounts are still accessible
curl -X GET http://localhost:8080/api/payments/{id}
curl -X GET http://localhost:8080/api/discounts/payment/{id}
```

### 6. BULK OPERATIONS

#### 6.1 Create Multiple Payments and Deactivate Some
```bash
#!/bin/bash
echo "Creating multiple payments..."

# Create 5 payments
for i in {1..5}; do
  curl -s -X POST http://localhost:8080/api/payments \
    -H "Content-Type: application/json" \
    -d "{
      \"studentId\": 1,
      \"originalAmount\": $(( $i * 1000 )).00,
      \"label\": \"Bulk Test Payment $i\",
      \"amountPaid\": $(( $i * 1000 )).00
    }" > /dev/null
  echo "Created payment $i"
done

echo "Getting all payments..."
ALL_PAYMENTS=$(curl -s -X GET http://localhost:8080/api/payments)
echo "Total payments: $(echo $ALL_PAYMENTS | jq '. | length')"

echo "Getting active payments..."
ACTIVE_PAYMENTS=$(curl -s -X GET http://localhost:8080/api/payments/active)
echo "Active payments: $(echo $ACTIVE_PAYMENTS | jq '. | length')"

# Deactivate the first payment (assuming ID 1 exists)
echo "Deactivating payment with ID 1..."
curl -s -X PATCH http://localhost:8080/api/payments/1/deactivate > /dev/null

echo "Getting active payments after deactivation..."
ACTIVE_AFTER=$(curl -s -X GET http://localhost:8080/api/payments/active)
echo "Active payments after deactivation: $(echo $ACTIVE_AFTER | jq '. | length')"

echo "Getting inactive payments..."
INACTIVE_PAYMENTS=$(curl -s -X GET http://localhost:8080/api/payments/inactive)
echo "Inactive payments: $(echo $INACTIVE_PAYMENTS | jq '. | length')"
```

## Summary of Available Endpoints

### Payment Creation (Unchanged)
- `POST /api/payments` - Create payment

### Payment Retrieval (Unchanged)
- `GET /api/payments` - Get all payments
- `GET /api/payments/{id}` - Get payment by ID
- `GET /api/payments/student/{studentId}` - Get payments by student
- `GET /api/payments/student/{studentId}/type/{paymentType}` - Get by student and type
- `GET /api/payments/term/{termId}` - Get payments by term

### NEW: Payment Status Management
- `PATCH /api/payments/{id}/deactivate` - **Deactivate payment (only allowed update)**

### NEW: Active/Inactive Filtering
- `GET /api/payments/active` - Get all active payments
- `GET /api/payments/inactive` - Get all inactive payments
- `GET /api/payments/student/{studentId}/active` - Get active payments by student
- `GET /api/payments/student/{studentId}/status/{isActive}` - Get payments by student and status

### Discount Retrieval (Unchanged)
- `GET /api/discounts` - Get all discounts
- `GET /api/discounts/{id}` - Get discount by ID
- `GET /api/discounts/payment/{paymentId}` - Get discounts for payment

## Key Business Rules Enforced

1. ✅ **New payments default to active** (`isActive = true`)
2. ✅ **Only deactivation allowed** (`true` → `false`, never `false` → `true`)
3. ✅ **No other updates permitted** on payment fields
4. ✅ **Deactivated payments remain accessible** for historical records
5. ✅ **Discounts remain intact** when payment is deactivated
