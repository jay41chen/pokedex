# Devlog

## 2026-11-26（一面、範圍議定、契約凍結）

**時間**：當天沒有記錄起訖時間。從下一次 Do/Judge 開始記錄。

**做了什麼**
- 第一次技術面談，議定交付範圍，結論寫在 ADR 0002、0003。
- 和 AI 逐輪討論 module 之間的契約，共五輪，全部凍結，寫在內部的 tech spec。
- 建立 repo，加入 `.gitignore`、`NOTES.md` 骨架、ADR 0001–0007（待 commit）。
- ADR 0001、0003–0006 補 mermaid 圖：分層、sync 時序、重試判斷、ER 圖（取代 ASCII）、訊息時序（待 commit）。

**決定**
- [0001](design/0001-module-split-and-layering.md) Module 切法與分層
- [0002](design/0002-show-data-as-it-arrives.md) 資料一抓到就顯示，不內建 seed
- [0003](design/0003-sync-fill-missing-and-resume.md) Sync 只補缺漏，中斷後跳過已完成的部分續抓
- [0004](design/0004-retry-in-application-interceptor.md) 重試放在共用的 application interceptor
- [0005](design/0005-table-split-and-write-semantics.md) 拆成五張表，寫入一律用 upsert
- [0006](design/0006-failure-classification-and-user-messages.md) 失敗分類，提示訊息存在 UI state
- [0007](design/0007-tests-without-extra-libraries.md) 測試用 fake，不引入 MockWebServer 和 Robolectric

**AI 協作紀錄**：AI 提出、但被我推翻或修正的方案（NOTES 第 1 題素材）
- AI 建議預先建好 18 個 type 列，避免畫面跳動。我指出要先抓到 detail 才知道有哪些 type，後來確認第一代實際只有 17 種，這個做法根本做不到（ADR 0002）。
- AI 建議重試只做在 sync 的 coroutine 裡。我指出所有請求都需要重試，包括圖片，所以改成 interceptor（ADR 0004）。
- AI 原本的設計是中斷後整輪重來、靠 HTTP cache 省成本。面談要求從中斷點續抓，第一次啟動時又沒有 cache，所以改成跳過已完成的部分（ADR 0003）。
- AI 建議時間欄位用 `Instant`。我考量 Room 不支援這個型別、還要多寫轉換，改用 `Long`。
- AI 建議用 DAO 參數或 SQL 條件限制在 151 隻以內。我指出 SQL 測試麻煩，討論後改成在 mapper 加明確規則（ADR 0005）。
- AI 建議引入 Robolectric 和 MockWebServer。我要求評估引入套件的成本，最後兩個都不引入（ADR 0007）。
- AI 建議拿掉 `PartialFailure`。我決定保留（ADR 0006）。
- AI 建議讓 interceptor 也注入網路狀態檢查。我指出耦合太高，最後只在 sync 裡檢查網路（ADR 0004）。

**未決・卡住**
- 列表捲到中段時，如果在 index 0 插入新列，key 錨定的實際效果還沒驗證。
- `flavor_text` 裡 `\f` 的處理方式還沒對照 mock 確認。

**下一步**
- 先 commit，再開始 Do：建立專案骨架（`:app` / `:domain` / `:data`、Hilt、Room、Retrofit、Compose）。
- 估時：面談時講約 18.5h，範圍調整後約 17h。
