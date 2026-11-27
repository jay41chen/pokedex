# Devlog

## 2026-11-27（repository、`:data` Hilt binding、Hilt＋Navigation 整合）

**時間**：RD-app Hilt＋Navigation 整合 10:34–12:16（含模擬器驗證與修正輪）。RD-data R 段 09:35–09:47（Do 寫檔約 3 分鐘），修正輪 09:50–09:57。RD-data H 段 10:1x–10:2x，H2b 修正 10:24–10:27。

**做了什麼**
- RD-data R 段：repository 與讀取用的 DAO（feat(data): add Pokemon and Capture repositories with R1-R7 tests `d0efd71`；rebase 前為 `af8a02c`）。
  - AI 產出：`PokemonDao`（`@Transaction` 加 `@Relation` 一次讀完 types，detail 與 types 不會分兩次送出）、`PokemonRepositoryImpl`、`CaptureRepositoryImpl`（UUID 加 `TimeSource`）、`PokedexDatabase.pokemonDao()`；schema 沒變。新增 R1–R4 instrumented 測試。
  - Gradle 耗時（舊 MBA）：doer 的 unit 加 androidTest 編譯 26 秒、connected 26 秒；Judge 的 unit（`--rerun-tasks`）21 秒、connected 12 秒、每個 mutation 8–19 秒。
  - Judge：通過。4 個 mutation 都被抓到：catalog 的 JOIN 改 LEFT JOIN（R1）、species 的 LEFT JOIN 改 JOIN（R2）、evolves-from 接錯欄位（R3）、capture id 改用 pokemon id（R4）。低嚴重度 3 項：evolves-from 那隻不在表裡時回 null 沒標 [待確認]、catalog 的 `ORDER BY p.id` 沒註明、三個行為沒有測試鎖住。
  - 人決定：低嚴重度 3 項全補（PO 原本建議只補 slot 排序）。
- RD-data R 段修正輪：新增 R5–R7，兩處補 [待確認] 註解，正式邏輯沒改；connected 13/13 通過（doer `--rerun-tasks` 16 秒，Judge 每次 6–10 秒）。
  - Judge：複審通過。mapper 拿掉 slot 排序時 R5 抓不到（DB 依主鍵讀回已經排好），由 `MappersTest` 抓到，低嚴重度。`PokemonRepositoryImpl` 是新檔，無法用 diff 證明只加了註解，Judge 逐行讀過確認邏輯一致。
- PO：spec 測試清單加入 R1–R7，不標 ★（docs: list R1-R7 repository tests in spec, log R stage and time breakdown `b2ef8d3`）。
- RD-data H 段：`:data` 的 Hilt binding、網路、reporter、logger（feat(data): add Hilt DataModule, connectivity, cache-evicting reporter and logging `088c5f2`，含 H2b 兩輪修正）。
  - AI 產出：`DataModule`（`internal object`，全部 `@Provides @Singleton`；Hilt 接受 `internal`，實作類別不用改成 public）、`AndroidConnectivityChecker`、`CacheEvictingReporter`、`DataLogger`；sync 的 5 個 TODO 換成 log，清掉 [待確認] 與多餘的一行；`data/build.gradle.kts` 加 `dagger.fullBindingGraphValidation=ERROR`、開 buildConfig（debug 才開 OkHttp log），沒加依賴。新增 H1、H2；unit 50 個全過，`:app:assembleDebug` 成功，`:app` 的 Hilt 元件已收進 `DataModule`（Judge 看產生的程式碼確認）。
  - Gradle 耗時：doer 的 unit 加 app 編譯 34 秒；Judge 基準 24 秒，H1 連跑 3 次各 6–9 秒，mutation 各 3–19 秒。
  - Judge：通過。retry 拿掉「每次嘗試前檢查取消」被 H1 抓到（連跑 3 次穩定）。兩個 mutation 沒抓到，都是規劃時憑印象假設函式庫行為：
    - reporter 拿掉 try/catch：H2b 的情境（OkHttp `Cache.delete()` 後呼叫 `urls()`）以為會丟例外，實際上 `delete()` 會先 close，之後 `urls()` 回傳空的，情境沒走到 catch。
    - 拿掉 `TimeSource` 的 provider：`fullBindingGraphValidation` 實際只抓重複綁定與 scope 錯誤，不抓缺少的 provider（單獨驗 module 時，缺的可能由別的 module 提供）。Judge 實測：刪 provider 時 ksp、compile、`:app:assembleDebug` 全過；多綁一次則 ksp 報 `[Dagger/DuplicateBindings]`。參數保留，防 `:app` 重複綁定。
  - 人決定：修 H2b；不加 `@EntryPoint` 提早驗缺 provider，等 RD-app 加 `@HiltViewModel` 時由 `:app` 編譯自然驗到（Judge 已人工核對依賴圖完整）。
- RD-data H2b 修正：cache 目錄改指向普通檔案（實測丟 `FileNotFoundException`），並斷言有記到 `"Cache eviction failed"` log；正式程式沒改。doer unit 16 秒；Judge 基準 10 秒。複審通過：拿掉 try/catch、catch 裡不記 log 兩個 mutation 都被抓到。低 1 項（H2b 沒呼叫 `cache.close()`、H2a 有，寫法不一致），RD 建議不修；人決定修（怕之後改寫法時漏關），測試結尾補 `cache.close()`，只改測試檔。
- RD-data H2b 補 close：10:28–10:31。H2b 改成 try/finally，finally 裡 `runCatching { cache.close() }`。doer unit 9 秒；Judge 基準 8 秒、mutation 7 秒。複審通過，沒有發現；拿掉 reporter 的 try/catch 仍被 H2b 抓到，證明 `runCatching` 只包住 close。
  - 意外：doer 第一次 Edit 因沒先 Read 而失敗，接著跑的 Gradle 用的是舊檔；doer 自己發現、重改重跑，並如實回報。
- RD-app Hilt＋Navigation 整合（feat(app): wire Hilt and Navigation, share OkHttpClient with Coil `ae2d86b`）。
  - 時間：Do 10:34–10:40、首審 10:41–10:45、模擬器驗證 10:47–11:10、修正輪 Do 11:40–11:50、複審 11:42–12:08（含模擬器）、N1 KDoc 12:13–12:16。Gradle：`assembleDebug --rerun-tasks` 約 14–17 秒，`testDebugUnitTest --rerun-tasks` 約 11–16 秒。
  - AI 產出：`UseCaseModule`（7 個 `@Provides`）、3 個 VM 加 `@HiltViewModel`、`PokedexNavHost`、MainActivity 在 `onStart` 呼叫 `onAppForegrounded`、`PokedexApp` 實作 Coil `SingletonImageLoader` 並接 `:data` 的共用 `OkHttpClient`。沒有出現 MissingBinding。
  - 意外：`DetailViewModel` 的 `@Assisted` 參數用 `PokemonId`（value class），KSP 報 `not a valid name: create-JYpEi7E`（Kotlin 對 value class 參數的函式做 name mangling，Dagger 產生的 Java 碼實作不了）。人決定照官方 recipe 傳整個 `DetailRoute`。
  - Judge 首審：通過，低 5 項。拿掉 `onStart` 呼叫、Coil 自建 client、拿掉 `@HiltViewModel` 三個 mutation 編譯與測試都抓不到；缺 provider 在 `:app` 編譯時報 MissingBinding（補上 H 段 `fullBindingGraphValidation` 抓不到的那一塊）。
  - 模擬器驗證：需修正，中 2 項。F1：連點兩次返回把 back stack 彈空，畫面全白。F2：DB 已有資料時冷啟動，sync 比 Room 第一次 emit 早結束，VM 把初始空 list 當成「DB 空」，先閃「暫時無法載入資料」。圖片確認經過共用 client。
  - 修正輪：F1 加 `runIfResumed` guard 加 2 個測試；F2 在 VM 內部加「catalog 已第一次 emit」旗標，新增 V6、V6b，`HomeUiState` 與契約不動；`:app` 測試 13 個全過。複審通過；guard 沒包在返回鍵上的 mutation 測試抓不到，只有模擬器能重現空白畫面。
  - N1（複審低）：返回首頁 0.5–1 秒內點卡片會被丟掉。查證後官方解法就是 lifecycle-runtime-compose 2.8 的 `dropUnlessResumed`，語意與 `runIfResumed` 相同，N1 是官方做法本身的取捨；人決定不改程式，只在 KDoc 註明（自己寫是因為官方只接受無參數 lambda）。Judge 對照 2.11.0 sources jar 確認一致。
  - 人決定先不修、交給 backlog 或 NOTES：詳細頁 `#005`（mock 是 `#5`）、卡片名稱字色與字重、列表右側沒露出半張卡、最右欄寶貝球觸控寬約 42.7dp、描述斷行、N2（一次單擊返回沒反應，重現不了）、import 順序、冷啟動慢（推測主機 load 高）。
- PO：spec 測試清單加 V6，`CatalogStatus.Loading` 註解補上「catalog 尚未第一次 emit」（F2 的根因是「DB 空」有歧義；語意與型別不變）（docs: add V6 and clarify Loading in spec, log Hilt and Navigation integration `e2679cc`）。
- RD-data FI 段：debug 限定的故障注入，用來在模擬器上驗 cache eviction（feat(data): add debug-only fault injection for cache eviction checks `a161339`）。
  - AI 產出：`FaultInjectionInterceptor` 讀 `filesDir/fault_urls.txt`，URL 完全相符時把 body 換成 `{}`；release build 是空的 stub。unit 50 → 53。
  - Judge：通過，3 個 mutation 都抓到；低 4 項人決定不修。
- RD-data FT 段：描述保留 `\n`（fix(data): keep line breaks in flavor text descriptions `38abb5b`）。12:30 開始，Judge 12:44–12:46。
  - AI 產出：`toSpecies` 把 `\r\n`、`\r` 轉成 `\n`、`\f` 轉空白，只合併空白與 tab，去掉換行前後的空白；DB 存原文不變。新增 M3c（Charizard 原文）、M3d；unit 53 → 55。
  - Judge：通過。還原成 `\s+`、拿掉 `\f` 轉空白、拿掉去除換行前後空白，3 個 mutation 都抓到；curl 抽查 6 隻的第一筆英文描述，Charizard 與 mock 一致。
  - 低 2 項：理由註解沒標 [待確認]（人決定修成只引用 spec）；NBSP 等 Unicode 空白沒處理（人決定列進 spec backlog，known issue）。
- RD-app 對齊 mock 段（feat(app): align UI with mock and size tiles from available width `020e435`）。Do 12:23–12:25，編譯＋測試 12:27–12:28，Judge 12:28–12:42。
  - AI 產出：卡片寬度依可用寬度算，至少 3 張完整再露半張；Pocket 空時用看不見的佔位卡撐高度；寶貝球觸控 48dp；`#5` 不補零；名稱 bodySmall 灰字；詳細頁大圖 min(寬 × 45%, 240dp)、名稱 titleLarge。新增 TileWidthTest（2）、DetailFormatTest（1），16 個全過。
  - Judge：通過。補零、拿掉半張、拿掉至少 3 張，3 個 mutation 都抓到；直向、橫向、平板 800dp、字級 1.3 四種截圖逐項對照 mock，寶貝球實測 48×48dp。
  - 意外：AI 原本要在 UI 加描述換行的格式化，只查了 `RemoteMappers`，沒發現 `DomainMappers` 已把 `\n` 換成空白；PO 抓到後拿掉，改由 RD-data 的 FT 段處理。
- RD-app low 修正段（fix(app): scroll pocket to newest capture, cap detail image by height `ad17d87`）。Do 12:48–12:49，Judge 12:50–13:10。
  - AI 產出：TileWidthTest 預期值寫死（原本用和實作同一條公式算）；大圖再加 高 × 40% 上限；捕捉時設旗標，Pocket 第一張改變時 `animateScrollToItem(0)`；Preview 改用有換行的 Charmeleon 描述。
  - Judge：通過。目標寬改 110 → 100 被測試抓到；拿掉高度上限、拿掉捲動兩個 mutation 在模擬器重現（名稱被擠出第一屏、新卡只露 12px）。描述換行在模擬器確認：Charizard、Charmeleon 各 5 行，斷行與 mock 一致。
  - 低 2 項（連續捕捉的理論競態，實測 0.2／0.4／0.7 秒都沒重現；捕捉失敗時旗標殘留）。
- RD-app 寫入失敗記 log 段（fix(app): log capture and release write failures instead of crashing `2ddb17f`）。Do 13:16–13:17，Judge 13:18–13:23。
  - 起因：`HomeViewModel` 的捕捉／釋放用 `viewModelScope.launch` 沒有錯誤處理，Room 寫入一丟例外 app 就 crash。人決定只記 log、不顯示 Snackbar。
  - AI 產出：`:app` 自己的 `AppLogger`（`:data` 的 `DataLogger` 是 internal）、`LoggingModule`；VM 用 `CoroutineExceptionHandler` 記 warn。新增 V7、V8，18 個全過。
  - Judge：通過。拿掉兩邊的 handler、handler 吞掉例外但不記 log，3 個 mutation 都抓到。沒有 handler 時例外只印到 stderr、測試不會紅，紅燈全靠 logger 斷言；doer 寫完時就提出這個疑問，Judge 實測確認。
  - 低 4 項人決定不修；Room 寫入失敗在真機上無法重現，這條路徑只有 JVM 測試證明。
- PO：spec 記下描述換行、M3c／M3d、Unicode 空白 backlog、寫入失敗記 log 與 V7／V8、Evolves from 載入中跳 Snackbar（從 backlog 拉進來）（docs(spec): record flavor text, write-failure logging and Evolves from snackbar decisions `e3a3a65`）。
- RD-data MV 段（手動驗證，HEAD `2ddb17f`）：13:29–13:44，installDebug 42 秒。證據截圖與 logcat 留在本機，不進 git。
  - MV1 kill 後續抓：通過。kill 前抓完 1–6 號，重開後第一批請求是 7–11，沒有重抓 list 和 1–6。`am kill` 對剛按 HOME 的 app 沒作用（pid 不變），改用 `run-as kill -9`。
  - MV2 全部抓完再開：新 process 的 OkHttp 請求數 0。
  - MV3 首次開 app 在飛航模式：空狀態文字有出現，關掉飛航模式切回 app 後自動載入；1fps 連拍 25 秒都沒拍到 Snackbar，RD-data 回報為疑似 bug。RD-app 逐格拆錄影（每 0.25 秒）後確認 Snackbar 有出現：錄影 14.5 秒出現、17.0 秒淡出，約 2 秒；**不是 bug**。`SnackbarDuration.Short` 是 4 秒，若截圖真是每秒一張不可能漏掉 2 秒的畫面，所以截圖漏掉、以及只顯示約 2 秒的原因都還沒確認 [待確認]：可能是模擬器過載（logcat 裡系統行程也大量 `Skipped frames`）讓 `screencap` 間隔遠大於 1 秒、進場動畫被吃掉計時，或截圖與錄影不是同一次啟動。
  - MV4 連線洩漏：三份 logcat 搜不到 "leaked"；OkHttp 要等 GC 才印，只算弱證據。
  - MV5 cache eviction：pokemon/25 被換成 `{}` 時記永久性失敗，journal 該 key 依序 DIRTY → CLEAN → REMOVE，Electric 列少了 Pikachu；拿掉故障後只重抓 pokemon/25 和它的圖，Pikachu 回來。
  - 附帶：按 HOME 幾秒後背景請求全部 `UnknownHostException`，推測是 API 37 對背景 app 的網路限制 [待確認]，sync 歸成暫時性失敗、回前景補抓；Pikachu 插到列最前面時列表保持原位，沒跳回開頭。
- RD-app MV3 重跑（證據留在本機）：冷啟動 debug build 在模擬器上 Choreographer 報 432–484 個 skipped frames（7–8 秒），Snackbar 看得到約 2 秒。原因查到：Material3 1.4.0 `SnackbarHost` 在資料出現時就開始 `delay(4000L)`，用真實時間倒數，主執行緒卡住時倒數照走。人決定維持 `Short`（推測是開發機負載，release 與真機沒量過）。
- RD-app V5 改名（test(app): prefix V5 test name to match spec `459c50e`）。
- RD-app Evolves from 載入中 Snackbar（feat(app): show loading snackbar when tapping Evolves from before its detail arrives `a1415e3`）：沒載入完時點擊不導頁，跳「資料還在載入，稍後再試」，不排隊。
- RD-app 無障礙修正（fix(app): give Evolves from a button role and click label, drop duplicate image description `251e0d9`）。
- PO：Compose UI test 列入測試依賴，spec 加 U1–U5（build: add Compose UI test libraries; list U1-U5 screen tests in spec `23064d9`）；[ADR 0008](design/0008-compose-ui-tests.md)，修訂 0007（`9291dca`）。
- PO：catalog 釘 `concurrent-futures` 1.2.0（`c72bae9`）：`profileinstaller` 帶 1.1.0、`androidx.test.ext:junit` 1.3.0 要 1.2.0，AGP 要求 test APK 與 app 依賴版本一致。
- PO：catalog 釘 `espresso-core` 3.7.0（`fe25334`）：`ui-test-junit4` 帶進的 3.5.0 在 API 37 呼叫已移除的 `InputManager.getInstance`，7 個 connected 測試全在 idle 同步時丟 `NoSuchMethodException`。spec 補 androidTest 方法名用 `U1_camelCase`（minSdk 26 的 DEX 不接受方法名有空白）。
- RD-app U1–U5 Compose UI test（test(app): add Compose UI tests U1-U5 for snackbars, detail states and ball tap `3d02707`），commit 16:17。connected 跑在 API 37 模擬器。
  - 基準：單元測試 18 個全過；connected 7 個全過，總時間 2:53。
  - Judge：通過。5 個 mutation 都抓到：拿掉不排隊的 guard（U2）、不回報 `MessageShown`（U3）、隱藏 Evolves from（U4b）、先送 `MessageShown` 再顯示（U3）、點球同時開詳細頁（U5）。
  - 意外：doer 第一次跑 connected 時 U4a 在 teardown 失敗（Activity 停在 PAUSED，沒進 DESTROYED），重跑就過。Judge 從 logcat 推論是模擬器負載：Activity 啟動 6.2 秒、PAUSED 到 DESTROYED 7.5 秒 [待確認]。
  - 低 3 項待人決定：U4c 沒驗空的 Text 節點；時間點寫死 `Short` = 4000ms；單個 detail 測試要 20–30 秒，有偶發逾時風險。
**決定**
- R1–R7 列進 spec 測試清單，不標 ★。
- DI 維持原分工：`:data` 提供自己的 Hilt binding，實作類別保持 `internal`（人問過是否該由 RD-app 做，討論後維持）。
- 缺 provider 不另加 `@EntryPoint` 提早驗，整合時由 `:app` 編譯驗到。
- 轉場中的點擊防護照官方 `dropUnlessResumed` 的語意，接受「轉場期間點擊被丟掉」的取捨。
- spec 加 V6、釐清 `Loading` 定義（人同意）。
- 描述保留 `\n`、`\f` 轉空白（對照 mock 的 Charizard、Charmeleon）；轉換放在 domain mapper，DB 存原文。
- 捕捉／釋放寫入失敗只記 log，不顯示 Snackbar、不 crash。
- 「點了還沒載入的 Evolves from 跳 Snackbar」從 backlog 拉進來做。
- flavor\_text 的 Unicode 空白列為 known issue（backlog）。
- Snackbar 維持 `SnackbarDuration.Short`（MV3 重跑後，人決定）。
- `:app` 加 Compose UI test（[ADR 0008](design/0008-compose-ui-tests.md)），只在改到 `ui/` 下 composable 時跑 U1–U5。
- 傳遞依賴衝突用 catalog 釘版解決：`concurrent-futures` 1.2.0、`espresso-core` 3.7.0。

**AI 協作紀錄**（NOTES 第 1 題素材）
- 這兩筆不是人推翻 AI 的提議，而是 AI 規劃時憑印象假設函式庫行為、沒有先驗證，由 Judge 的 mutation 抓出：`fullBindingGraphValidation` 的作用、H2b 的例外情境（細節見上方 H 段）。
- RD-app 的 AI 只查了一個 mapper 就下結論（描述換行），PO 對照 `DomainMappers` 抓到。
- AI 寫的 TileWidthTest 用和實作同一條公式算預期值，公式錯時測試跟著錯；Judge 列為低，人決定改成寫死。
- MV3 的誤判：截圖沒拍到 Snackbar 就被當成 bug，逐格看錄影才確認有出現；「1fps 剛好漏掉」的解釋 AI 直接接受了，PO 對照 4 秒的 duration 才發現算不通。驗證方法本身的取樣率也要檢查。

**未決・卡住**
- U1–U5 低 3 項等人決定。
- 待實測還剩：離線時按返回再立刻重開、離線且 DB 空時旋轉、上方插入整列新 type（MV5 只驗了列內插入）。
- NOTES 第 1–5 題草稿待人審；第 6 題等時數。

**下一步**
- 補驗剩下的待實測。
- PO：NOTES 第 6 題、README 截圖後 commit、更新 HANDOVER。

## 2026-11-26（一面、範圍議定、契約凍結）

**時間**：Phase 0a：18:04–18:09（Do 約 3 分鐘、Judge 約 1.5 分鐘）。Phase 0b：18:12–18:36（含契約修訂與兩輪 Judge）。

**時間分配**（NOTES 第 6 題素材）：面談前的分析和討論當時沒記起訖時間，下表是事後從 AI 對話紀錄的時間戳推算的。兩則訊息相隔不到 20 分鐘就算在工作，所以包含 AI 執行和人閱讀的時間；面談本身、離線讀規格、寫 email 不在內。

| 階段 | 時間 |
| --- | --- |
| 面談前：讀題、試打 API、對照 mock（11/18） | 約 1.0h |
| 面談前：資料設計：schema、REPLACE／RESTRICT、species 與名字對照（11/18–11/19） | 約 1.3h |
| 面談前：sync 設計：offline first、重試、觸發時機、local default（11/19–11/23） | 約 1.2h |
| 面談前：架構分層：三 module、命名（11/24） | 約 0.7h |
| 面談前：範圍、砍單順序、估時（11/19、11/24） | 約 0.7h |
| 面談前：email 提問與定稿（11/18–11/19） | 約 1.2h |
| 面談前：面談準備：STAR 確認、Part 1 題目、預演（11/18–11/19）；訪綱（11/26） | 約 1.2h |
| 提案與方案比較（11/26 面談前） | 約 0.4h |
| 面談後定案與五輪契約（11/26） | 約 1.6h |
| 建 repo、HANDOVER、PO／RD 分工設定（11/26） | 約 0.8h |
| Phase 0 與 RD 平行實作（11/26 17:40–21:26，牆鐘） | 約 3.5h |

- 面談前跟題目直接相關的設計（讀題到範圍估時）約 4.9h；email 與面談準備約 2.4h。
- 只算 spec 的部分（提案、方案比較、五輪契約）約 2h；加上實作，約 5.5h。
- 五輪契約能在 1.6h 內談完，是因為 schema、upsert／RESTRICT、三 module 分層在面談前就討論過了（11/18–11/24）。

**做了什麼**
- 第一次技術面談，議定交付範圍，結論寫在 ADR 0002、0003。
- 和 AI 逐輪討論 module 之間的契約，共五輪，全部凍結，寫在內部的 tech spec。
- 建立 repo，加入 `.gitignore`、`NOTES.md` 骨架、ADR 0001–0007；ADR 0001、0003–0006 補 mermaid 圖（docs: add ADRs 0001-0007 with diagrams, devlog, and NOTES skeleton `1af8b72`）。
- tech spec 搬進 repo 成為 `docs/spec.md`；新增專案 `CLAUDE.md`（PO＋RD-data＋RD-app 分工）與 `.claude/agents/doer.md`、`judge.md`（docs: move tech spec into repo, add CLAUDE.md and doer/judge agents `59ba297`）。
- Phase 0a：三 module Gradle 骨架（`:app` / `:domain` / `:data`），version catalog 放齊已同意套件（`623e437`）。
  - AI 產出：build 設定、wrapper、`PokedexApp` 與空的 `MainActivity`；Room 2.8.5、Coil 3.6.3 由 AI 查最新穩定版。
  - Judge：通過，3 個低嚴重度：`:domain` 的 coroutines 應用 `api`（併入 0b）、多了未回報的 foojay settings plugin、只有佔位 theme（交給 RD-app）。
  - 編譯時間：首次 `assembleDebug` 1m12s（含下載依賴），有快取 0.9s，`--rerun-tasks` 13s。
- Phase 0b：`:domain` 全部（model、三個 repository 介面、7 個 use case、`groupByType`）與 D1 測試 5 個（`224e580`）。
  - AI 產出：照 spec 第 1–3 輪逐字實作；mutation 自測 3 個皆被抓到。
  - Judge 首審：通過，但指出 spec 契約本身的風險：`results` 是 `SharedFlow(replay = 0)`，冷啟動離線時結果在 HomeViewModel 建立前就發出而遺失，空畫面卡在 `Loading`。
  - 契約修訂：sync 結果改成狀態 `status: StateFlow<SyncStatus>`（Idle／Running／Finished(round, result)），「顯示過了沒」留在 VM（ADR 0006 修訂）。
  - Judge 複審：需修正（只有文件），補上「HomeViewModel 晚於 onStart 建立」的前提與手動驗證、旋轉時不退回 `Loading`、`lastShownRound` 的推進規則、check-and-set 要原子。
- Phase 0 完成（`224e580`），開 worktree：`../pokedex-data`（`data`）、`../pokedex-app`（`app`），session 名稱 `po`／`rd-data`／`rd-app`。
- RD-data mapper 階段：18:43–18:48（Do 3 分鐘、Judge 2 分鐘）（`c8ddc62`）。
  - AI 產出：3 個 DTO、5 個 Room entity（尚無 DAO）、雙向 mapper、`MappersTest` 12 個（含 M1–M4，用 PokéAPI 真實 fixture）。
  - Judge：通過；3 個新 mutation 有 2 個漏網（151 邊界 `<=` 改 `<`、對照表 32／122 未測、`idFromUrl` 測試分不出取法），低嚴重度。
  - 修正輪 18:59–19:01：人決定 4 項全補；補 32／122、151／152 邊界、`/api/2/` URL 三組斷言，測試 14 個全過，複審通過。RD 自己在交給 doer 前發現原本的 `/api/v2/` 例子測不出取錯段而改正。
- RD-app HomeViewModel 階段：18:44–18:48（Do 2 分鐘、Judge 1.5 分鐘）（`30e627f`）。
  - AI 產出：`HomeContract`（照 spec 第 3 輪）、`HomeViewModel`（單一 private 狀態 + `update {}`）、手寫 fake repository、V1–V4。
  - Judge：通過；漏網 mutation「Success 不清掉舊 message」，低嚴重度 3 項（該行為無測試、fake 有死碼、MainDispatcherRule 隱含依賴 Unconfined）。
  - 人決定：`@HiltViewModel` 延到 Hilt 整合階段；`Finished(Success)` 清掉尚未顯示的舊 message（spec 未定義，PO 已補進 spec 第 3 輪與 ADR 0006，新增 V5）。
  - 修正輪 18:58–19:01：補 V5（Success 清掉未顯示的舊 message）、刪 fake 死碼、MainDispatcherRule 加 KDoc；測試 5 個全過，複審通過，剩一個未用 import。
- RD-data DB 與 SyncStore 階段：19:24–19:43（Do 2 分鐘；首審含 instrumented 6 分鐘；修正 2 分鐘；複審 5 分鐘）（`bdd25ba`）。
  - AI 產出：`PendingWork`、`SyncStore`、`PokedexDatabase` v1（schema 匯出）、`SyncDao`／`CaptureDao`（只用 `@Upsert`）、`RoomSyncStore`；B1–B5 instrumented 6/6 通過。
  - Judge 首審：需修正。中：B4 的插入順序讓「依 `c.id` 排序」也能過，守不住 rowid 排序規則；低：B2 catch 範圍太寬。修正後複審通過。
  - 實測佐證（NOTES 第 2 題素材）：改成 `@Insert(REPLACE)` 時因 capture 的 RESTRICT 失敗，B3 擋下；B2 實際丟出 FOREIGN KEY constraint failed (787)。
  - 決定：`writeDetail` 不先刪舊 type（補缺只寫一次）；每日更新（B）時要改，已記進 spec backlog。
- RD-app MainViewModel／DetailViewModel 階段：19:25–19:29（Do 2 分鐘、Judge 1.5 分鐘）（`69b3e97`）。
  - AI 產出：`MainViewModel`、`DetailContract`（照 spec 第 3 輪）、`DetailViewModel`（id 由 `SavedStateHandle` 讀）、1 個測試；`@HiltViewModel` 與 `onStart` 接線延到 Hilt 整合。
  - Judge：通過；漏網 mutation `filterNotNull()`（契約裡 detail 不會變回 null），低嚴重度 3 項。
  - 修正輪 19:44–19:46：3 項全修；人決定 `DetailViewModel` 的 id 改由畫面傳入（Hilt 整合時用 assisted injection），不再從 `SavedStateHandle` 讀字串 key，型別由編譯器保證；新增「detail 變回 null 時回到 Loading」測試。複審通過。
- RD-app 首頁 UI 階段：19:51–20:07（Do 1.5 分鐘、Judge 1 分鐘，之後截圖與低嚴重度修正）（`b0e2e25`）。
  - 人決定：這階段只用 Preview 驗證；畫面文字照 spec 用中文、全部放 `strings.xml`；寶貝球用自己畫的 vector。
  - AI 產出：`HomeScreen`（無狀態，含 Snackbar 與 5 個 Preview）、`PocketSection`（固定 160dp，key 用 captureId）、`CatalogContent`（type 列 key 用 type、列內 key 用 id，加 `animateItem`）、`PokemonTile`（Coil，有佔位圖）、`ic_pokeball.xml`；不接 VM、不新增依賴。沒有自動化 UI 測試（Compose UI test 要新增依賴）。
  - Judge：通過；逐條檢查 12 項全過（key 不重複、`MessageShown` 在 `showSnackbar` 回傳後才送、Capture／Release 沒接反、文字與 spec 一致、`:app` 沒有 import `:data`）；低嚴重度 6 項。Judge 讀不到 mock 圖，與 mock 的比對由人看 Preview。
  - 語意確認：Snackbar 顯示中又來一輪同樣的 `Offline`，`message` 是同一個 enum 值、StateFlow 去重，只顯示一次；符合「較新一輪取代尚未顯示的 message」，人確認接受。
  - 模擬器截圖（19:58–20:01）：人當時不在電腦前，改用 debug source set 裡的暫時 harness 截 7 張圖，用完已刪除、模擬器已關、字級已還原。結構與 mock 一致。系統字級 1.5 倍時「Charmander」在 96dp 卡片被截成「Charman…」，人決定不修，寫進 NOTES。卡片底色、edge-to-edge 狀態列圖示顏色、紫色分隔線併到 theme 階段；寶貝球 32dp 觸控範圍留到 Hilt 整合後手動驗證。
  - 低嚴重度修正（Do 20:05–20:06、複審 20:07）：人選 3 項（`rememberUpdatedState`、重複的 null 判斷、完整套件名改 import），其餘延後。複審通過，assembleDebug 與 Home 5 個、Detail 2 個單元測試通過，沒有新問題。
- RD-data retry interceptor 階段：19:49–20:04（Do 3 分鐘、修正 2 分鐘、首審因模擬器排隊暫停約 10 分鐘、複審 2 分鐘）（`5c3750e`）。
  - AI 產出：`Sleeper`（internal）與 `ThreadSleeper`、`RetryInterceptor`（最多 4 次，退避 1／2／4 秒 ±50%，只重試 500..599 與 429，重試前 close，每次嘗試前檢查取消）、`RetryInterceptorTest` 13 個（真的 OkHttpClient 加腳本 responder，追蹤 body 是否 close）。
  - doer 回報規格歧義，人決定 2 點：等待後的取消檢查和迴圈頂端重複，刪掉（原本的 mutation 抓不到它）；負數 `Retry-After` 會讓 `Thread.sleep` 丟例外、繞過失敗分類，改走一般退避。
  - Judge 首審：中 1 項，回傳給呼叫端的最後一個 response 沒被 close 的測試守著，已補；低 4 項已修（`Retry-After` 5／6 邊界、第 3 次等待範圍、4 次 IOException 後重新丟出、`>= 500` 改 `500..599`）；抖動沒注入所以「完全沒抖動」抓不到，人決定不做。複審通過。
  - 已知代價：Retrofit 取消不會中斷執行緒，正在 sleep 的 worker 最長約 6 秒才停。
  - spec 同步（人同意）：取消檢查改成「每次嘗試前」、`Sleeper` 標 `internal`、`Retry-After` 缺少／非整數／負數走一般退避。
- RD-app 詳細頁 UI 階段：20:26–20:33（Do 2 分鐘、Judge 5 分鐘，其中等 Gradle 排隊時先做讀碼審查）（`3dc1e90`）。
  - 人決定：Navigation 與 MainActivity 接線延到 Hilt 整合（NavHost 要向 Hilt 取 VM、用 assisted injection 傳 route id，`:data` binding 進來前無法編譯）。
  - AI 產出：無狀態 `DetailScreen`（6 個 Preview）、`ui/navigation/Routes.kt`（只定義）、灰色佔位 painter 抽成 `component/ImagePlaceholder.kt` 共用、返回鍵用自己畫的 vector（不加 material-icons 依賴）、strings 3 個。
  - Judge：通過；spec 狀態表 10 列逐列對上。3 個 mutation（`imageUrl == null` 仍可點、`species == null` 當成沒有前一階段、空描述仍顯示）都能編譯，只靠 Preview 與人工檢查擋，沒有 UI 測試；低嚴重度 3 項交給人。
- RD-data sync 階段：20:27–21:04（Do 5 分鐘、首審約 10 分鐘、修正與複審約 8 分鐘，中間照 PO 排隊讓出 Gradle 與模擬器）（`2cb099a`）。
  - AI 產出：`PokeApi`、`PokeApiUrls`、`ConnectivityChecker` 與 `PermanentFailureReporter`（只有介面，實作在 Hilt 階段）、`RetryableHttp`（retry 與 sync 共用的重試碼判斷）、`DefaultSyncRepository`（CAS 迴圈切到 `Running`，round 從前一個 `Finished` 取；5 個 worker 從 Channel 取工作，detail 先、species 後）。`:data` 測試 45 個全過（Mappers 14、Retry 13、Sync 18）。
  - 人決定：DB 寫入失敗不取消整輪、結果 `PartialFailure`（人問「取消其他 worker 損失太大」後改的）；reporter 失敗只在呼叫點 catch、不影響結果；不加整輪 `withTimeout`，Hilt 階段改設 OkHttp `callTimeout`（是否涵蓋 interceptor 的重試等待 [待確認]，屆時用測試驗）；list 永久性失敗時本輪為 `Success`，列入 backlog。
  - Judge 首審：中 1 項，`pendingIds`／`isOnline`／reporter 丟出未預期例外時 status 永遠卡在 `Running`、之後不再 sync，用暫時測試重現；外層 try/catch 兜底（結果 `PartialFailure`）加 reporter 呼叫點 catch 修好。低 3 項已修（spec 明寫但沒測試守的 4 項、round 欄位改成從狀態取、重試碼判斷抽共用）。複審通過，低 3 項交給人（依賴自己丟 `CancellationException` 或 `Error` 時仍會卡住、reporter 例外只測 detail 路徑、spec 沒列的例外一律當暫時性）。
  - 第二次修正（21:00–21:04）：人決定照 spec「status 不可停在 Running」修複審低 1，launch 改成 try/catch/finally，結束時一律寫回 `Finished`；新增 `pendingIds` 丟 `CancellationException`、丟 `Error` 2 個測試，改回舊寫法時兩個都失敗（停在 `Running`）。測試 14／13／20 全過。複審通過，低 1 項：一處推論註解沒標 [待確認]，交給人。
  - 意外：doer 第一次建目錄時 `cd` 失敗，把測試檔寫進主資料夾，自己搬回 worktree；PO 用 `git status` 確認主資料夾沒有殘留。
- RD-app 外觀對齊 mock 階段：20:46–20:55（Do 4 分鐘、Judge 3 分鐘，含模擬器截圖對照 mock）（`8347d3e`）。
  - 起因：詳細頁 UI 的 Judge 只照 spec 狀態表審，沒有對照 mock 版面，漏掉置中與小圖位置；RD-app 比對 mock 截圖時才發現，併進這一階段。mock 第 3 頁用 macOS PDFKit（swift 腳本）轉成圖片，不需要 poppler。
  - AI 產出：`PokedexTheme`（主色深紫、不用 dynamic color、不做深色模式；狀態列畫紫色 scrim 並 `consumeWindowInsets`）、MainActivity 加 `enableEdgeToEdge`、卡片去灰底、分隔線改紫色、詳細頁大圖 160dp 置中、名稱與 chips 置中、Evolves from 改成左邊文字右邊小圖。標題與名稱間 4dp 是 doer 自選，標 [待確認]。
  - Judge：通過；對照 mock 9 項全符合；MainActivity 實機可啟動。3 個 mutation（誤用 dynamic color、可點條件永遠成立、狀態列圖示改深色）都能編譯，沒有自動測試擋，其中「可點條件永遠成立」連截圖都看不出來。低 3 項交給人。
  - 人決定：低 3 項都不修（placeholder 沒鋪滿接 NavHost 後會被蓋掉、scrim 寫進 NOTES、4dp 接受）；「沒有圖就不能點」抽成純函式補 JVM 測試，由 RD-app 下一個小階段做。
- RD-app「Evolves from 可點判斷」小階段：21:03–21:09（Do 1 分鐘、Judge 4 分鐘）（`cc276a3`）。
  - 起因：詳細頁 UI 與外觀兩輪 Judge 都指出「可點條件永遠成立」只有人工能擋，連截圖都看不出來；人決定補測試。
  - AI 產出：`evolvesFromClickTarget(PokemonCard): PokemonId?`（internal 純函式），`EvolvesFromSection` 改用它決定能不能點；`EvolvesFromClickTargetTest` 2 個。
  - Judge：通過；`:app` 單元測試 9 個全過；3 個 mutation（條件永遠成立、永遠回 null、回錯 id）都被抓到。低 1 項：Composable 怎麼接這個函式仍沒有自動測試，要 Compose UI test，當 NOTES 素材。
- 意外：RD-data 的 commit 指令被貼到主資料夾，產生訊息是 data、內容是 domain 的錯誤 commit；RD-app 發現後通知 PO，在 main 上 rebase 移除，未推上遠端。CLAUDE.md 新增規則：commit 指令第一行必須 `cd` 到自己的 worktree（`9ba20d5`）。

**決定**
- [0001](design/0001-module-split-and-layering.md) Module 切法與分層
- [0002](design/0002-show-data-as-it-arrives.md) 資料一抓到就顯示，不內建 seed
- [0003](design/0003-sync-fill-missing-and-resume.md) Sync 只補缺漏，中斷後跳過已完成的部分續抓
- [0004](design/0004-retry-in-application-interceptor.md) 重試放在共用的 application interceptor
- [0005](design/0005-table-split-and-write-semantics.md) 拆成五張表，寫入一律用 upsert
- [0006](design/0006-failure-classification-and-user-messages.md) 失敗分類，提示訊息存在 UI state
- [0007](design/0007-tests-without-extra-libraries.md) 測試用 fake，不引入 MockWebServer 和 Robolectric
- tech spec 改放 repo、以 repo 版為正本：subagent 與 worktree 直接讀檔，契約改動走 commit 有 diff 可查。
- 依賴清單（version catalog）只由 PO 改，RD 要加依賴先回報。
- [0006](design/0006-failure-classification-and-user-messages.md) 修訂：sync 結果從事件改成狀態，`:domain` 凍結前完成。

**AI 協作紀錄**：AI 提出、但被我推翻或修正的方案（NOTES 第 1 題素材）
- AI 建議預先建好 18 個 type 列，避免畫面跳動。我指出要先抓到 detail 才知道有哪些 type，後來確認第一代實際只有 17 種，這個做法根本做不到（ADR 0002）。
- AI 建議重試只做在 sync 的 coroutine 裡。我指出所有請求都需要重試，包括圖片，所以改成 interceptor（ADR 0004）。
- AI 原本的設計是中斷後整輪重來、靠 HTTP cache 省成本。面談要求從中斷點續抓，第一次啟動時又沒有 cache，所以改成跳過已完成的部分（ADR 0003）。
- AI 建議時間欄位用 `Instant`。我考量 Room 不支援這個型別、還要多寫轉換，改用 `Long`。
- AI 建議用 DAO 參數或 SQL 條件限制在 151 隻以內。我指出 SQL 測試麻煩，討論後改成在 mapper 加明確規則（ADR 0005）。
- AI 建議引入 Robolectric 和 MockWebServer。我要求評估引入套件的成本，最後兩個都不引入（ADR 0007）。
- AI 建議拿掉 `PartialFailure`。我決定保留（ADR 0006）。
- AI 建議讓 interceptor 也注入網路狀態檢查。我指出耦合太高，最後只在 sync 裡檢查網路（ADR 0004）。
- AI 的專案骨架產生器（template）輸出 hexagonal 架構：feature module、`AppResult` 包裝、Turbine 與 MockK。評估後不套用，只取版本組合與 KSP #2729 的繞法；理由是只有 2 個畫面、資料以本機 DB 為準（讀取不會有網路錯誤）、測試不引入額外套件（ADR 0001、0007）。
- Judge 抓到 sync 結果會在 VM 建立前遺失後，AI 先提 `replay = 1`。我認為它和「改觸發時機」都只是 workaround，要求邏輯上完整的解法（ADR 0006）。
- AI 接著提議把「訊息顯示過了沒」存在 repository。我指出這是把 presentation 的事放進 domain；最後改成 repository 只公開 sync 狀態（含 round），顯示與否由 VM 判斷（ADR 0006）。

**未決・卡住**
- 列表捲到中段時，如果在 index 0 插入新列，key 錨定的實際效果還沒驗證。
- `flavor_text` 裡 `\f` 的處理方式還沒對照 mock 確認（mapper 目前換成空白，標待確認）。
- 要寫進 NOTES：系統字級 1.5 倍時，96dp 卡片上較長的名字會被截斷（人決定不修）。
- 要寫進 NOTES：`PokedexTheme` 同時負責狀態列 scrim 版面，之後有沉浸式畫面要先拆開；詳細頁顯示規則與外觀沒有 UI 測試，靠 Preview、截圖與人工檢查。
- 首頁 UI 延後的低嚴重度項目，以及卡片底色、狀態列圖示顏色、紫色分隔線，併到 theme 階段；寶貝球 32dp 觸控範圍在 Hilt 整合後手動驗證。
- 待人決定：RD-data 打算加 R1–R4 instrumented 測試（抓 repository 讀取 SQL 的 JOIN 寫錯），spec 測試清單沒有；要不要列進 spec。
- Hilt 綁定歸屬（RD-data 提出，需同步給 RD-app）：`TimeSource`、`@ApplicationScope` 由 `:data` 提供，`:app` 不可再綁，否則 duplicate binding；`OkHttpClient` 由 `:data` 以 `@Singleton` 提供給 Coil 共用（spec「共用一個 `OkHttpClient`」），Coil 設定在 `:app`。

**下一步**
- 2026-11-26 晚上暫停，隔天繼續。
- RD-data：repository＋`:data` Hilt binding，提議拆兩段，等人放行：
  - R：兩個 repository 加讀取用的 DAO（要跑模擬器）。
  - H：`:data` 的 Hilt module、網路、真的 reporter、logger、註解清理。
- RD-app：已 rebase 到 `0f5e1bf`，等 `:data` Hilt binding 進 main 後做 Hilt＋Navigation 整合與手動驗證。Navigation 延後是因為 NavHost 要向 Hilt 取 VM、用 assisted injection 傳 route id，binding 沒進來前無法編譯（人決定）。
- 檢查點二（第 3 天結束）：★ 10 個全過（S1、S2 在 `2cb099a`）。
- 合併後的 main（`0f5e1bf`）：`assembleDebug` 與三個 module 的 JVM 測試 61 個全過（domain 5、data 47、app 9；29s，`--rerun-tasks`）。
- 估時：面談時講約 18.5h，範圍調整後約 17h。實際分配見本段開頭的「時間分配」。
