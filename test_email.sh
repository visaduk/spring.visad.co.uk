#!/bin/bash

# Test Email Script for VisaD Spring Boot Application
# Usage: ./test_email.sh <recipient_email> [subject] [message]

RECIPIENT_EMAIL="${1:-test@example.com}"
SUBJECT="${2:-Test Email from VisaD Spring Boot}"
MESSAGE="${3:-This is a test email to verify the SMTP configuration is working correctly. If you receive this, the email system is functioning properly!}"

API_URL="http://localhost:8080/api/test/send-email"

echo "================================================"
echo "VisaD Email Test"
echo "================================================"
echo "Sending test email to: $RECIPIENT_EMAIL"
echo "Subject: $SUBJECT"
echo "------------------------------------------------"

# Send the test email
RESPONSE=$(curl -s -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"toEmail\": \"$RECIPIENT_EMAIL\",
    \"subject\": \"$SUBJECT\",
    \"message\": \"$MESSAGE\"
  }")

echo "Response from server:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo "------------------------------------------------"

# Check if successful
if echo "$RESPONSE" | grep -q '"status":"success"'; then
    echo "✓ Email sent successfully!"
    echo "Please check the inbox of: $RECIPIENT_EMAIL"
else
    echo "✗ Email sending failed!"
    echo "Check the server logs for more details:"
    echo "  tail -50 /home/ubuntu/spring.visad.co.uk/server.log"
fi

echo "================================================"
StrongPassword123!