const CACHE_NAME = 'isim-sehir-v1';
// Önbelleğe alınacak temel dosyalar (CDNs dahil)
const STATIC_ASSETS = [
  './',
  './index.html',
  'https://cdn.tailwindcss.com',
  'https://fonts.googleapis.com/css2?family=Nunito:wght@400;700;900&display=swap'
];

// Kurulum (Install)
self.addEventListener('install', event => {
  self.skipWaiting(); // Beklemeden aktif ol
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(STATIC_ASSETS);
    })
  );
});

// Aktifleştirme (Activate) - Eski versiyonları temizle
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.map(key => {
            if (key !== CACHE_NAME) {
             return caches.delete(key);
            }
        })
      );
    }).then(() => self.clients.claim()) // Hemen kontrolü ele al
  );
});

// İstekleri Yakalama (Fetch) - Network First (Önce İnternet, Yoksa Cache)
self.addEventListener('fetch', event => {
  event.respondWith(
    fetch(event.request)
      .then(response => {
        // İnternetten başarılı cevap geldiyse, cache'i güncelle
        if (!response || response.status !== 200 || response.type !== 'basic' && response.type !== 'cors') {
            return response;
        }
        const responseToCache = response.clone();
        caches.open(CACHE_NAME).then(cache => {
          cache.put(event.request, responseToCache);
        });
        return response;
      })
      .catch(() => {
        // İnternet yoksa cache'den dön
        return caches.match(event.request);
      })
  );
});