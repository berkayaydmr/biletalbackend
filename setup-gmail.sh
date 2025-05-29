#!/bin/bash

echo "🔧 Gmail SMTP Kurulum Scripti"
echo "=============================="

# Kullanıcıdan Gmail bilgilerini al
echo
read -p "Gmail adresinizi girin: " GMAIL_ADDRESS
echo

echo "Gmail App Password oluşturmak için:"
echo "1. https://myaccount.google.com/ → Security → 2-Step Verification"
echo "2. App passwords → Mail → Other (Bilet Al Backend)"
echo "3. 16 haneli kodu kopyalayın"
echo

read -p "16 haneli App Password'u girin (boşluksuz): " APP_PASSWORD
echo

# Environment variables'ları ayarla
export MAIL_USERNAME="$GMAIL_ADDRESS"
export MAIL_PASSWORD="$APP_PASSWORD"

echo "✅ Environment variables ayarlandı:"
echo "MAIL_USERNAME: $MAIL_USERNAME"
echo "MAIL_PASSWORD: ${APP_PASSWORD:0:4}****${APP_PASSWORD: -4}"
echo

# Uygulamayı yeniden başlat
echo "🚀 Uygulamayı yeniden başlatıyor..."
echo

# Mevcut java process'ini bul ve durdur
JAVA_PID=$(lsof -ti:8081)
if [ ! -z "$JAVA_PID" ]; then
    echo "Mevcut uygulamayı durduruyor (PID: $JAVA_PID)..."
    kill -9 $JAVA_PID
    sleep 2
fi

echo "Uygulamayı Gmail SMTP ile başlatıyor..."
export MAIL_USERNAME="$GMAIL_ADDRESS"
export MAIL_PASSWORD="$APP_PASSWORD"

# Maven ile başlat
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081" &

echo "✅ Uygulama Gmail SMTP ile başlatıldı!"
echo
echo "🧪 Test etmek için:"
echo "curl -X POST http://localhost:8081/api/auth/forgot-password \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"email\":\"$GMAIL_ADDRESS\"}'"
echo
echo "📧 Gmail inbox'ınızı kontrol edin!"
