# Devlog

## 2026-11-26（一面、範圍議定、契約凍結）

**時間**：面談與契約討論沒有記錄起訖時間。Phase 0a：18:04–18:09（Do 約 3 分鐘、Judge 約 1.5 分鐘）。Phase 0b：18:12–18:36（含契約修訂與兩輪 Judge）。

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

**下一步**
- RD-data：repository＋`:data` Hilt binding（RD-app 的整合在等這個）。
- RD-app：等 `:data` Hilt binding 進 main 後做 Hilt＋Navigation 整合與手動驗證。Navigation 延後是因為 NavHost 要向 Hilt 取 VM、用 assisted injection 傳 route id，binding 沒進來前無法編譯（人決定）。
- 檢查點二（第 3 天結束）：★ 10 個全過（S1、S2 在 `2cb099a`）。
- 合併後的 main（`69b3e97`）：`assembleDebug` 與三個 module 的 JVM 測試 26 個全過（17s，`--rerun-tasks`）。
- 估時：面談時講約 18.5h，範圍調整後約 17h。
