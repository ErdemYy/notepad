# NotePad

Java Swing ile geliştirilmiş modern, zengin özellikli bir metin editörü. Sözdizimi vurgulama, çoklu dosya formatı desteği ve şık karanlık mod arayüzü içerir.

## Özellikler

- **Çoklu Sekme Arayüzü**: Aynı anda birden fazla dosya ile çalışın
- **Sözdizimi Vurgulama**: Java, Python, JavaScript, C/C++, HTML, CSS, XML ve daha fazlası dahil 20+ programlama dili desteği
- **Karanlık/Açık Mod**: FlatLaf açık ve karanlık temaları arasında geçiş yapın, Monokai sözdizimi teması ile
- **Dosya Gezgini**: Dizin gezinme özellikli yerleşik dosya ağacı tarayıcısı
- **Zengin Metin Editörü**: 
  - Özel gutter stili ile satır numaraları
  - Kod katlama
  - Geri Al/Yinele desteği
  - Kes/Kopyala/Yapıştır işlemleri
  - Gerçek zamanlı imleç konumu takibi
- **Çoklu Dosya Formatı Desteği**:
  - Sözdizimi vurgulama ile metin dosyaları
  - Resim görüntüleme (JPG, PNG, GIF, BMP)
  - PDF görüntüleme
- **Dosya Şifreleme**: AES-256 şifreleme ile dosya kaydetme ve açma
- **Akıllı Sekme Yönetimi**:
  - Sekmelerde kapatma butonları
  - Sağ tıklama bağlam menüsü (Kapat, Tümünü Kapat, Diğerlerini Kapat)
  - Orta tıklama ile sekme kapatma
  - Onay diyalogları ile kaydedilmemiş değişiklik tespiti
  - Değiştirilmiş durum göstergesi (sekme başlığında yıldız işareti)
- **UTF-8 Desteği**: Türkçe ve diğer Unicode karakterleri düzgün şekilde işler
- **Durum Çubuğu**: Satır, sütun, karakter sayısı ve dosya kodlamasını görüntüler
- **Araç Çubuğu**: Yaygın işlemler için hızlı erişim butonları

## Gereksinimler

- Java 17 veya üzeri
- Maven 3.9.x veya üzeri

## Bağımlılıklar

- **RSyntaxTextArea 3.3.3**: Sözdizimi vurgulama ile gelişmiş metin editörü bileşeni
- **Apache PDFBox 2.0.30**: PDF render desteği
- **FlatLaf 3.3**: Modern görünüm ve his

## Derleme ve Çalıştırma

### Maven Kullanarak

```bash
# Projeyi derle
mvn clean compile

# Uygulamayı çalıştır
mvn exec:java
```

### Çalıştırılabilir JAR Oluşturma

```bash
mvn clean package
java -jar target/note_uyg-0.1.0-SNAPSHOT.jar
```

## Kullanım

### Temel İşlemler

- **Yeni Dosya**: [New] butonuna tıklayın veya Ctrl+N
- **Dosya Aç**: [Open] butonuna tıklayın veya Ctrl+O
- **Dosya Kaydet**: [Save] butonuna tıklayın veya Ctrl+S
- **Karanlık Modu Aç/Kapat**: View menüsü → Dark Mode veya Ctrl+D

### Sekme Yönetimi

- **Sekmeyi Kapat**: X butonuna tıklayın, orta tıklama yapın veya sağ tıklama → Kapat
- **Tüm Sekmeleri Kapat**: Herhangi bir sekmeye sağ tıklama → Tümünü Kapat
- **Diğer Sekmeleri Kapat**: Bir sekmeye sağ tıklama → Diğerlerini Kapat

### Dosya Ağacı

- Bir klasöre göz atmak için "Select Directory" butonuna tıklayın
- Dosyaları açmak için çift tıklayın
- Dosya türlerini otomatik olarak algılar ve uygun sözdizimi vurgulamasını uygular

## Klavye Kısayolları

- `Ctrl+N` - Yeni dosya
- `Ctrl+O` - Dosya aç
- `Ctrl+S` - Dosya kaydet
- `Ctrl+D` - Karanlık modu aç/kapat
- `Ctrl+Z` - Geri al
- `Ctrl+Y` - Yinele
- `Ctrl+X` - Kes
- `Ctrl+C` - Kopyala
- `Ctrl+V` - Yapıştır

## Proje Yapısı

```
note_uyg/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── noteuyg/
│                       └── AdvancedEditor.java
├── pom.xml
└── README.md
```

## Lisans

Bu proje açık kaynaklıdır ve MIT Lisansı altında mevcuttur.

## Katkıda Bulunma

Katkılar memnuniyetle karşılanır! Lütfen Pull Request göndermekten çekinmeyin.

## Yazar

Java Swing ile ❤️ ile geliştirildi
