const CACHE_NAME = 'isim-sehir-v7';

// Önbelleğe alınacak dosyalar listesi
const STATIC_ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './style.css'
];

self.addEventListener('install', event => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      // Dosyaları önbelleğe al
      return cache.addAll(STATIC_ASSETS);
    })
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.map(key => {
          // Eski versiyon cache'leri temizle
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  // Sadece HTTP(S) isteklerini yakala
  if (!event.request.url.startsWith('http')) return;

  event.respondWith(
    // Önce internete gitmeye çalış (Güncel versiyon için)
    fetch(event.request)
      .then(response => {
        // Cevap başarılıysa cache'i güncelle
        if (response && response.status === 200) {
          const responseToCache = response.clone();
          caches.open(CACHE_NAME).then(cache => {
            cache.put(event.request, responseToCache);
          });
        }
        return response;
      })
      .catch(() => {
        // İnternet yoksa cache'den dön
        return caches.match(event.request)
          .then(cachedResponse => {
            if (cachedResponse) {
              return cachedResponse;
            }
            // Eğer sayfa isteğiyse ve cache'de yoksa index.html dön
            if (event.request.mode === 'navigate') {
              return caches.match('./index.html');
            }
          });
      })
  );
});