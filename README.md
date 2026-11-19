# react-native-okhttp-nodelay

Android-only React Native library that turns off Nagle's algorithm (sets `TCP_NODELAY`) on the
sockets behind React Native's `fetch` / `XMLHttpRequest`. It removes a stall of about one round
trip (~50 ms p50 at our ingress) from every POST. Installing it is the whole integration: it is
autolinked and needs no app code and no JS import.

## Why

- React Native wraps every request body in `ProgressRequestBody`, whose `writeTo` ends with
  `flush()`. OkHttp's HTTP/2 sink therefore sends HEADERS + DATA, and then, on `close()`, a
  separate tiny DATA frame carrying END_STREAM.
- OkHttp 4.12 does not set `TCP_NODELAY`, so Nagle holds that second small write until the first
  one is ACKed, and the server delays that ACK.
- nginx buffers the request body and forwards upstream only after END_STREAM, so the whole
  request waits. GETs have no body and are unaffected; native OkHttp clients that do not flush
  mid-body are unaffected.

iOS needs nothing: `NSURLSession` already sets `TCP_NODELAY`. The library ships no podspec, so
iOS autolinking skips it.

## Install

```json
"react-native-okhttp-nodelay": "city-mall/react-native-okhttp-nodelay#v0.1.0"
```

Then rebuild the Android app. This is a native change: it reaches users only with a new binary,
not over CodePush / OTA.

## How it works

- `NoDelaySocketFactory` wraps `SocketFactory.getDefault()` and sets `tcpNoDelay = true` on every
  socket it returns. OkHttp layers TLS over that socket, so HTTPS is covered.
- Autolinking adds `OkHttpNoDelayPackage` to the app's `PackageList`, which React Native
  constructs before it runs any JS. Its constructor calls
  `NetworkingModule.setCustomClientBuilder { it.socketFactory(NoDelaySocketFactory) }`.
- `NetworkingModule` applies that builder to the per-request `client.newBuilder()` in
  `sendRequest` (React Native 0.73 through 0.86 at least). The shared client keeps its
  timeouts, cookie jar, HTTP cache, connection pool and dispatcher.
- Only `fetch` / XHR change. Image loading (Fresco), WebSockets, the dev-support client and
  other libraries' OkHttp clients are untouched. That is why this uses the `NetworkingModule`
  hook and not `OkHttpClientProvider.setOkHttpClientFactory`, which those callers share.

## This library owns `NetworkingModule.setCustomClientBuilder`

`NetworkingModule` keeps a **single static slot** for the custom client builder: the last
`setCustomClientBuilder` call wins. With this library installed, an app (or another library) that
also calls it silently replaces this one, or is replaced by it, depending on order. If an app ever
needs its own per-request builder change, add it here so there is one owner, rather than calling
`setCustomClientBuilder` from the app.

## Tests

JVM unit tests live in `android/src/test` and run inside any host app that depends on the
library, against that app's React Native and OkHttp:

```sh
cd android && ./gradlew :react-native-okhttp-nodelay:testDebugUnitTest
```

- `NoDelaySocketFactoryTest`: each `createSocket` overload returns a socket with Nagle off, and a
  real OkHttp call through the factory runs on a connection with Nagle off.
- `OkHttpNoDelayPackageTest`: constructing the package registers a builder that gives React
  Native's per-request client this socket factory. It reads `NetworkingModule`'s private slot,
  so it also fails if a React Native upgrade moves or renames the hook.
