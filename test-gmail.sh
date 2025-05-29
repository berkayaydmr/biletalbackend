#!/bin/bash

echo "🧪 Gmail SMTP Test Scripti"
echo "========================="

# Kullanıcıdan test email'ini al
read -p "Test için e-posta adresinizi girin: " TEST_EMAIL

echo
echo "📤 Forgot Password API'sini test ediyor..."

RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\"}" \
  -w "\nHTTP_STATUS:%{http_code}")

echo "$RESPONSE"

HTTP_STATUS=$(echo "$RESPONSE" | grep "HTTP_STATUS:" | cut -d':' -f2)

if [ "$HTTP_STATUS" = "200" ]; then
    echo
    echo "✅ API Test Başarılı!"
    echo "📧 Gmail inbox'ınızı kontrol edin: $TEST_EMAIL"
    echo "📋 Konu: 'Bilet Al - Şifre Sıfırlama'"
    echo
    echo "🔍 Console loglarını kontrol etmek için:"
    echo "Application loglarında şu mesajı arayin:"
    echo "✅ Password reset email sent successfully to: $TEST_EMAIL"
else
    echo
    echo "❌ API Test Başarısız (HTTP $HTTP_STATUS)"
    echo "🔧 Gmail SMTP kurulumunu kontrol edin"
fi
