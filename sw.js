const CACHE_NAME = "trader-pink-ai-v1";

const APP_FILES = [
  "./",
  "./index.html",
  "./manifest.json"
];

self.addEventListener("install", event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(APP_FILES))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys
          .filter(key => key !== CACHE_NAME)
          .map(key => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", event => {
  const request = event.request;

  if (request.method !== "GET") {
    return;
  }

  const url = new URL(request.url);

  // Cloudflare Worker API সবসময় live network থেকে নেবে
  if (
    url.hostname.includes("workers.dev") ||
    url.pathname.startsWith("/analyze") ||
    url.pathname.startsWith("/market") ||
    url.pathname.startsWith("/history") ||
    url.pathname.startsWith("/results")
  ) {
    event.respondWith(
      fetch(request).catch(() =>
        new Response(
          JSON.stringify({
            status: "error",
            message: "Internet connection required for live market data."
          }),
          {
            headers: {
              "Content-Type": "application/json"
            }
          }
        )
      )
    );
    return;
  }

  // App files cache থেকে দ্রুত লোড হবে
  event.respondWith(
    caches.match(request).then(cachedResponse => {
      return cachedResponse || fetch(request).then(response => {
        if (
          response &&
          response.status === 200 &&
          response.type === "basic"
        ) {
          const responseClone = response.clone();

          caches.open(CACHE_NAME).then(cache => {
            cache.put(request, responseClone);
          });
        }

        return response;
      });
    })
  );
});
