# İsim Şehir

Türkçe "İsim Şehir Hayvan Bitki Eşya" oyununu tek telefonda oynamak için
yazılmış native Android uygulaması. Kotlin + Jetpack Compose.

Akış aynı: harf → yazma → puanlama → tur kapanışı.

## Tasarım

Mürekkep laciverti zemin (`#0F1620`), kâğıt kremi kartlar (`#F2EDE3`) ve tek
bir sıcak vurgu rengi. Gradyan ve blur yok; onların yerine kalın tipografi
(Archivo / Archivo Narrow), ince çizgi kurallar ve geniş harf aralıklı
büyük harfli etiketler.

Vurgu rengi Ayarlar'dan dört seçenek arasında değiştirilebilir.

## Ekranlar

| Ekran | Ne yapar |
|---|---|
| Oyun | Aktif tur — yazma ve puanlama aynı düzenin iki hâli |
| Çark | Harfi rastgele seçer; kullanılmamış harfler arasından |
| Harf seçimi | Alfabeden elle seçim; oynanmış harfler soluk |
| Turlar | Bu oturumda kapanan turlar ve genel toplam |
| Ayarlar | Kategoriler, zamanlayıcı, vurgu rengi |

Zamanlayıcı iki biçimde çalışır: ekranda sürüklenebilir yüzen bir hap
(varsayılan) ya da üst bardaki süre göstergesi. İkisi de aynı paneli açar.

Bir turu silmek için Turlar ekranındaki kartı basılı tutun.

## Kalıcılık

Ayarlar (kategoriler, vurgu rengi, zamanlayıcı tercihleri) DataStore ile
kalıcıdır. Turlar bilinçli olarak oturumluktur — uygulama kapanınca sıfırlanır.

## Derleme

JDK 17 ve Android SDK (platform 35, build-tools 35.0.0) gerekir.
`local.properties` içinde `sdk.dir` SDK yolunu göstermeli.

```bash
./gradlew assembleRelease
```

Çıktı: `app/build/outputs/apk/release/app-release.apk`

### İmzalama

`keystore.properties` yoksa release derlemesi debug anahtarıyla imzalanır —
yan yüklemek için yeterlidir, Play Store'a yüklemek için değil. Kendi
anahtarınızı bağlamak için repo köküne şu dosyayı ekleyin (git'e girmez):

```properties
storeFile=C:/yol/uploader.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## İkon

Adaptive icon; hepsi vektör, hiç PNG yok. `background` + `foreground` +
Android 13+ temalı ikonlar için `monochrome` katmanı. Çizim 108dp tuvalin
66dp güvenli bölgesinde kalır, böylece hiçbir maske biçiminde kırpılmaz.
Aynı vektör Android 12+ splash ekranında da kullanılır.

## Geçmiş

Bu uygulama daha önce bir PWA idi. O sürüm `web-legacy` branch'inde duruyor.
Servis çalışanı, PWA manifest'i ve önbellek temizleme butonu gibi web'e özgü
her şey bu branch'te kaldı; Android sürümünde karşılıkları yok.

## Lisans

Archivo ve Archivo Narrow, SIL Open Font License 1.1 altında dağıtılıyor —
bkz. [licenses/Archivo-OFL.txt](licenses/Archivo-OFL.txt).
