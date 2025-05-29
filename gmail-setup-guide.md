# Gmail SMTP Kurulum Rehberi

## 1. Gmail'de App Password Oluşturun

### Adım 1: Google Account'a gidin
- https://myaccount.google.com/ adresine gidin
- Gmail hesabınızla giriş yapın

### Adım 2: Security ayarlarını açın
- Sol menüden **Security** seçin
- **2-Step Verification** açık olmalı (kapalıysa açın)

### Adım 3: App Password oluşturun
- **Security** sayfasında **App passwords** bölümünü bulun
- **Select app** → **Mail** seçin
- **Select device** → **Other (custom name)** seçin
- **Name**: "Bilet Al Backend" yazın
- **Generate** butonuna tıklayın
- **16 haneli password'u kopyalayın** (örnek: `abcd efgh ijkl mnop`)

## 2. Environment Variables Ayarlayın

Terminal'de şu komutları çalıştırın (kendi bilgilerinizi girin):

```bash
# Gmail bilgilerinizi girin
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-16-digit-app-password"
```

**Örnek:**
```bash
export MAIL_USERNAME="oktay.korkut@gmail.com"
export MAIL_PASSWORD="abcd efgh ijkl mnop"
```

## 3. Uygulamayı Yeniden Başlatın

```bash
cd /Users/oktaykorkut/biletalbackend

# Mevcut uygulamayı durdur
lsof -ti:8081 | xargs kill -9

# Yeniden başlat
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

## 4. Test Edin

```bash
# Forgot password API'sini test edin
curl -X POST http://localhost:8081/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"your-real-email@gmail.com"}'
```

## 5. Beklenen Sonuç

- ✅ API: `{"message":"Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.","success":true}`
- ✅ Console: `✅ Password reset email sent successfully to: your-email@gmail.com`
- ✅ Email: Gmail inbox'ınızda "Bilet Al - Şifre Sıfırlama" konulu email

## Troubleshooting

### "Authentication failed" hatası alırsanız:
- App password doğru kopyalandığından emin olun
- 2-Step Verification açık olduğundan emin olun
- Gmail hesabınızın güvenlik ayarlarını kontrol edin

### Email gelmiyor:
- Spam klasörünü kontrol edin
- Environment variables doğru ayarlandığından emin olun
- Console loglarını kontrol edin

### Port problemi:
```bash
# Port 8081'i kontrol edin
lsof -i :8081

# Gerekirse uygulamayı farklı portta başlatın
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```
