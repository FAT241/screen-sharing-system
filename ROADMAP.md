# ROADMAP — screen-sharing-system → mini-TeamViewer

> **Đây là file định hướng duy nhất của project.**
> Người hoặc AI mới vào project: đọc file này TRƯỚC, làm theo thứ tự từ trên xuống.
> Không tự ý làm phase sau khi phase trước chưa xong. Không tự thêm tính năng.
> Cập nhật checkbox ngay khi làm xong mục nào.

### Mục lục

| Mục | Nội dung | Khi nào cần đọc |
|---|---|---|
| [0](#0-dự-án-là-gì) | Dự án là gì, module, lệnh build | Lần đầu vào project |
| [1](#1-giao-thức-hiện-tại-đọc-trước-khi-sửa) | Giao thức TCP/UDP, framing, bắt tay | Trước khi sửa bất kỳ thứ gì ở `net/` |
| [2](#2-bản-đồ-thread-cần-hiểu-trước-khi-sửa-concurrency) | 9 thread, ai tạo ở đâu | Trước khi sửa concurrency |
| [3](#3-roadmap-thực-thi) | **Phase 0–6 — việc cần làm** | Luôn |
| [4](#4-quy-tắc-làm-việc-cho-ai) | Quy tắc làm việc cho AI | Trước khi sửa code |
| [5](#5-bảng-tra-nhanh-điểm-cần-sửa) | Vấn đề → `file:line` | Khi cần tra nhanh |
| [6](#6-ghi-chú-phát-hiện) | Ghi chú phát hiện | Khi có phát hiện mới |
| [7](#7-kịch-bản-kiểm-thử-thủ-công) | 12 tình huống + đo CPU | **Trước khi tick checkbox** |

### Trạng thái hiện tại

| | |
|---|---|
| **Phase đang mở** | Phase 0 — Chặn máu (bổ sung 0.8–0.16) + Phase 1 |
| **Đã xong** | Phase 0 — 0.1–0.6 |
| **Đang làm** | Phase 0 mở rộng — 0.7–0.16 |
| **Build lần cuối** | 2026-09-29 — build sạch (`install -pl lib` → `package -pl client,host`) |
| **Test** | 0 test. Đã có JUnit 5 + surefire trong `pom.xml` (hạ tầng 1.10 mới xong phần cấu hình) |
| **Quy ước checkbox** | `[x]` xong · `[~]` làm nhưng **chưa đạt yêu cầu** (xem mục đó) · `[ ]` chưa làm |

---

## 0. Dự án là gì

| | |
|---|---|
| **Tên** | screen-sharing-system (`pl.polsl.screensharing`) |
| **Bản chất** | Ứng dụng chia sẻ màn hình 1 chiều, Java Swing, 3 module Maven |
| **Mục tiêu cuối** | **mini-TeamViewer**: điều khiển máy tính từ xa (2 chiều), nhiều client, truyền file, clipboard, âm thanh |
| **Kiến trúc hiện tại** | `lib` (shared) + `host` (server) + `client` (viewer). Host = 1 máy, nhiều client xem. |
| **Kết nối** | TCP cho control/signal, **UDP** cho video (JPEG thô, **không** dùng codec) |
| **Mã hoá** | RSA-2048 (bắt tay) + BCrypt (mật khẩu) + AES-128/CTR (video) |
| **Giao diện** | Swing + FlatLaf dark theme + RxJava state management |

### Module map

```
lib/     pl.polsl.screensharing.lib     Shared: GUI base, net, crypto, payload, icon, utils
host/    pl.polsl.screensharing.host    Server: capture màn hình, stream cho client
client/  pl.polsl.screensharing.client  Viewer: nhận + render stream
```

### Lệnh quan trọng (PowerShell 5.1, không có `&&`)

```powershell
$env:JAVA_HOME="D:\Java"
.\mvnw.cmd -q clean install -pl lib -DskipTests      # BẮT BUỘC: install lib trước
.\mvnw.cmd -q clean package -pl client,host -DskipTests
.\build.cmd          # build host.jar + client.jar vào .bin/ (tự làm cả 2 bước trên)
.\start.cmd          # menu: 1=Host 2=Client 3=rebuild 4=exit
```

> ⚠️ **Không được bỏ bước `install -pl lib`.** `host`/`client` khai báo phụ thuộc `pl.polsl.screensharing:lib:1.0.0`,
> nên `-pl client,host` **không** đưa `lib` vào reactor — Maven sẽ lấy jar `lib` **cũ** trong `~/.m2`.
> Triệu chứng: `cannot find symbol: method stopRunner()` hoặc bất kỳ lỗi nào chỉ xuất hiện khi sửa `lib/`.
> Hoặc dùng `-am` (also-make) thay cho bước trên: `.\mvnw.cmd -q clean package -pl client,host -am -DskipTests`.

> **Verification bắt buộc:** mọi thay đổi phải build sạch trước khi báo xong.
> Project **chưa có test nào** — compile thành công là tiêu chí duy nhất hiện tại.

---

## 1. Giao thức hiện tại (đọc trước khi sửa)

| Tầng | Công nghệ | Vị trí code |
|---|---|---|
| Định khung message | Text line, phân tách `%`, dạng `STATE%payload` | `lib/.../net/SocketState.java:18-30` |
| Control/signaling | TCP | `host/.../net/ServerTcpSocket.java`, `ClientThread.java`, `client/.../ClientTcpSocket.java` |
| Video | UDP, 1 frame = N fragment × 49168 byte | `host/.../net/ServerDatagramSocket.java`, `FrameSenderThread.java`, `client/.../ClientDatagramSocket.java` |
| RSA | 2048, `Cipher("RSA")` = **PKCS1v1.5** | `lib/.../net/CryptoAsymmetricHelper.java:40,48` |
| Mật khẩu | BCrypt cost 10 | `ClientTcpSocket.java:95` / `ClientThread.java:128` |
| Video crypto | AES-128/CTR/NoPadding, IV 16B ghi cuối packet | `lib/.../net/CryptoSymmetricHelper.java:22-39` |
| Payload | JSON (Jackson) → mã hoá → Base64 | `lib/.../net/payload/*` |

**Thứ tự bắt tay (client → host):**
`EXHANGE_KEYS_REQ` → `EXHANGE_KEYS_RES` → `CHECK_PASSWORD_REQ` → `CHECK_PASSWORD_RES` → `SEND_CLIENT_DATA_REQ` → `SEND_CLIENT_DATA_RES` → `WAITING`

Host **không** gắn prefix state khi trả lời — client tự điều khiển máy trạng thái.

**Định nghĩa frame UDP** (`SharedConstants.java:16-19`):
`FRAME_SIZE=49152`, `IV_SIZE=16`, `PACKAGE_SIZE=49136`
Mỗi datagram = `[countOfPackages(1B)][packageIteration(1B)][JPEG data][IV(16B)]` = 49168 B

---

## 2. Bản đồ thread (cần hiểu trước khi sửa concurrency)

| Thread | Tạo ở | Vai trò |
|---|---|---|
| `Thread-TCP-Server` | `ServerTcpSocket.run()` | accept loop, tạo `ClientThread` mỗi kết nối |
| `Thread-TCP-Client-N` | `ServerTcpSocket.run()` | handshake + auth, đọc dòng từ client |
| `Thread-TCP-Signal-N` | `ClientThread` (`:158`) | gửi event về client |
| `Thread-TCP-Server(client)` | `ClientTcpSocket` | state machine phía client |
| `Thread-TCP-Signal(client)` | `ReceiveSignalsThread` | nhận event từ host |
| `Thread-UDP-Host` | `ServerDatagramSocket` | nén JPEG + cắt fragment + đẩy queue |
| `Thread-UDP-Frame` | `FrameSenderThread` | `take()` từ queue → AES → gửi UDP cho mọi client |
| `Thread-UDP-Client` | `ClientDatagramSocket` | nhận UDP → decrypt → ghép khung → decode |
| `VideoCanvasController` | extends `AbstractPerTickRunner` | capture màn hình 60 FPS + vẽ preview |

**State chia sẻ:** `HostState` / `ClientState` dùng RxJava `BehaviorSubject`, expose qua `wrapAsDisposable()`.

---

## 3. Roadmap thực thi

Xếp theo **mức độ ảnh hưởng**. Phase trên phải xong (hoặc ít nhất phần blocking) mới sang phase dưới.

---

### PHASE 0 — Chặn máu (P0) 🔴

*Mục tiêu: hết cháy CPU, hết nuốt lỗi im lặng, hết vỡ khung hình. Đây là nền để mọi thứ sau còn đo được.*

- [x] **0.1 Sửa busy-loop ở `ClientTcpSocket.run()`**
  `client/.../net/ClientTcpSocket.java:76-158` — `while (isThreadActive)` + `switch (socketState)` không có `case WAITING`. Sau handshake, state = `WAITING` (`:153`) → quay vòng vô hạn, **không bao giờ gọi `readLine()`** → cháy 1 core.
  → Thêm `case WAITING:` gọi `readLine()` chặn, dispatch sang `ReceiveSignalsThread` hoặc chờ.

- [x] **0.2 Sửa busy-loop ở `SendSignalsThread`**
  `host/.../net/SendSignalsThread.java:98-100` — `signalEventLoop()` không có `case WAITING` → mỗi client = 1 core cháy.
  → Thêm `case WAITING:` `Thread.sleep()` hoặc dùng `BlockingQueue<SocketState>` cho event.

- [x] **0.3 Cho `AbstractPerTickRunner` dừng được**
  `lib/.../thread/AbstractPerTickRunner.java:17` — `while (true)` không sleep, không cờ dừng. Thread capture **không bao giờ dừng**, kể cả khi đóng app.
  → Thêm `volatile boolean isRunning`, `stopRunner()`, `join()`.

- [x] **0.4 Bỏ `catch (Exception ignored)` ở các chỗ mạng**
  10 chỗ. Nguy hiểm nhất:
  - `host/.../FrameSenderThread.java:47` — client chết/sai IP → im lặng
  - `client/.../ClientDatagramSocket.java:123` — nuốt `SocketTimeoutException` mỗi giây → **stream chết mà UI không báo**
  - `host/.../ServerDatagramSocket.java:123` — nuốt lỗi nén ảnh, lỗi `rawImage == null`
  → Log ở mức `WARN`, đếm lỗi, đẩy lên state để UI hiện.

- [x] **0.5 Sửa mất đồng bộ khung hình (frame desync)**
  `client/.../ClientDatagramSocket.java:53-55, 110-122` — mất gói **cuối** khung ⇒ `countOfPackages` không bao giờ khớp ⇒ `receivedDataBuffer` **không bao giờ reset** ⇒ mọi khung sau nối đuôi vào buffer cũ. Phải disconnect mới sửa được.
  → Thêm cơ chế **resync**: nếu nhận `packageIteration == 1` mà `isStarted == true` ⇒ reset buffer, bỏ khung cũ.
  → Thêm `frameSequence` / timeout để phát hiện khung hụt.

- [x] **0.6 `volatile` cho dữ liệu chia sẻ giữa thread**
  `host/.../controller/VideoCanvasController.java:32` — `rawImage` không `volatile`, ghi bởi thread capture, đọc bởi thread nén (`ServerDatagramSocket.java:170`). Không happens-before ⇒ đọc `BufferedImage` dở dang ⇒ NPE / ảnh xanh.
  → `volatile` cho `rawImage`, `isThreadActive` ở `AbstractDatagramSocketThread`, `AbstractTcpSocketThread`.
  *(Ghi chú: `AbstractTcpSocketThread` không hề có `isThreadActive`; chỗ tương ứng là `ClientTcpSocket:40`.)*

- [ ] **0.7 Bỏ `port` giả khỏi API `createDatagramSocket`** 🔴 *chưa đạt — lần trước chỉ thêm comment*
  `host/.../ServerDatagramSocket.java:146-152` — nhận `(byte[] secretKey, int port)` nhưng gọi `new DatagramSocket()` không truyền `port`. Đang chạy được vì lúc gọi truyền `0` (`AbstractStreamController.java:34`).
  Lần "sửa" trước chỉ thêm dòng comment `// port intentionally unused` ⇒ **API vẫn nói dối**, không đạt yêu cầu của chính mục này.
  → Gỡ tham số `port` khỏi `AbstractDatagramSocketThread.createDatagramSocket`; phía client nhận `udpPort` qua constructor.
  Không để lại API nói dối.

#### 0.8–0.16 — phát hiện khi rà code ngày 2026-09-29 (sau Phase 0)

*Các mục này nghiêm trọng ngang Phase 0 gốc: treo, leak, NPE, stream chết âm thầm.*

- [ ] **0.8 Sửa chuỗi build (build chain)** 🔴
  `pom.xml` + lệnh build trong tài liệu. `host`/`client` phụ thuộc `lib` qua **Maven repository**, không phải reactor ⇒ `-pl client,host` dùng jar `lib` cũ trong `~/.m2`. Lệnh ghi trong ROADMAP/AGENTS trước đây **fail**, khiến "build sạch" báo cáo lại trước là không trung thực.
  → Sửa lệnh trong tài liệu thành `install -pl lib` → `package -pl client,host` (hoặc thêm `-am`).

- [ ] **0.9 `ClientWindow.clientDatagramSocket` không bao giờ được init** 🔴
  `client/.../view/ClientWindow.java:36-37` khai báo field nhưng constructor **không gán** ⇒ `null` ở lần connect đầu.
  `ClientTcpSocket.java:59` chụp nó vào field `final`; `abstractStopAndClear():215-218` bọc cả `isThreadActive = false` lẫn `clientDatagramSocket.stopAndClear()` trong `if (clientDatagramSocket != null)` ⇒ guard **luôn false**.
  Hậu quả: thread TCP không bao giờ dừng, **socket UDP không bao giờ đóng** ⇒ leak mỗi lần connect/disconnect.
  → Bỏ field `final` đã chụp, đọc `clientWindow.getClientDatagramSocket()` tại thời điểm teardown; `isThreadActive = false` phải ngoài `if`.

- [ ] **0.10 Race: thread TCP client có thể chết ngay khi start** 🔴
  `client/.../net/ClientTcpSocket.java:196-197` — `startThread()` **rồi mới** `isThreadActive = true`. Thread mới đọc `while (isThreadActive)` (`:76`) với giá trị default `false` ⇒ `run()` return ngay, socket không đóng, client báo "thành công" nhưng không có stream.
  (`AbstractDatagramSocketThread.start():30` và `ClientThread.start():188` đặt cờ **trước** `super.start()` — chỉ `ClientTcpSocket` sai.)
  → Đặt `isThreadActive = true` trước `startThread()`.

- [ ] **0.11 Sai mật khẩu ⇒ client treo vĩnh viễn** 🔴
  `client/.../net/ClientTcpSocket.java:103-109` — `!res.isValidStatus()` gọi `onFailure` rồi `break` **không đổi `socketState`** (vẫn `CHECK_PASSWORD_RES`) ⇒ vòng lặp quay lại `readLine():185` và block vĩnh viễn vì host không gửi gì thêm. `stopAndClear()` không bao giờ chạy ⇒ **1 socket + 1 thread leak cho mỗi lần nhập sai mật khẩu**.
  → `throw new SocketException("Invalid password")` để rơi vào nhánh `stopAndClear()` sẵn có.

- [ ] **0.12 Thread capture 60fps chết vĩnh viễn, không một dòng log** 🔴
  `lib/.../thread/AbstractPerTickRunner.java:24` — `onTickUpdate()` được gọi **không có try/catch**; bất kỳ exception nào cũng kết thúc vòng lặp và giết thread ⇒ `rawImage` đóng băng, stream chết, **không có gì giải thích trong log**.
  Dễ kích hoạt: `host/.../controller/VideoCanvasController.java:97` guard `width >= 0 && height >= 0` là vô nghĩa với `0x0` (canvas chưa layout) nên `Scalr.resize(img, 0, 0)` vẫn chạy và bị imgscalr từ chối.
  → Bọc `onTickUpdate()` trong try/catch + log `WARN`, `delta--` vẫn phải trừ; sửa guard thành `width > 0 && height > 0`.

- [ ] **0.13 NPE khi dừng UDP socket chưa tạo được** 🔴
  `lib/.../net/AbstractDatagramSocketThread.java:23-24` — `datagramSocket.disconnect()` không null-check.
  `createDatagramSocket` có thể ném `UnoperableException` (vd `BindException` — `Utils.getRandomPortOrDefault` luôn trả về **1024**, rất dễ trùng), nhưng instance nửa vời đã được publish. Mọi lần Disconnect sau đó ⇒ NPE trên EDT.
  → Null-check `datagramSocket` trong `stopAndClear()`.

- [ ] **0.14 `FrameSenderThread` kẹt `take()` vĩnh viễn — leak mỗi vòng start/stop** 🔴
  `host/.../FrameSenderThread.java:39` `sendPackagesQueue.take()` không ai `interrupt()` (dù nó có xử lý `InterruptedException` ở `:47`).
  `ServerDatagramSocket.abstractStopAndClear():160` chỉ `queue.clear()` ⇒ thread vẫn kẹt, và `disposables.dispose()` ở `FrameSenderThread.java:57` là **dead code** (thread không bao giờ thoát tới đó).
  `AbstractStreamController.java:32` tạo `ServerDatagramSocket` **mới** (kèm `FrameSenderThread` mới) mỗi lần bấm Start ⇒ 5 vòng start/stop = 5 thread treo.
  → `ServerDatagramSocket.abstractStopAndClear()` phải `frameSenderThread.interrupt()`.

- [ ] **0.15 `loadImage()` leak resource + "downscale" lại upscale ảnh dọc** 🟠
  `host/.../net/ServerDatagramSocket.java:169-193`:
  - Thiếu `try/finally` ⇒ `jpgWriter.write()` ném lỗi là mất `ImageWriter`, `ImageOutputStream`, `ByteArrayOutputStream` (3 resource **mỗi frame**, ở 60fps).
  - `rawImage` có thể `null` (chưa có tick capture nào) ⇒ NPE.
  - `ImageIO.getImageWritersByFormatName("JPEG").next()` không `hasNext()` ⇒ `NoSuchElementException`.
  - Resize luôn nhắm `MAX_FRAME_WIDTH`, `MAX_FRAME_HEIGHT` chỉ dùng để so sánh ⇒ ảnh dọc 800x1200 bị resize thành **1600x2400** (diện tích ×4), tức **tăng** băng thông.
  → Bọc `try/finally`, null-check `rawImage`, chọn mốc resize theo cả 2 chiều.

- [ ] **0.16 `ServerTcpSocket.sendSignalToClient` dereference client có thể không còn** 🟠
  `host/.../net/ServerTcpSocket.java:74-77` không null-check `connectedClients.get(threadId)`, khác hẳn `sendSignalToAllClients:66-70`. Gọi từ `ParticipantsController.java:50` khi client rớt giữa lúc chọn dòng và bấm Kick ⇒ NPE trên EDT.
  → Null-check + log.

**Definition of Done phase 0:** chạy host + 3 client 5 phút → CPU mỗi luồng < 5%, không còn máy chết khi ngắt mạng giữa chừng, log không còn dòng trống, **sai mật khẩu 10 lần liên tiếp không rò thread/socket**, **start/stop stream 10 lần không rò thread**.

---

### PHASE 1 — Nền tảng vững (P1) 🟠

*Mục tiêu: code đáng tin, đo được, test được. Cần thiết trước khi thêm tính năng.*

- [ ] **1.1 Tách rõ 3 loại lỗi mạng**
  `CONNECTION_LOST` (client rớt) / `STREAM_STALLED` (TCP còn sống nhưng UDP không về) / `PROTOCOL_ERROR` (dữ liệu hỏng). Mỗi loại có state riêng + UI riêng.

- [x] **1.2 Sửa `ParticipantsController.removeSelectedParticipant()`**
  `host/.../controller/ParticipantsController.java:28-29` đọc `table.getValueAt(getSelectedRow(), ...)` **trước** khi check bounds ở `:39` → `ArrayIndexOutOfBoundsException` khi chưa chọn dòng nào. Tên biến cũng sai (`:44` là `keySet` chứ không phải `connectedClientInfo`).
  ✅ Đã chuyển check bounds lên trước. *(Xem 1.9 về cách khớp dòng để kick.)*

- [~] **1.3 Đồng bộ ghi TCP**
  `ClientThread` (`:74`) và `SendSignalsThread` (`:96`) tạo 2 `PrintWriter` khác nhau trên **cùng 1 socket output** → có thể xen kẽ byte, corrupt line. Cần 1 writer dùng chung + `synchronized`.
  ✅ Đã gộp về 1 `PrintWriter` + `sendLine()` synchronized. **NHƯNG vẫn hỏng:** `ClientThread.run()` còn giữ try-with-resources trên `PrintWriter` ⇒ khi vòng `while` thoát nó **đóng luôn writer dùng chung**, còn `SendSignalsThread` đang cần writer đó. `PrintWriter` nuốt `IOException` **im lặng** (chỉ set cờ `trouble`) ⇒ `KICK_FROM_SESSION` / `END_UP_SESSION` biến mất không log.
  → Bỏ try-with-resources khỏi `ClientThread.run()`, để `stopAndClose()` đóng socket một lần (đóng socket đã đóng cả 2 stream).

- [x] **1.4 `disposeAllSubscriptions()` cho mọi thread**
  `lib/.../state/AbstractDisposableProvider.java:21` — chỉ được gọi trong `GuiWindowAdapter.java:22`. Subscription tạo trong constructor của `ClientThread`, `FrameSenderThread`, `ClientDatagramSocket` **không bao giờ dispose** → leak mỗi lần connect.
  ✅ Đã thay `wrapAsDisposable` bằng `CompositeDisposable` riêng cho `ClientThread`, `FrameSenderThread`, `ServerDatagramSocket`, `ServerTcpSocket`, `ClientDatagramSocket`.
  ⚠️ Riêng `FrameSenderThread` vẫn là **dead code** vì thread không bao giờ thoát khỏi `take()` — xem 0.14.

- [~] **1.5 Dọn thread leak + hỗ trợ `interrupt()`**
  - `ClientThread.stopAndClose()` không dừng `SendSignalsThread`
  - `ServerTcpSocket.abstractStopAndClear()` không `join()` thread nào
  - `VideoCanvasController` không bao giờ dừng (xem 0.3)
  - **Grep `InterruptedException` trong toàn repo = 0 kết quả.** Không thread nào dừng được bằng `interrupt()`.
  ✅ Đã thêm `interrupt()` + `join()` cho `SendSignalsThread`; `AbstractPerTickRunner` đã hỗ trợ `interrupt()`; `stopRunner()` đã được gọi qua shutdown hook.
  **NHƯNG vừa tạo bug mới:** `stopAndClose()` được gọi từ **cả** `ClientThread.run():99` **và** `SendSignalsThread.run():109`. Khi nó chạy **trên** `SendSignalsThread` thì `sendSignalsThread.join(2000)` là join vào chính nó — `isAlive()` luôn true ⇒ chờ trọn **2 giây** rồi mới return. Mỗi lần client rớt là treo 2s.
  → Guard `if (Thread.currentThread() != sendSignalsThread)`.
  ⚠️ `FrameSenderThread` vẫn không dừng được (xem 0.14).

- [x] **1.6 Bổ sung `volatile` cho timer**
  `host/.../controller/BottomInfobarController.java:16-17` — `sessionTime`/`streamingTime` là `long` (64-bit) được ghi bởi EDT và đọc bởi chính EDT nên hiện chưa lộ, nhưng dễ vỡ khi tách thread. Làm cùng đợt 0.6 cho nhất quán.
  ✅ `volatile long`.

- [x] **1.7 Đưa `stopSessionTimer()` vào `stopStreamingTimer()`**
  `host/.../controller/BottomInfobarController.java:46-49` — `stopStreamingTimer()` gọi `streamingTimer.stop()` nhưng **không** dừng `sessionTimer`. Session timer cứ chạy tiếp sau khi ngừng stream ⇒ thanh "Session time" vẫn đếm ⇒ người dùng tưởng vẫn đang trong phiên.
  ✅ Đã thêm `sessionTimer.stop()` + reset `sessionTime`.

- [~] **1.8 Dừng session sạch khi đóng app**
  `lib/.../gui/GuiWindowAdapter.java:18-25` — `windowClosing` chỉ `disposeAllSubscriptions()` rồi `System.exit(0)`. **Không** dừng `ServerTcpSocket`, `ServerDatagramSocket`, `VideoCanvasController`. Client đang xem không nhận được `END_UP_SESSION` → treo tới timeout, màn hình đen không giải thích được.
  ✅ Đã thêm `getShutdownHook()` vào `AbstractRootFrame` + `GuiWindowAdapter`; `HostWindow`/`ClientWindow` truyền hook dừng socket + `stopRunner()`.
  **NHƯNG `END_UP_SESSION` vẫn không tới client, và lần sửa còn làm tệ hơn:** `sendSignalToAllClients(END_UP_SESSION)` chỉ **set cờ** `eventSignalState`; `SendSignalsThread` poll mỗi 50ms, còn socket bị `closeSocket()` ngay sau. ⇒ giao hàng **thuần ngẫu nhiên**. Lần sửa này còn **chuyển lời gọi từ cuối lên đầu** `abstractStopAndClear()` ⇒ càng giảm xác suất gửi được.
  → Phải gửi **đồng bộ + flush** trước khi đóng socket, không dựa vào cờ async.

- [~] **1.9 Sửa cột Thread ID trong bảng người tham gia**
  `host/.../view/dialog/ParticipantsDialogWindow.java:33,75` — cột "Thread ID" hiển thị số ngẫu nhiên 5 chữ số (`ClientThread.java:193-199`), vô nghĩa với người dùng. Tốn 20px cột mà không giá trị.
  → Bỏ hẳn cột này, đổi thành thời điểm vào phiên (giờ phút) — thông tin này hữu ích và nhìn ra con người hơn.
  ✅ Đã đổi sang cột "Joined" (`ConnectedClientInfo.joinTime`).
  ⚠️ Còn 2 vấn đề: (a) `setColumnWidth(0, 20)` **vẫn còn** — di sản của Thread ID, giờ vô nghĩa; (b) kick giờ khớp bằng `ip:port + username` ⇒ **mơ hồ**, hai client cùng username cùng IP (đúng kịch bản T3 mở 2 client) sẽ kick nhầm người vì `findFirst()` trả về phần tử tuỳ ý. `threadId` thì duy nhất.

- [~] **1.10 Thêm test tối thiểu**
  Hiện **0 test** trong 146 file.
  → Thêm JUnit 5 + surefire vào parent `pom.xml` trước, rồi viết test:
  - `SocketState` framing (round-trip `generateBody`/`extractHeader`/`extractContent`)
  - `CryptoSymmetricHelper` (encrypt→decrypt, nhiều kích thước)
  - Ghép frame tự động (mô phỏng mất gói → khẳng định resync hoạt động)
  - `equals`/`hashCode` của `SavedConnection`
  ✅ Mới xong **hạ tầng** (JUnit 5 + surefire trong `pom.xml`). **4 test vẫn chưa viết** ⇒ chưa đạt.

- [ ] **1.11 Chuẩn hoá config path**
  `lib/.../AppType.java:14-15` dùng `"host.json"` **relative** → chạy từ Desktop là rác file. Chuyển sang thư mục cấu hình cố định.
  → Áp dụng cho cả `.logs/` (`host/.../logback.xml:8` cũng đang relative).

#### 1.12–1.15 — phát hiện khi rà code ngày 2026-09-29

- [ ] **1.12 Client `receivedImage` thiếu `volatile`** 🟠
  `client/.../controller/VideoCanvasController.java:20-22` — ghi từ UDP thread (`ClientDatagramSocket.java:122`), đọc từ EDT trong `drawContent()`. Không happens-before ⇒ `BufferedImage` dở dang có thể tới EDT.
  *(0.6 chỉ sửa `rawImage` phía host, bỏ sót phía client.)*

- [ ] **1.13 Swing bị mutate ngoài EDT** 🟠
  - `host/.../view/dialog/ParticipantsDialogWindow.java:95-107` — observer `getConnectedClientsInfo$` gọi `model.setRowCount(0)` / `addRow` / `setEnabled`; emitter là `ClientThread.java:162,212` tức **TCP thread của từng client**.
  - `host/.../net/ServerDatagramSocket.java:120` — `JOptionPane.showMessageDialog` từ **UDP thread**.
  - `client/.../net/ClientTcpSocket.java:151` → `AbstractPopupDialogController.java:34-35` `closeWindow()/dispose()` từ TCP thread.
  → Bọc các khối mutate Swing vào `SwingUtilities.invokeLater`.

- [ ] **1.14 `commons-lang3` dùng ở 17 file nhưng không khai báo ở đâu cả** 🟠
  `pom.xml:86-90` chỉ khai `commons-text` — thứ mà **không file nào import** (0 usage). 17 file import `org.apache.commons.lang3.*` (`lib/.../Utils.java:7-8`, `lib/.../file/FileUtils.java:7-8`, `client/.../model/FastConnectionDetails.java:8-9`, …) chỉ build được vì `commons-text` kéo `commons-lang3` vào **transitively**.
  ⇒ Bom transitive: gỡ `commons-text` là vỡ 17 file.
  → Khai báo thẳng `commons-lang3`, bỏ `commons-text`.

- [ ] **1.15 `Utils.calcSizeBaseAspectRatio` trả `Dimension` âm trước khi layout** 🟡
  `lib/.../Utils.java:40-53` — `containerHeight = getHeight() - 10`; component chưa layout thì `getHeight()` = 0 ⇒ `containerHeight` = **-10**. `(double) containerWidth / containerHeight` với 0 cho `Infinity` (không ném lỗi) nhưng kết quả là `Dimension` âm truyền vào `setPreferredSize` (`client/.../controller/VideoCanvasController.java:31-35`, `host/.../controller/VideoCanvasController.java:52-55`).
  Được gọi từ observable `getFrameAspectRatio$` bắn lúc connect, **trước** khi frame layout.
  → Guard `width/height <= 0`, trả về kích thước dự phòng.

---

### PHASE 2 — Bảo mật (P2) 🟡

*Mục tiêu: đủ cơ sở để gọi là "remote access tool" mà vẫn an toàn.*

- [ ] **2.1 AES-CTR → AES-GCM**
  `lib/.../net/CryptoSymmetricHelper.java:22-39` — CTR là mã hoá stream, **không có xác thực**. Attacker trên LAN flip bit tùy ý trong datagram, client vẫn "giải mã thành công" → ảnh bị sửa hoặc crash `ImageIO`.
  → Đổi `AES/GCM/NoPadding`, tag 16 byte, tránh lại IV/nonce trùng.

- [ ] **2.2 RSA PKCS1v1.5 → OAEP**
  `lib/.../net/CryptoAsymmetricHelper.java:40,48` — `Cipher.getInstance("RSA")` mặc định PKCS#1 v1.5, **có lỗ hổng Bleichenbacher**. (AGENTS.md ghi OAEP nhưng code thực tế không phải.)
  → `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`.

- [ ] **2.3 Hash mật khẩu thay vì lưu plaintext**
  `host/.../model/SessionDetails.java:47` — `host.json` lưu `Base64(plaintext)`. Đọc file là có mật khẩu.
  → Chỉ lưu BCrypt hash, không lưu plaintext.

- [ ] **2.4 Challenge-response chống replay**
  `client/.../ClientTcpSocket.java:95-96` — client gửi BCrypt hash mỗi lần connect. Hash **dùng lại được vĩnh viễn**, ai sniff handshake thì giả lập được client.
  → Host gửi nonce, client trả lời dạng HMAC(nonce, hash).

- [ ] **2.5 Giới hạn số client + timeout**
  `host/.../net/ServerTcpSocket.java:52-54` accept vô hạn, mỗi kết nối = 2 thread. Client mở socket rồi im = giữ thread vĩnh viễn. Đây là đường DoS.
  → Giới hạn số client đồng thời + `setSoTimeout()` cho handshake.

- [ ] **2.6 Xác thực server**
  Client không có cách nào biết mình đang nói chuyện với host thật hay kẻ MITM. → Fingerprint + trao đổi khoá công khai dài hạn.

---

### PHASE 3 — Hiệu năng (P3) 🟢

*Mục tiêu: chạy mượt 1080p30 trên WAN, không giật, không đốt CPU.*

- [ ] **3.1 Resize ảnh 1 lần thay vì 2–3 lần**
  - Host: `VideoCanvasController.java:98` resize **mỗi tick 60fps** chỉ để preview, xong `ServerDatagramSocket.java:177-180` resize lần nữa.
  - Client: `client/.../VideoCanvasController.java:43` — `Scalr.resize` **mỗi lần repaint** dù ảnh đã đúng kích thước.
  → Tách: 1 ảnh đã resize đúng kích thước stream dùng chung cho cả preview lẫn gửi đi. Client cache ảnh đã scale.

- [ ] **3.2 FPS cấu hình được + đo FPS thật của stream**
  `lib/.../thread/AbstractPerTickRunner.java:6` — `MAX_FPS = 60.0` **hardcode**. UI hiện FPS lấy từ thread capture, **không phải** FPS thật của stream.
  → Cho user chọn 15/30/60 FPS; đếm FPS đo ở `ServerDatagramSocket` sau khi gửi.

- [ ] **3.3 Backpressure + giới hạn băng thông**
  `host/.../ServerDatagramSocket.java:118` chỉ `sleep(1)` — tốc độ gửi phụ thuộc CPU, không phải FPS. `sendPackagesQueue` là `ArrayBlockingQueue(100)`, client chậm là `put()` block cả thread nén.
  → Token bucket theo băng thông người dùng chọn; queue có timeout thay vì block vô hạn.

- [ ] **3.4 `byte` → `short`/`int` cho counter fragment**
  `host/.../ServerDatagramSocket.java:61-62` — `countOfPackages`/`packageIteration` là `byte` ⇒ **giới hạn 127 fragment/khung**, vượt là tràn số âm ⇒ client ghép sai khung.

- [ ] **3.5 Bỏ `System.gc()`**
  6 chỗ, gồm `ServerDatagramSocket.java:130` — gọi **mỗi 6 giây trong vòng lặp**. Gây pause không kiểm soát.

- [ ] **3.6 `getRandomPortOrDefault` để OS cấp port**
  `lib/.../Utils.java:87-100` — quét tuần tự từ 1024, mở rồi đóng socket để dò ⇒ TOCTOU race.
  → `new DatagramSocket(0)` rồi `getLocalPort()`.

- [ ] **3.7 Gửi vùng thay đổi (dirty region)**
  Hiện gửi **toàn bộ màn hình** mỗi frame kể cả khi không có gì thay đổi. Chỉ gửi phần vừa đổi là bước nhảy lớn nhất về băng thông.

---

### PHASE 4 — Giao diện 🟣

*Mục tiêu: người dùng luôn biết đang ở trạng thái nào. Không bao giờ im lặng khi hỏng.*

- [ ] **4.1 Thanh trạng thái kết nối thật**
  Hiện `VisibilityState` chỉ có 3 giá trị (`client/.../state/VisibilityState.java`). Stream chết vẫn hiện "đang kết nối".
  → Thêm `STALLED` (TCP sống, UDP không về) + banner cảnh báo kèm nút reconnect.

- [ ] **4.2 Báo lỗi kết nối chi tiết**
  `client/.../AbstractPopupDialogController.java:55-57` — mọi lỗi đều ra chung `"Cannot connect... Check connection parameters."`
  → Phân biệt: sai IP, sai port, sai mật khẩu, host đã đầy, firewall chặn UDP.

- [ ] **4.3 Bảng người tham gia chịu được dữ liệu xấu**
  Username **không được host validate** (`ClientThread.java:145-168`), chỉ lọc regex phía client (`SharedConstants.java:10`, chặn cả chữ hoa). Client gửi tên bất kỳ → hiển thị thẳng vào bảng.
  → Validate + escape phía host, giới hạn độ dài, cho phép ký tự Unicode.

- [ ] **4.4 Hiện IP động thay vì IP tĩnh**
  `host/.../view/dialog/SessionInfoDialogWindow.java:73` hiện `sessionDetails.getIpAddress()` — **string tĩnh đã lưu**. Đổi mạng thì host vẫn hiện IP cũ.
  → Theo dõi `NetworkInterface`, cập nhật UI khi IP đổi, cảnh báo session đang bind IP cũ.

- [ ] **4.5 Fix bug UI còn lại**
  - `client/.../view/dialog/ConnectWindow.java:86` + `LastConnectionsWindow` dùng `AppType.HOST` thay vì `CLIENT` → sai icon, sai file config
  - `lib/.../gui/fragment/JAppTabbedLogsPanel` — `textArea` là `public static`, dùng chung cho mọi cửa sổ
  - `lib/.../gui/component/JAppLink` giữ URI null sau khi log lỗi → `Desktop.browse(null)` NPE
  - `client/.../model/SavedConnection` — `hashCode()` chứa `description` nhưng `equals()` không ⇒ vi phạm contract

---

### PHASE 5 — Nền cho mini-TeamViewer 🔵

*Mục tiêu: mở đường cho tính năng 2 chiều mà không phải viết lại từ đầu.*

- [ ] **5.1 Đặt lại tên + version giao thức**
  `SocketState` mới có 12 state, thêm tính năng sẽ phình. `EXHANGE_KEYS_REQ` còn typo.
  → `PROTOCOL_VERSION` trong bắt tay, từ chối client phiên bản cũ. Chuẩn hoá tên state.

- [ ] **5.2 Tách `VideoChannel` khỏi `SocketThread`**
  Hiện `ServerDatagramSocket` vừa nén vừa cắt fragment vừa quản lý queue. Không thể thêm kênh thứ 2 (input/clipboard) mà không đụng code cũ.
  → Interface chung `Channel { open(); loop(); close(); }`, mỗi loại dữ liệu một kênh riêng.

- [ ] **5.3 Registry kênh mở rộng**
  Đăng ký `VIDEO`, `INPUT`, `CLIPBOARD`, `FILE`, `AUDIO`, `CHAT` theo cùng một interface. Thêm kênh mới = thêm 1 class, không sửa code cũ.

- [ ] **5.4 Đa luồng input có kiểm soát**
  `Robot` chỉ tạo 1 lần (`host/.../gfx/ScreenCapturer.java:40`) và không thread-safe về `createScreenCapture` khi nhiều thao tác. Cần 1 input pump riêng, có rate-limit.

- [ ] **5.5 Quản lý băng thông nhiều client**
  Hiện `FrameSenderThread.java:40-46` mã hoá **1 lần rồi gửi cùng byte cho mọi client**. Đúng khi mọi client cùng key, nhưng sẽ vỡ khi cần key/quality khác nhau.
  → Tách phần mã hoá và phần định tuyến.

---

### PHASE 6 — Tính năng mini-TeamViewer 🟠🟡

*Làm theo thứ tự giá trị/công sức. Mỗi tính năng phải đi qua hết Phase 0–5 trước.*

- [ ] **6.1 Điều khiển chuột + bàn phím (2 chiều)** ⭐ *giá trị cao nhất*
  Đây là thứ biến "xem màn hình" thành "điều khiển máy tính". Hiện project **stream 1 chiều, không có gì ngược lại**.
  → Kênh INPUT, chuột (move/click/wheel/drag), bàn phím (scan code → key event), cần dùng `java.awt.Robot` phía host.

- [ ] **6.2 Resize chất lượng thích ứng**
  → Đo băng thông thực, tự hạ quality/FPS khi nghẽn, tự nâng khi rảnh.

- [ ] **6.3 Clipboard đồng bộ 2 chiều**
  → Chặn clipboard qua `ClipboardListener`, chống loop bằng sequence number.

- [ ] **6.4 Truyền file**
  → Kênh riêng, chunk + checksum + resume. Nên chạy qua TCP hoặc kênh UDP riêng, không trộn vào video.

- [ ] **6.5 Multi-monitor + chọn màn hình khi kết nối**
  Hiện host tự chọn monitor (`CaptureSettingsController.java:28-41`), client không có quyền chọn.

- [ ] **6.6 Sổ ghi chú kết nối (address book)**
  `client.json` đã lưu `savedConnections` nhưng IP tĩnh, không tự cập nhật. → Tự dò host trong LAN (mDNS/broadcast), lưu theo tên máy.

- [ ] **6.7 Reconnect tự động + resume**
  Khi rớt mạng, thử lại với backoff, giữ nguyên thông tin session.

- [ ] **6.8 Chuyển codec sang H.264**
  JPEG thô tốn ~5–10× băng thông so với H.264. Đây là **bước nâng cấp lớn nhất** cho chất lượng, nhưng cần thư viện ngoài.
  → Cân nhắc thư viện phía native (x264/ffmpeg) hoặc OpenH264; cần đổi framing + thêm packet loss concealment.

- [ ] **6.9 Âm thanh**
  → Kênh AUDIO, capture ở 44.1kHz, encode.

- [ ] **6.10 Chạy headless / như service**
  Hiện `ScreenCapturer` phụ thuộc `GraphicsDevice` — không chạy được trên máy không có màn hình. Cần tách lớp capture khỏi Swing.

---

## 4. Quy tắc làm việc (cho AI)

1. **Đọc ROADMAP.md trước khi sửa gì.** Làm đúng phase đang mở, không nhảy phase.
2. **Không sửa file ngoài phạm vi task.** Đặc biệt đừng refactor lan sang module khác.
3. **Build sạch trước khi báo xong — đúng thứ tự, không bỏ bước:**
   ```powershell
   $env:JAVA_HOME="D:\Java"
   .\mvnw.cmd -q clean install -pl lib -DskipTests
   if ($?) { .\mvnw.cmd -q clean package -pl client,host -DskipTests }
   ```
   Nếu bỏ bước `install -pl lib`, lệnh sau **vẫn có thể xanh** dù `lib` đã hỏng — và đó là nguồn gốc của một báo cáo "build sạch" sai.
4. **Sửa 1 mục → tick checkbox → commit.** Không gộp nhiều phase vào 1 commit.
5. **Không dùng `&&`** — PowerShell 5.1. Dùng `;` hoặc `if ($?) { }`.
6. **Giữ nguyên hành vi runtime** khi sửa lỗi, trừ khi mục đó nói rõ đổi hành vi.
7. **Đừng ghi comment thừa.** Code tự giải thích.
8. **Ghi lại phát hiện mới vào mục "Ghi chú" cuối file** thay vì sửa lại cả roadmap.
9. **Hỏi trước khi làm thay đổi kiến trúc** (đổi protocol, đổi format frame, thêm dependency ngoài).

### Khi hoàn thành một phase
- Tick toàn bộ checkbox trong phase đó
- Cập nhật mục "Trạng thái hiện tại" ở dưới
- Commit message theo convention repo: `fix:`, `feat:`, `refactor:`, `build:`, `docs:`

---

## 5. Bảng tra nhanh điểm cần sửa

| Vấn đề | File:line |
|---|---|
| Lệnh build `-pl client,host` dùng jar `lib` cũ | `pom.xml` — phải `install -pl lib` trước hoặc thêm `-am` |
| `clientDatagramSocket` luôn null ⇒ leak UDP + thread không dừng | `client/.../view/ClientWindow.java:36-37`, `ClientTcpSocket.java:59,215-218` |
| `startThread()` trước `isThreadActive = true` (race) | `client/.../net/ClientTcpSocket.java:196-197` |
| Sai mật khẩu ⇒ kẹt `readLine()` vĩnh viễn | `client/.../net/ClientTcpSocket.java:103-109` |
| `onTickUpdate()` không try/catch ⇒ thread capture chết im lặng | `lib/.../thread/AbstractPerTickRunner.java:24` |
| Guard resize `width >= 0` vô nghĩa với `0x0` | `host/.../controller/VideoCanvasController.java:97` |
| `stopAndClear()` NPE khi `datagramSocket == null` | `lib/.../net/AbstractDatagramSocketThread.java:23-24` |
| `FrameSenderThread` kẹt `take()`, không ai `interrupt()` | `host/.../net/FrameSenderThread.java:39`, `ServerDatagramSocket.java:160` |
| `loadImage()` leak 3 resource khi ném lỗi | `host/.../net/ServerDatagramSocket.java:169-193` |
| Resize luôn nhắm `MAX_FRAME_WIDTH` ⇒ upscale ảnh dọc | `host/.../net/ServerDatagramSocket.java:179-181` |
| `sendSignalToClient` không null-check | `host/.../net/ServerTcpSocket.java:74-77` |
| try-with-resources đóng writer dùng chung của `SendSignalsThread` | `host/.../net/ClientThread.java:82-85` |
| `stopAndClose()` join vào chính nó ⇒ treo 2s | `host/.../net/ClientThread.java:218-220` |
| `END_UP_SESSION` set cờ async ⇒ không bao giờ tới client | `host/.../net/ServerTcpSocket.java:105` (moved to top) |
| Client `receivedImage` thiếu `volatile` | `client/.../controller/VideoCanvasController.java:20-22` |
| Bảng participants mutate ngoài EDT | `host/.../view/dialog/ParticipantsDialogWindow.java:95-107` |
| `JOptionPane` từ UDP thread | `host/.../net/ServerDatagramSocket.java:120` |
| `commons-lang3` chưa khai báo (chỉ nhờ transitive) | `pom.xml:86-90` |
| `calcSizeBaseAspectRatio` trả `Dimension` âm | `lib/.../Utils.java:40-53` |
| Kick mơ hồ khi trùng username + IP | `host/.../controller/ParticipantsController.java:42-49` |
| `setColumnWidth(0, 20)` di sản của Thread ID | `host/.../view/dialog/ParticipantsDialogWindow.java:75` |
| Busy-loop client (đã sửa, nay ngủ 100ms) | `client/.../net/ClientTcpSocket.java:76-162` |
| Busy-loop signal (đã sửa, nay ngủ 50ms) | `host/.../net/SendSignalsThread.java:39-95` |
| Thread capture không dừng được | `lib/.../thread/AbstractPerTickRunner.java:42-45` |
| Khung hình desync (đã có resync) | `client/.../net/ClientDatagramSocket.java:100-104` |
| NPE bảng người tham gia (đã sửa) | `host/.../controller/ParticipantsController.java:26-30` |
| 2 PrintWriter trên 1 socket (đã gộp) | `host/.../net/ClientThread.java:73-77` |
| Subscription leak (đã có CompositeDisposable) | `lib/.../state/AbstractDisposableProvider.java:21` |
| `System.gc()` trong loop | `host/.../net/ServerDatagramSocket.java:131` |
| `getRandomPortOrDefault` luôn trả 1024 (dễ `BindException`) | `lib/.../Utils.java:87-100` |
| Counter fragment là `byte` | `host/.../net/ServerDatagramSocket.java:61-62` |
| FPS hardcode 60 | `lib/.../thread/AbstractPerTickRunner.java:6` |
| `Timer` không giữ reference ⇒ không tắt được | `lib/.../Utils.java:110-119` |
| `file://${user.home}/.m2` khai như remote repo | `pom.xml:42-47` |
| Đóng app không gửi `END_UP_SESSION` (đã thêm hook, chưa hiệu quả) | `lib/.../gui/GuiWindowAdapter.java:18-42` |
| Regex username chặn chữ hoa | `lib/.../SharedConstants.java:10` |
| AES-CTR không xác thực | `lib/.../net/CryptoSymmetricHelper.java:43` |
| RSA PKCS1v1.5 (Bleichenbacher) | `lib/.../net/CryptoAsymmetricHelper.java:40,48` |
| Mật khẩu lưu Base64 | `host/.../model/SessionDetails.java:47` |
| Hash mật khẩu replay được | `client/.../net/ClientTcpSocket.java:95` |
| Accept không giới hạn (DoS) | `host/.../net/ServerTcpSocket.java:52-54` |
| IP hiển thị là string tĩnh | `host/.../view/dialog/SessionInfoDialogWindow.java:73` |
| Sai `AppType` trong client UI | `client/.../view/dialog/ConnectWindow.java:86` |
| Config lưu relative CWD | `lib/.../AppType.java:14-15` |
| `.logs/` cũng là relative path | `host/.../logback.xml:8` |

---

## 6. Ghi chú phát hiện

*(Ghi phát hiện mới vào đây, không sửa lại các phase đã xong.)*

- 2026-09-29: Lập roadmap. Project có **0 test**, 146 file Java, compile sạch.
  Đã xác nhận các lỗi P0/P1/P2/P3 ở trên bằng cách đọc code, chưa sửa gì.
  Branch hiện tại: `appmod/java-upgrade-20260926090626`, working tree sạch.

- 2026-09-29: **Hoàn thành Phase 0.** Build sạch. Chi tiết sửa:
  - **0.1** `ClientTcpSocket`: thêm `case WAITING: Thread.sleep(100)` — hết cháy 1 core client
  - **0.2** `SendSignalsThread`: thêm `case WAITING: Thread.sleep(50)` — hết cháy 1 core/client trên host
  - **0.3** `AbstractPerTickRunner`: thêm `volatile isRunning`, `stopRunner()`, `Thread.sleep(1)` khi idle, hỗ trợ `interrupt()`
  - **0.4** 3 chỗ `catch (Exception ignored)` → log `WARN`; `SocketTimeoutException` → log `DEBUG` + reset state
  - **0.5** `ClientDatagramSocket`: thêm resync — khi nhận `packageIteration==1` mà buffer chưa trống → reset buffer, đếm corrupted
  - **0.6** `volatile` cho: `rawImage` (VideoCanvasController), `isThreadActive` (AbstractDatagramSocketThread, ClientTcpSocket, ClientThread)
  - **0.7** `ServerDatagramSocket.createDatagramSocket`: giữ nguyên signature nhưng thêm comment ghi rõ port intentionally unused

- 2026-09-29: Rà lần 2, bổ sung các mục còn thiếu:
  - **0.7** `createDatagramSocket` bỏ qua tham số `port` (lần trước bỏ sót khỏi roadmap dù đã nêu trong chat)
  - **1.7** `stopStreamingTimer()` quên dừng `sessionTimer` → "Session time" đếm tiếp sau khi ngừng stream
  - **1.8** `windowClosing` không dừng socket/session → client treo, màn hình đen không giải thích được
  - **1.9** Cột "Thread ID" trong bảng participants hiển thị số ngẫu nhiên, vô nghĩa với người dùng
  - **1.10** Không chỉ thiếu test — mà còn thiếu cả JUnit + surefire trong `pom.xml`
  - **1.11** `.logs/` (`logback.xml:8`) cũng là relative path, cùng bện với config

- 2026-09-29: Ghi nhận các hạn chế kiến trúc còn lại, chưa xếp vào phase vì phụ thuộc đường đi ở Phase 5:
  - **Sử dụng `ObjectMapper` mới ở mỗi thread** (`ClientThread.java:61`, `SendSignalsThread.java:36`, `ClientTcpSocket.java:64`) — mỗi cái giữ cache class riêng, tốn bộ nhớ khi nhiều client. Nên dùng 1 instance dùng chung (ObjectMapper an toàn khi dùng đa luồng sau khi cấu hình xong).
  - **`LogbackTextAreaAppender` append vào `public static` JTextArea** dùng chung cho mọi cửa sổ — khi thêm nhiều cửa sổ, log của chúng trộn vào nhau và có rủi ro `IllegalStateException` khi sửa Swing ngoài EDT.
  - **Không có `volatile`/lock nào bảo vệ `HostState`** khi nhiều `ClientThread` cùng gọi `updateConnectedClients()` — hiện chỉ an toàn vì mọi client cùng dùng chung 1 `DatagramKey` và 1 byte đã mã hoá.
  - **Bảng participants không có thứ tự** — `ConcurrentHashMap` không bảo đảm thứ tự chèn, nên danh sách nhảy vị trí mỗi lần có client mới. Cần cấu trúc giữ thứ tự join.

- 2026-09-29: **Rà code lần 3 sau một vòng sửa Phase 1.** Kết luận: phần cosmetic làm đúng, nhưng **3 mục tự nhận "xong" thực ra hỏng hoặc tạo bug mới**, và 5 lỗi P0 cũ chưa đụng tới. Chi tiết đã đưa vào Phase 0 (0.8–0.16) và Phase 1 (1.12–1.15):
  - **0.7 chưa đạt** — chỉ thêm comment, API vẫn nói dối. Đã bỏ tick, viết lại tiêu chí.
  - **0.1/0.2 làm nhưng lệch spec** — roadmap yêu cầu `readLine()` chặn / `BlockingQueue`; code dùng `Thread.sleep(100)` / `sleep(50)` ⇒ vẫn là poll loop, chỉ chậm hơn. Giữ tick `[x]` vì đã hết cháy CPU (kết quả thực tế đạt), nhưng ghi rõ trong mục.
  - **1.3 tạo bug mới** — gộp về 1 `PrintWriter` là đúng hướng, nhưng `try-with-resources` trong `ClientThread.run()` giờ đóng **writer dùng chung** ⇒ `SendSignalsThread` ghi vào writer đã đóng, `PrintWriter` nuốt lỗi im lặng ⇒ `KICK`/`END_UP_SESSION` biến mất không log.
  - **1.5 tạo bug mới** — `stopAndClose()` được gọi từ cả hai thread, nên `sendSignalsThread.join(2000)` có thể là join vào chính nó ⇒ treo 2s mỗi lần client rớt.
  - **1.8 làm vô hiệu và tệ hơn trước** — `sendSignalToAllClients(END_UP_SESSION)` chỉ set cờ, thread poll 50ms, socket đóng ngay sau ⇒ giao hàng ngẫu nhiên. Bản sửa còn chuyển lời gọi từ cuối lên đầu `abstractStopAndClear()` làm giảm thêm xác suất. Đánh dấu `[~]`.
  - **1.4 một nửa** — `FrameSenderThread` có `CompositeDisposable` nhưng `dispose()` là dead code vì thread kẹt `take()` mãi (xem 0.14).
  - **Bốn "test" của 1.10 chưa viết** — mới chỉ có hạ tầng. Đánh dấu `[~]` cho khỏi báo cáo quá.
  - **`setColumnWidth(0, 20)` còn sót** sau khi đổi cột Thread ID → "Joined". Và `CellEditableModel(..., 0, 2)` thực ra là **disable** cột 0–2 (tham số tên là `colStartDisabled`) nên **không** phải bug như thoáng nhìn — đã loại khỏi danh sách.
  - **Phát hiện thêm ngoài roadmap cũ:** `clientDatagramSocket` null gây leak UDP; race `startThread()`/`isThreadActive`; sai mật khẩu treo vĩnh viễn; `onTickUpdate()` không try/catch giết thread capture; `stopAndClear()` NPE; `FrameSenderThread` leak mỗi vòng start/stop; `loadImage()` leak + upscale ảnh dọc; `sendSignalToClient` NPE; `receivedImage` client thiếu volatile; Swing ngoài EDT; `commons-lang3` chưa khai báo; `calcSizeBaseAspectRatio` trả `Dimension` âm.
  - **Bài học về quy trình:** báo "build sạch" mà không chạy đúng lệnh là vô nghĩa. Lệnh `-pl client,host` **không** build lại `lib`; HEAD thậm chí không compile được vì `HostWindow.java:93` gọi `stopRunner()` còn `AbstractPerTickRunner` bản commit chưa có. Phải luôn `install -pl lib` trước.

---

## 7. Kịch bản kiểm thử thủ công

Chạy `.\build.cmd` rồi `.\start.cmd` để mở host + client. Dùng các kịch bản này để kiểm tra trước khi tick checkbox.

| # | Kịch bản | Cách làm | Kết quả mong đợi |
|---|---|---|---|
| T1 | Kết nối cơ bản | Host: tạo session → Client: điền IP:port → Connect | Client thấy màn hình host, host thấy 1 dòng trong Participants |
| T2 | **Sai mật khẩu** | Host bật "Has Password" + đặt mật khẩu, client nhập sai | Báo "Invalid password", không vào được phiên |
| T3 | **3 client cùng lúc** | Mở 3 client, cùng IP/port | Cả 3 thấy stream, host thấy 3 dòng, kick 1 người → 2 người còn |
| T4 | **Ngắt mạng giữa chừng** | Đang stream → tắt Wi-Fi client hoặi kill client đột ngột | Host bỏ client khỏi danh sách; client còn lại **vẫn nhận stream bình thường** |
| T5 | **Restart stream** | Đang stream → Stop → Start lại | Client nhận lại stream, **không bị vỡ hình** |
| T6 | **Ẩn/hiện màn hình** | Bấm nút ẩn màn hình | Client thấy "Screen temporary hidden by host", host không gửi frame |
| T7 | **Chọn vùng** | Chuyển sang chế độ capture vùng, kéo khung | Chỉ vùng đó được stream |
| T8 | **Đổi chất lượng** | Poor → Good → Best khi đang stream | Không giật, không rơi kết nối |
| T9 | **Screenshot** | Client: chụp màn hình | Lưu được file jpg đúng nội dung đang xem |
| T10 | **Chốt session** | Host: xoá session hoặc đóng app khi client đang xem | Client báo "Session has been ended", **không treo màn hình đen** |
| T11 | **Đổi IP (host)** | Stream đang chạy → đổi mạng | Xác định hành vi: hoặc báo lỗi rõ ràng, hoặc tự phục hồi (xem 4.4) |
| T12 | **Sai IP/port** | Client nhập IP không tồn tại | Báo lỗi **phân biệt được** (sai IP vs sai port vs sai mật khẩu) |
| T13 | **Sai mật khẩu nhiều lần** | Nhập sai mật khẩu 10 lần liến tiếp, mỗi lần đóng app rồi mở lại | Mỗi lần báo "Invalid password" rồi **thoát hẳn**, không treo. *(0.11)* |
| T14 | **Start/Stop stream nhiều lần** | Bấm Start → Stop → Start → … 10 vòng | Số thread không tăng dần. *(0.14)* |
| T15 | **Đóng host khi client đang xem** | Client đang stream → bấm X trên host, chọn Yes | Client nhận "Session has been ended" **ngay lập tức**, không treo màn hình đen. *(1.8)* |
| T16 | **Trỏi trong bảng participants** | 2 client cùng tên, cùng IP:port | Kick đúng người đang chọn, không kick nhầm. *(1.9)* |
| T17 | **Mở app rồi đóng ngay** | Mở host → tạo session → Start stream → đóng app trong vòng 1 giây | Không exception nào trong log. *(0.12, 0.13)* |

### Đếm thread để phát hiện leak (kiểm tra 0.14, 0.9, 1.5)

```powershell
Get-Process java | Select-Object Id, @{n='Threads';e={$_.Threads.Count}}, CPU
```

Chạy T14 xong, số thread phải **quay về mức ban đầu**. Nếu tăng đều mỗi vòng Start/Stop ⇒ `FrameSenderThread` chưa được `interrupt()`.

### Đo CPU (kiểm tra Phase 0)

```powershell
Get-Process java | Select-Object Id, CPU, WorkingSet64
```

Lấy mức CPU trước và sau 10 giây. Mỗi luồng nên **< 5%**. Trước khi sửa Phase 0, host + 1 client sẽ đốt hết các core.

### Xem log

- Console: cửa sổ chạy `start-host.cmd` / `start-client.cmd`
- File: `.logs/host/host.log`, `.logs/client/client.log`
- Trong app: tab "Logs"

Sau khi sửa mục 0.4, log phải **có** dòng báo lỗi mạng thay vì im lặng.
