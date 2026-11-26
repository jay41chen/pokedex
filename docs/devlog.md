# Devlog

## 2026-11-26（一面、範圍議定、契約凍結）

**時間**：面談與契約討論沒有記錄起訖時間。Phase 0a：18:04–18:09（Do 約 3 分鐘、Judge 約 1.5 分鐘）。Phase 0b：18:12–18:36（含契約修訂與兩輪 Judge）。

**做了什麼**
- 第一次技術面談，議定交付範圍，結論寫在 ADR 0002、0003。
- 和 AI 逐輪討論 module 之間的契約，共五輪，全部凍結，寫在內部的 tech spec。
- 建立 repo，加入 `.gitignore`、`NOTES.md` 骨架、ADR 0001–0007；ADR 0001、0003–0006 補 mermaid 圖（docs: add ADRs 0001-0007 with diagrams, devlog, and NOTES skeleton `1af8b72`）。
- tech spec 搬進 repo 成為 `docs/spec.md`；新增專案 `CLAUDE.md`（PO＋RD-data＋RD-app 分工）與 `.claude/agents/doer.md`、`judge.md`（docs: move tech spec into repo, add CLAUDE.md and doer/judge agents `59ba297`）。
- Phase 0a：三 module Gradle 骨架（`:app` / `:domain` / `:data`），version catalog 放齊已同意套件（待 commit）。
  - AI 產出：build 設定、wrapper、`PokedexApp` 與空的 `MainActivity`；Room 2.8.5、Coil 3.6.3 由 AI 查最新穩定版。
  - Judge：通過，3 個低嚴重度：`:domain` 的 coroutines 應用 `api`（併入 0b）、多了未回報的 foojay settings plugin、只有佔位 theme（交給 RD-app）。
  - 編譯時間：首次 `assembleDebug` 1m12s（含下載依賴），有快取 0.9s，`--rerun-tasks` 13s。
- Phase 0b：`:domain` 全部（model、三個 repository 介面、7 個 use case、`groupByType`）與 D1 測試 5 個（待 commit）。
  - AI 產出：照 spec 第 1–3 輪逐字實作；mutation 自測 3 個皆被抓到。
  - Judge 首審：通過，但指出 spec 契約本身的風險：`results` 是 `SharedFlow(replay = 0)`，冷啟動離線時結果在 HomeViewModel 建立前就發出而遺失，空畫面卡在 `Loading`。
  - 契約修訂：sync 結果改成狀態 `status: StateFlow<SyncStatus>`（Idle／Running／Finished(round, result)），「顯示過了沒」留在 VM（ADR 0006 修訂）。
  - Judge 複審：需修正（只有文件），補上「HomeViewModel 晚於 onStart 建立」的前提與手動驗證、旋轉時不退回 `Loading`、`lastShownRound` 的推進規則、check-and-set 要原子。
- Phase 0 完成（`224e580`），開 worktree：`../pokedex-data`（`data`）、`../pokedex-app`（`app`），session 名稱 `po`／`rd-data`／`rd-app`。
- RD-data mapper 階段：18:43–18:48（Do 3 分鐘、Judge 2 分鐘），待 commit。
  - AI 產出：3 個 DTO、5 個 Room entity（尚無 DAO）、雙向 mapper、`MappersTest` 12 個（含 M1–M4，用 PokéAPI 真實 fixture）。
  - Judge：通過；3 個新 mutation 有 2 個漏網（151 邊界 `<=` 改 `<`、對照表 32／122 未測、`idFromUrl` 測試分不出取法），低嚴重度。
  - 修正輪 18:59–19:01：人決定 4 項全補；補 32／122、151／152 邊界、`/api/2/` URL 三組斷言，測試 14 個全過，複審通過。RD 自己在交給 doer 前發現原本的 `/api/v2/` 例子測不出取錯段而改正。
- RD-app HomeViewModel 階段：18:44–18:48（Do 2 分鐘、Judge 1.5 分鐘），待 commit。
  - AI 產出：`HomeContract`（照 spec 第 3 輪）、`HomeViewModel`（單一 private 狀態 + `update {}`）、手寫 fake repository、V1–V4。
  - Judge：通過；漏網 mutation「Success 不清掉舊 message」，低嚴重度 3 項（該行為無測試、fake 有死碼、MainDispatcherRule 隱含依賴 Unconfined）。
  - 人決定：`@HiltViewModel` 延到 Hilt 整合階段；`Finished(Success)` 清掉尚未顯示的舊 message（spec 未定義，PO 已補進 spec 第 3 輪與 ADR 0006，新增 V5）。
  - 修正輪 18:58–19:01：補 V5（Success 清掉未顯示的舊 message）、刪 fake 死碼、MainDispatcherRule 加 KDoc；測試 5 個全過，複審通過，剩一個未用 import。

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
- `flavor_text` 裡 `\f` 的處理方式還沒對照 mock 確認。

**下一步**
- commit 本次的 spec、CLAUDE.md、agents。
- Phase 0（PO 用 doer/judge）：骨架 `:app` / `:domain` / `:data`、version catalog、整個 `:domain` 與 D1。先評估 `android-project-template` skill 能不能用。
- Phase 0 commit 後開兩個 worktree，啟動 RD-data、RD-app。
- 估時：面談時講約 18.5h，範圍調整後約 17h。
