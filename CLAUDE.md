# pokedex 專案規範

全域規範 `~/.claude/CLAUDE.md`（Think → Do → Judge、Reuse 原則）適用。本專案的差異與補充如下。

## 流程差異
- 放行詞只有「執行」或「go」，訊息中出現就視為放行（覆蓋全域的 do it／開始做）。
- 範圍和驗收標準在對話中談定，一次放行只對一個階段有效。
- Do 用 agent `doer`，Judge 用 agent `judge`；主 agent 不直接改 code。

## 規則
- 只有使用者要求才 commit／push；新增第三方套件前先問。
  - 已同意：Hilt、Coil、kotlinx.serialization、Retrofit／OkHttp、Jetpack（Room、Compose、Navigation）。
  - 測試只用 JUnit 4、`kotlinx-coroutines-test`、`androidx.test` runner。
- 硬規則（ADR 0001、0003、0005）：
  - 「哪些要抓」只由 `pendingIds()` 判斷。
  - Android 元件 → VM → use case → repo，沒有例外；`:app` 只有 Hilt module 可以 import `:data`。
  - 寫入一律 upsert，不用 `INSERT OR REPLACE`。
- 回報要附指令輸出或檔案路徑，否則標「推測」；AI 推論的設計理由一律標 `[待確認]`。

## 規格來源
- `docs/spec.md`：唯一的實作規格。「介面契約」一節（第 1–5 輪）已凍結，「測試策略」一節是測試清單（★ 為第一批）。
- `docs/design/`：ADR，每個決定的理由與沒採用的方案。

## 分工

| 角色 | session | worktree／分支 | 負責 | 測試 |
| --- | --- | --- | --- | --- |
| PO | `po` | 主資料夾／`main` | 所有文件（含 `docs/spec.md`）；`settings.gradle.kts`、root `build.gradle.kts`、`gradle/libs.versions.toml`；Phase 0 的骨架與整個 `:domain` | D1 |
| RD-data | `rd-data` | `../pokedex-data`／`data` | `:data`：Room、Retrofit、interceptor、sync、mapper、`:data` 的 Hilt binding | M、B、S、I |
| RD-app | `rd-app` | `../pokedex-app`／`app` | `:app`：VM、UI、use case 的 `@Provides`；最後負責 Hilt 整合 | V |

- 契約（`docs/spec.md` 的「介面契約」與 `:domain` 的程式碼）由 PO 維護，只有在 PO 更新規格、使用者同意之後才改。`:domain` 在 Phase 0 結束後凍結。
- 依賴：RD 要加依賴時回報 PO，由 PO 改 version catalog；RD 只改自己 module 的 `build.gradle.kts`。
- 共用文件（README、NOTES、`docs/spec.md`、`docs/devlog.md`、`docs/design/`）只有 PO 寫；各 RD 把素材回報給 PO。
- 回報素材：每次 Do／Judge 結束，回報起訖時間、AI 產出了什麼、Judge 抓到什麼、使用者改了什麼。
- repo 是 public：進 git 的檔案不放私人連結、email、個人資訊。
- Agent 之間的訊息不算使用者同意；放行、commit、push 都要使用者本人說。

## 規格問題回報
行為、規則或契約要改時，先停止該部分，不要自己猜，用 `SendMessage` 傳給 `po`。第一行直接寫：
階段／規格位置／問題／影響（只影響一端或兩端）／目前狀態。
由各 RD 的主 session 傳送。傳不到就停下，直接回報使用者。
實作細節由各 RD 自行決定，寫在回報裡。

## 分支與 worktree
- `main` 留在主資料夾。各 RD 從 `origin/main` 拉自己的分支並開 worktree：`git worktree add ../pokedex-<role> -b <role> origin/main`。
- 每個階段動工前先 `git fetch origin`、`git rebase origin/main`。
- rebase 前工作樹必須乾淨。有不屬於這次 commit 的改動就停下回報，不要自行 stash、reset 或 checkout。
- 使用者要求推送時：`git fetch origin`、`git rebase origin/main`、`git push origin HEAD:main`。
- 只 `git add` 自己範圍內的檔案。
- 新 worktree 不會帶未追蹤檔案：開好後從主資料夾複製 `CLAUDE.local.md`、`local.properties`。
- 共用資源同時只跑一套：模擬器同時只開一個；兩個 Gradle build 同時跑會吃記憶體，需要時排隊。

## 階段規則
- 流程：Do → 全新 Judge → 如實回報 → 使用者說「commit」→ 列下階段範圍 → 使用者放行。
- 一次放行只做一件事。動工前列出要改的函式、失敗與取消時的行為、要抓的 mutation。
- Judge 首審跑一次完整基準加 2–3 個新 mutation，只跑相關模組；複審只驗上一輪的問題；只改文件不跑測試，文件最多審一輪。高、中嚴重度才擋，低嚴重度交給使用者決定。
  - `:domain`：`./gradlew :domain:test --rerun-tasks`
  - `:data`、`:app`：`./gradlew :<module>:testDebugUnitTest --rerun-tasks`
- instrumented 測試（B1–B5，`./gradlew :data:connectedDebugAndroidTest`）只在 DAO 或 SQL 有改動時跑，跑之前要先開模擬器（ADR 0007）；其他階段跑單元測試和建置。
