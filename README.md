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
| Ayarlar | Kategoriler, zamanlayıcı, vurgu rengi, titreşim |

Zamanlayıcı iki biçimde çalışır: ekranda sürüklenebilir yüzen bir hap
(varsayılan) ya da üst bardaki süre göstergesi. İkisi de aynı paneli açar.

Bir turu silmek için Turlar ekranındaki kartı basılı tutun. Silinen turun
harfi de serbest kalır — çarkta ve harf seçiminde yeniden oynanabilir hâle
gelir. Oynanmış harfler ayrı bir liste olarak tutulmuyor, turlardan
türetiliyor.

## Titreşim

Arayüz dokunuşları — buton, chip, harf seçimi, sekme, uzun basış — üç kademeli
bir yoldan çalar. Öncelik **haptic primitive**'lerde (CLICK, TICK, LOW_TICK,
THUD): üreticinin motor için ayarladığı hazır darbelerdir, keskin atak ve hızlı
sönümle "tok" hissederler; `scale` ile şiddetleri düşünce hafifler ama tokluğunu
kaybetmezler. Primitive desteklenmiyorsa tepe + kısa sönüm biçiminde yazılmış
kendi dalgalarımız devreye girer — düz ve alçak bir dalga motoru oturtamadan
bıraktığı için pürüzlü hissettiriyordu, zarf bunu düzeltiyor. Genliği hiç
ayarlanamayan cihazlarda sistemin en yumuşak sabitlerine düşülür.

Ayarlar'daki Hafif / Orta / Güçlü seçimi bu şiddetleri ölçekler; varsayılan
Hafif.

Çark dönerken işaretçinin altından geçen her harf bir tık verir; tıklar arasında
en az 55 ms bırakılır, yoksa dönüşün başındaki hız tek bir vızıltıya dönüşür.
Tur harfiyle başlamayan bir kelime girildiğinde alan sessizce temizlendiği için
ayrı bir "reddet" titreşimi var.

Süre bitişi ayrı bir kanal: `VIBRATE` izniyle çalınan bir kalıp, bipi ve kırmızı
flaşı tamamlıyor. Gücü Ayarlar'dan Yüksek / Orta / Kapalı olarak seçilir
(varsayılan Orta) ve genel titreşim anahtarından bağımsız çalışır — dokunuşları
kapatıp yalnızca süre uyarısını açık bırakmak mümkün. Son üç saniyede her saniye
tek bir hafif tik gelir.

## Kalıcılık

Ayarlar (kategoriler, vurgu rengi, zamanlayıcı ve titreşim tercihleri)
DataStore ile kalıcıdır. Turlar bilinçli olarak oturumluktur — uygulama kapanınca sıfırlanır.

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

## Otomatik yayın

`main` branch'ine her push'ta `.github/workflows/release.yml` çalışır: APK'yı
derler ve GitHub Releases'e yükler. Elle tetiklemek için Actions sekmesindeki
"Derle ve yayınla" → *Run workflow*.

Sürüm adı `2.0.<derleme sayısı>` biçiminde üretilir; sayı, iş akışının
`run_number` değeridir. `versionCode` de aynı sayaçla artar, böylece her
release bir öncekinin üzerine kurulabilir. Temel sürümü (`2.0`) yükseltmek
için `app/build.gradle.kts` içindeki `baseVersionName` değerini değiştirmek
yeterli. Yerel derlemede sayaç 0 kabul edilir, sürüm sade `2.0` kalır.

### CI'da imzalama

Depoda imza anahtarı yoksa CI, release APK'sını runner'ın debug anahtarıyla
imzalar. Bu anahtar her çalışmada yeniden üretildiği için imza sürümden
sürüme değişir — kullanıcı güncellemeyi kurmadan önce eskisini silmek zorunda
kalır. Sabit imza için depo ayarlarına şu secret'ları ekleyin:

| Secret | Değer |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 uploader.jks` çıktısı |
| `KEYSTORE_PASSWORD` | keystore parolası |
| `KEY_ALIAS` | anahtar takma adı |
| `KEY_PASSWORD` | anahtar parolası |

`KEYSTORE_BASE64` tanımlıysa iş akışı `keystore.properties` dosyasını kendisi
oluşturur ve APK gerçek anahtarla imzalanır.

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
