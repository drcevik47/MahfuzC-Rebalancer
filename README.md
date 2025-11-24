# Bybit Portfolio Rebalancer

Android uygulaması olarak geliştirilmiş Bybit Unified Trading hesabı için otomatik portföy dengeleme sistemi.

## Özellikler

- **API Entegrasyonu**: Bybit API v5 ile tam entegrasyon
- **Anlık Fiyat Takibi**: WebSocket ile gerçek zamanlı fiyat güncellemeleri
- **Otomatik Dengeleme**: Belirlenen eşik değerine göre otomatik alım/satım
- **Arka Plan Çalışma**: Foreground Service ile kesintisiz çalışma
- **Otomatik Başlatma**: Telefon yeniden başlatıldığında otomatik başlama
- **Detaylı Loglama**: Tüm işlemlerin ve olayların kaydı
- **Modern UI**: Material Design 3 ile güzel arayüz

## Kurulum

### Gereksinimler

- Android Studio Hedgehog (2023.1.1) veya üzeri
- JDK 17
- Android SDK 34
- Minimum Android 8.0 (API 26)

### Derleme

```bash
# Projeyi klonla
git clone https://github.com/[username]/bybit-rebalancer.git

# Android Studio ile aç veya terminal'den derle
./gradlew assembleDebug
```

### APK Oluşturma

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (imzalı)
./gradlew assembleRelease
```

APK dosyası `app/build/outputs/apk/` klasöründe oluşturulur.

## Kullanım

### 1. API Ayarları

1. Bybit hesabınızdan API Key ve Secret oluşturun
2. Unified Trading ve Spot Trading izinlerini aktif edin
3. Uygulamada API sekmesine giderek bilgileri girin
4. "Bağlantıyı Test Et" ile doğrulayın

### 2. Portföy Kurulumu

1. Portföy sekmesine gidin
2. "+" butonuyla coin ekleyin
3. Her coin için hedef yüzdeyi belirleyin
4. Toplam yüzdenin %100 olmasına dikkat edin (USDT dahil)

### 3. Ayarlar

- **Dengeleme Eşiği**: Portföyün hedeften ne kadar sapması durumunda dengeleme yapılacağı (örn: %1)
- **Minimum İşlem**: Bu tutarın altındaki işlemler yapılmaz
- **Kontrol Aralığı**: Portföyün ne sıklıkla kontrol edileceği

### 4. Servisi Başlatma

1. Ana sayfada "Başlat" butonuna tıklayın
2. Bildirim izni verin
3. Pil optimizasyonunu kapatmanız önerilir

## Teknik Detaylar

### Mimari

- **MVVM Architecture**: ViewModel ile UI state yönetimi
- **Hilt**: Dependency Injection
- **Room**: Yerel veritabanı
- **DataStore**: Ayarların saklanması
- **Retrofit + OkHttp**: HTTP istekleri
- **Jetpack Compose**: Modern UI toolkit
- **Coroutines + Flow**: Asenkron işlemler

### Klasör Yapısı

```
app/src/main/java/com/bybit/rebalancer/
├── api/                    # API servisleri ve WebSocket
│   ├── BybitApiService.kt
│   ├── BybitAuthInterceptor.kt
│   └── BybitWebSocket.kt
├── data/
│   ├── database/          # Room DAOs ve Database
│   ├── model/             # Data sınıfları
│   └── repository/        # Repository pattern
├── di/                    # Hilt modules
├── service/               # Foreground Service ve Receivers
├── ui/
│   ├── components/        # Yeniden kullanılabilir UI bileşenleri
│   ├── screens/           # Ekranlar
│   ├── theme/             # Tema ve renkler
│   └── viewmodel/         # ViewModels
└── util/                  # Yardımcı fonksiyonlar
```

### Güvenlik

- API bilgileri şifrelenmiş DataStore'da saklanır
- HTTPS zorunlu
- API imzalama (HMAC-SHA256)

## Dengeleme Algoritması

1. Portföy anlık değeri hesaplanır
2. Her coin için mevcut yüzde ve hedef yüzde karşılaştırılır
3. Eşik değerini aşan sapmalar tespit edilir
4. Önce SELL işlemleri (USDT likidite için)
5. Sonra BUY işlemleri yapılır
6. Her işlem öncesi/sonrası portföy durumu kaydedilir

## Bilinen Sınırlamalar

- Sadece Spot market desteklenir
- USDT quote coin olarak kullanılır
- Market order kullanılır (slippage olabilir)

## Lisans

MIT License

## Sorumluluk Reddi

Bu uygulama eğitim amaçlıdır. Kripto para ticareti risk içerir. Kayıplardan geliştirici sorumlu değildir.
