# Pokémon 圖鑑技術規格

> Internal working spec (Chinese). The frozen contracts live here; design rationale is in the English ADRs under `docs/design/`.

本文件是 Pokémon 圖鑑作業的實作交接規格：範圍已於 2026-11-26 會議議定，模組契約同日凍結，預估約 17h。來源：規格 PDF Rev 2.4、會前 email 假設、會議結論、本文件的契約討論。

## 範圍與時程

| 做 | 不做 |
| --- | --- |
| 首頁：固定在頂端的 My Pocket、依 type 分組的列表 | 內建資料（seed） |
| 詳細頁 | 每日更新（B，backlog） |
| 捕捉／釋放 | 網路恢復的當下自動補抓（backlog） |
| sync：補缺、中斷後續抓、回前景觸發 | 手動重試按鈕 |
| 重試 interceptor、離線與部分失敗提示 | 151 以外的寶可夢 |
| contract test 與 NOTES.md | 捕捉機率 |
| Bonus：Evolves from 可點 |  |

| 項目 | 估計 |
| --- | --- |
| 必做 | 約 15h |
| 續抓 | 約 1h |
| 提示與空狀態 | 約 0.8h |
| **合計** | **約 17h**（預算 25h = 5 天 × 5h） |

- 檢查點一：契約凍結，2026-11-26 已完成。
- 檢查點二：第 3 天結束時，★ 測試全部通過。
- 砍單順序：排版與動畫 → Bonus。contract test 與 NOTES 不砍。
- 每次 Do／Judge 後寫一則 devlog：起訖時間、AI 產出什麼、Judge 抓到什麼、人改了什麼。NOTES 第 1、3、6 題靠這份紀錄。

## 需求與決議

規格需求逐條對應到決議與驗收方式；「會議」表示 2026-11-26 面試方的答覆。

| 規格需求 | 決議 | 驗收 |
| --- | --- | --- |
| 頂端橫向列出已捕捉，新的在前，可重複，可釋放 | 會議：My Pocket 固定在頂端；在 Pocket 內點寶貝球 = 釋放 | B4、B5 |
| 依 type 分組、type 依字母、組內依 id，可重複出現 | 規格已明寫 | D1 |
| 捕捉按鈕 | 會議：寶貝球是按鈕，在圖鑑內點 = 捕捉；一定成功 | 手動 |
| 資料一到就顯示 | 會議：不做 seed，輕微跳動可接受 → key 錨定加 `animateItem` | 手動（待實測） |
| 本地 DB 持久化，可更新捕捉狀態 | Room 五張表 | B3 |
| 減少重複請求，意外中斷後可繼續 | 會議：第 12 隻中斷要能從 12 續抓 → 補缺語意，跳過已完成 | B1、S1、手動 kill |
| 盡快抓資料 | 回前景觸發；並發 5，detail 先排 | S5 |
| 點寶可夢進詳細頁 | 顯示 #id、圖、名稱、type、Evolves from、描述（依 mock） | 手動 |
| Evolves from | 會議：151 以外不顯示，視同沒有前一階段 | M1 |
| 只用三支 API | 不打 `/type`，不寫死 type 清單 | — |
| 善待 PokéAPI | 並發 5、最多 4 次嘗試含退避、`Retry-After` 上限 5 秒 | I1–I4 |
| 響應式排版、動畫、大資料量效能 | 做；時間不夠時第一個砍，NOTES 交代 | 手動 |
| Bonus：Evolves from 可點 | 前一階段還沒有 detail 時不可點 | 手動 |

email Q3（資料是否靜態）會議未明確答覆；採靜態假設（補缺語意），並在 NOTES 註明。

## 架構

三個 module、單向依賴，資料只從 Room 流向畫面（offline first，Room 是唯一資料來源）。依賴圖與介面見「介面契約」。

| Module | 類型 | 內容 |
| --- | --- | --- |
| `:app` | Android app | Compose UI、ViewModel、Navigation、Hilt module（含 use case 的 `@Provides`） |
| `:domain` | kotlin-jvm | model、repository 介面、use case、`PokemonScope`；只依賴 Kotlin 官方 library |
| `:data` | Android library | Room、Retrofit／OkHttp、DTO、entity、mapper、repository 實作、sync、interceptor |

- 分層：Android 元件 → VM → use case → repo，沒有例外；讀取類 use case 是轉呼叫，接受這個代價換規則單一。
- 命名：`XxxDto`（`data.remote.dto`）、`XxxEntity`（`data.local.entity`）、`Xxx`（`domain.model`，不加後綴）。
- VM 為輕量 MVI：單一 `StateFlow` 加 sealed intent。
- 套件：Hilt、Coil、kotlinx.serialization、Retrofit／OkHttp（皆已同意）；Room、Compose、Navigation 為 Jetpack。測試只加 JUnit 4、`kotlinx-coroutines-test`、`androidx.test` runner。
- 時間與等待皆可注入：`TimeSource`、`Sleeper`；網路狀態經 `ConnectivityChecker`（只有 sync 用）。

## 資料模型

五張表，寫入一律 upsert、不用 REPLACE；欄位、transaction 邊界與 mapper 見「介面契約」第 4a 輪。

| 表 | 代表 | 關係 |
| --- | --- | --- |
| ① pokemon | list 已抓到的 151 隻 | 父表 |
| ② pokemon\_detail | 有這筆 = detail 已抓完 | 1:1 → ① |
| ③ pokemon\_type | 寶可夢與 type 多對多 | N:1 → ②；type 只是字串，不另開表 |
| ④ pokemon\_species | 有這筆 = species 已抓完 | 1:1 → ①；`evolves_from_id` 不設 FK |
| ⑤ capture | 一次捕捉，同一隻可多筆 | N:1 → ①，RESTRICT |

| 決定 | 替代方案會壞在哪 |
| --- | --- |
| upsert 而非 `REPLACE` | REPLACE 是先刪後插：FK 若是 CASCADE 會連帶刪掉捕捉紀錄；若是 RESTRICT 寫入會失敗（B3 守住） |
| capture 用 UUID 主鍵 | 用 pokemon id：同一隻無法捕捉兩次；LazyRow 用 pokemon id 當 key 會重複而 crash |
| capture → ① 用 RESTRICT | CASCADE：寶可夢資料被刪時使用者的捕捉會靜默消失 |
| ① 與 ② 分開 | 合併：list 的 upsert 會把已抓好的 `image_url` 蓋成 null，必須改寫只更新部分欄位；`image_url == null` 分不出「還沒抓」與「API 本來就是 null」，要多一個 fetched 旗標。分開時每支 API 各自擁有一張表，「② 有這一筆」就代表抓完 |
| ② 與 ④ 分開 | 合併：`evolves_from_id = null` 是常見的正常值，分不出「還沒抓」與「沒有前一階段」；兩個 worker 併發寫同一列時 `@Upsert` 會互相蓋成 null，只在特定時序出現；先有 species 時「有這列 = detail 抓完」不再成立。PokéAPI 裡 species 與 pokemon 是一對多，分開也與 API 模型一致 |
| 排序 `captured_at DESC, rowid DESC` | 只用 `captured_at`：同毫秒兩次捕捉順序不確定（B4 守住） |

## 網路

只用規格指定的三支 API，一輪完整同步共 303 個請求（1 + 151 + 151），並發 5 時實測外推約 40 秒。

| API | 取用欄位 | 寫入 |
| --- | --- | --- |
| `GET /pokemon?limit=151` | `results[].name`、`results[].url`（取 id） | ① |
| `GET /pokemon/{id}` | `sprites.other.official-artwork.front_default`、`types[].slot`、`types[].type.name` | ② ③ |
| `GET /pokemon-species/{id}` | `evolves_from_species.url`（取 id）、`flavor_text_entries[]`（第一筆英文） | ④ |

- 共用一個 `OkHttpClient`，Retrofit 與 Coil 都用它，所以圖片請求也有重試。重試規則見第 4b 輪。
- JSON 用 kotlinx.serialization，DTO 只宣告用到的欄位，`ignoreUnknownKeys = true`。
- PokéAPI 為靜態託管、回傳 `cache-control: max-age=86400` 與 ETag；OkHttp Cache 保留，永久性失敗時移除該 URL。

## Sync

每次 app 回到前景就補抓缺的資料；全部抓完後一輪是 0 個請求。流程、失敗分類與 interceptor 見第 4b 輪。

1. `MainActivity.onStart` → `MainViewModel.onAppForegrounded()` → `RequestSync` → `SyncRepository.requestSync()`。
2. 一輪在 application scope 執行，同時只有一輪；「哪些要抓」只由 `pendingIds()` 決定。
3. 狀態經 `SyncRepository.status` → `ObserveSyncStatus` → `HomeViewModel`，推導出 `catalog` 狀態與 `message`。

之後加 B（每日更新）只改 `pendingIds()` 的選取條件、加 `fetched_at` 與冷卻，預估 B 本身約 1h，先 A 後 B 只多約 0.3h。

## 畫面

兩個畫面，版面依 mock；狀態型別見第 3 輪。

**首頁**：`Column { PocketRow; LazyColumn }`

| 區塊 | 規則 |
| --- | --- |
| My Pocket | 在 LazyColumn 外、固定高度、不捲走；標題右側顯示數量；空的時候顯示「還沒有捕捉的寶可夢」；LazyRow key = `captureId` |
| type 列 | LazyColumn key = type 名稱；每列標題右側顯示數量；LazyRow key = pokemon id；新項目用 `animateItem` |
| 卡片 | 圖、名稱、右上角寶貝球；圖鑑內點球 = 捕捉，Pocket 內點球 = 釋放；點卡片本體 = 進詳細頁 |
| `catalog` | `Loading`：載入中；`Empty`：空狀態文字；`Ready`：列表 |
| `message` | Snackbar，顯示後送 `MessageShown` |

**詳細頁**：返回鍵、右上 `#id`、大圖、名稱、type chips、Evolves from（名稱加小圖）、描述。

| 資料狀態 | 顯示 |
| --- | --- |
| `species == null` | Evolves from 與描述顯示佔位 |
| `species.evolvesFrom == null` | 隱藏 Evolves from 區塊 |
| `evolvesFrom.imageUrl == null` | 佔位小圖加名稱，不可點 |
| `description == ""` | 隱藏描述 |
| `imageUrl == null` | 佔位圖 |

顯示格式在 UI 層處理：type 名稱在列標題首字母大寫（Bug），chips 維持小寫（fire），對應 mock。

## 介面契約

契約分五輪討論，每輪確認即凍結。已凍結：第 1–5 輪（第 4 輪分 4a、4b），2026-11-26 全部凍結。測試清單見「測試策略」。

### 第 1 輪：Module 依賴與 domain model（2026-11-26 凍結）

```
:app ──→ :domain ←── :data
  └──────────────────→ :data   （只為 Hilt 綁定實作）
```

- `:domain` 是純 Kotlin，只依賴 coroutines（`Flow`）。
- `:app` 依賴 `:data` 只為了 DI；UI 與 VM 程式碼不得 import `:data` 的類別，靠 code review 把關。

```kotlin
@JvmInline value class PokemonId(val value: Int)
@JvmInline value class TypeName(val value: String)   // 不用 enum：type 清單來自資料，不寫死
@JvmInline value class CaptureId(val value: String)  // UUID

fun interface TimeSource { fun nowEpochMillis(): Long }  // 注入，測試用固定值

// 列表、My Pocket、Evolves from 共用
data class PokemonCard(
    val id: PokemonId,
    val name: String,
    val imageUrl: String?,      // null = 這隻的 detail 還沒抓到
)

data class TypeSection(val type: TypeName, val pokemon: List<PokemonCard>)  // 數量 = pokemon.size

data class CapturedPokemon(
    val captureId: CaptureId,
    val pokemon: PokemonCard,
    val capturedAtEpochMillis: Long,
)

data class PokemonDetail(
    val card: PokemonCard,
    val types: List<TypeName>,
    val species: Species?,      // null = species 還沒抓到
)

data class Species(
    val evolvesFrom: PokemonCard?,  // null = 沒有前一階段，或前一階段 > 151
    val description: String,
)
```

| 決定 | 理由 | 考慮過的替代 |
| --- | --- | --- |
| `Species` 包一層 nullable | 詳細頁要分清「載入中」（佔位）與「確實沒有前一階段」（隱藏區塊） | 攤平成 `evolvesFrom` 欄位：`null` 同時代表兩種狀態 |
| `TypeSection` 只含已有 detail 的寶可夢 | type 只在 detail 回應裡，沒有 detail 就不知道該放哪一列 | 「未分類」列：是假的 type，載完會消失，多一個跳動來源 |
| `PokemonCard` 不帶捕捉狀態 | 寶貝球是按鈕不是標示；同一隻可抓多次，「抓過沒」不是布林值 | 帶 `isCaptured`：首頁兩條 Flow 被迫 combine |
| 捕捉時間用 `Long` epoch millis | Room 直接存，不需 TypeConverter；欄位只用於排序 | `Instant`：型別較安全，但多一層轉換 |

同一毫秒內連續捕捉兩次時，My Pocket 的排序用 `captured_at DESC, rowid DESC` 決定先後，細節見第 4 輪。

### 第 2 輪：Repository 介面（2026-11-26 凍結）

介面定義在 `:domain`，由 `:data` 實作。

```kotlin
interface PokemonRepository {
    fun observeCatalog(): Flow<List<CatalogEntry>>          // 有 detail 的寶可夢，不分組、不排序
    fun observeDetail(id: PokemonId): Flow<PokemonDetail?>  // null = 這隻的 detail 還沒抓到
}
data class CatalogEntry(val card: PokemonCard, val types: List<TypeName>)

interface CaptureRepository {
    fun observeCaptures(): Flow<List<CapturedPokemon>>  // 新的在前：captured_at DESC, rowid DESC
    suspend fun capture(id: PokemonId)                  // 一定成功
    suspend fun release(captureId: CaptureId)           // 冪等：不存在就不做事
}

interface SyncRepository {
    fun requestSync()                   // 非 suspend；回傳前 status 已是 Running；在 application scope 執行；進行中再呼叫會被忽略
    val status: StateFlow<SyncStatus>   // 狀態，不是事件：任何時候開始收集都拿得到目前值
}

sealed interface SyncStatus {
    data object Idle : SyncStatus                                              // 這個 process 還沒跑過任何一輪
    data object Running : SyncStatus
    data class Finished(val round: Long, val result: SyncResult) : SyncStatus  // round 從 1 開始，每輪加 1
}

sealed interface SyncResult {
    data object Success : SyncResult
    data object PartialFailure : SyncResult  // 有暫時性失敗沒補上；缺的下次回前景會自動補
    data object Offline : SyncResult         // 沒網路，或中途斷線而中止
}
```

| 決定 | 理由 | 考慮過的替代 |
| --- | --- | --- |
| 分組與排序放 use case，repo 回平的列表 | 規格明寫的規則放在 domain，是純函式，JVM 上可測 | 在 SQL／mapper 做完：測試要跑 Room，規則散在 SQL |
| `release` 冪等 | 連點兩下不該出錯 | 不存在時丟錯：UI 要多處理一種沒意義的錯誤 |
| `capture` 一定成功 | 會議結論；只有已在 type 列的寶可夢能被捕捉，資料必定存在，違反即 bug（FK RESTRICT 會丟） | 回傳結果型別：為不會發生的情況加分支 |
| `requestSync()` 加 `status: StateFlow` | sync 在 `onStart` 觸發，要撐過旋轉與 VM 清除；repo 保證同時只跑一輪。sync 是一個過程，公開成狀態：VM 不論何時建立都讀得到目前狀態，不靠誰先訂閱；`requestSync()` 回傳前就是 `Running`；HomeViewModel 由第一次組合建立，而 `setContent` 的第一次組合在 `onResume` 之後，晚於 `onStart`，所以新開的畫面讀不到上一個 Activity 的舊結果（前提由 Android 生命週期保證，列入手動驗證；不成立時最壞是多跳一次舊 Snackbar）。`round` 讓連續兩輪相同結果仍是不同的值 | `suspend fun sync()`：綁在呼叫端的 scope，畫面一走就被取消；`results: SharedFlow(replay = 0)`：VM 建立前結束的一輪會掉，冷啟動離線時卡在 `Loading`；`replay = 1`：Activity 重開時重播舊結果；畫面組合後才觸發：正確性靠我們自己的寫法維持（VM 要立即訂閱、首頁要在組合中），重構就可能壞，壞了會卡在 `Loading`，且無法用 JVM 測試驗證；repo 記「訊息已顯示」：把畫面的事放進 domain |
| 保留 `PartialFailure` | 讓使用者知道列表不完整的原因 | 只留兩種結果：缺口只記 log，使用者看到少幾隻卻沒有說明 |

補抓不依賴 `SyncResult`：沒抓到的寶可夢沒有 detail，會留在 `pendingIds()`，下次回前景自動再抓。`SyncResult` 只決定畫面提示。

### 第 3 輪：Use case 與 UI state（2026-11-26 凍結）

Use case 不加 `@Inject`，在 Hilt module 用 `@Provides` 提供；Android 元件只跟 VM 溝通，VM 只經由 use case 存取 repo。

| Use case | 做什麼 |
| --- | --- |
| `ObserveTypeSections` | `observeCatalog()` 加分組排序；規則是純函式 `groupByType(entries)`：type 依字母、組內依 id，雙 type 出現在兩列 |
| `ObserveCaptures` | 轉呼叫 repo |
| `ObservePokemonDetail(id)` | 轉呼叫 repo |
| `CapturePokemon(id)` / `ReleaseCapture(captureId)` | 轉呼叫 repo |
| `RequestSync` / `ObserveSyncStatus` | 轉呼叫 repo |

輕量 MVI：單一 `StateFlow` 輸出、sealed intent 單一入口；不用 MVI 框架，不另拆 reducer。導航不是 intent，UI 直接呼叫導航 lambda。

VM 內部只有一份狀態：`lastShownRound`、最近一次 `Finished` 的 result 這類不需要畫面渲染的值放在 VM 的 private 狀態裡（與 `HomeUiState` 一起，例如包成一個 private data class），不放進 `HomeUiState`，也不另外用散落的 `var`。intent 與資料流（catalog、captures、status）造成的變更，都經同一個 `update { }` 更新這份狀態，對外只暴露 `state: StateFlow<HomeUiState>`。

```kotlin
// 首頁
data class HomeUiState(
    val pocket: List<CapturedPokemon>,
    val sections: List<TypeSection>,
    val catalog: CatalogStatus,
    val message: UserMessage?,            // null = 沒有要顯示的 Snackbar
)
sealed interface CatalogStatus {
    data object Loading : CatalogStatus   // DB 空，且 VM 還沒看過任何 Finished
    data object Ready : CatalogStatus     // 至少一隻
    data class Empty(val offline: Boolean) : CatalogStatus  // DB 空，且 VM 看過 Finished；offline = 最近一次 Finished 的 result == Offline（之後 Running 期間維持，不退回 Loading）
}
enum class UserMessage { Offline, PartialFailure }

sealed interface HomeIntent {
    data class Capture(val id: PokemonId) : HomeIntent
    data class Release(val captureId: CaptureId) : HomeIntent
    data object MessageShown : HomeIntent  // UI 顯示完 Snackbar 後回報，VM 把 message 清成 null
}
class HomeViewModel : ViewModel() {
    val state: StateFlow<HomeUiState>
    fun onIntent(intent: HomeIntent)
}

// 詳細頁：沒有 intent
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val detail: PokemonDetail) : DetailUiState
}

// Activity 範圍：MainActivity.onStart 呼叫
@HiltViewModel
class MainViewModel @Inject constructor(private val requestSync: RequestSync) : ViewModel() {
    fun onAppForegrounded() = requestSync()
}
```

| 畫面狀態 | 顯示 |
| --- | --- |
| `Empty(offline = true)` | 「目前離線，連上網路後會自動載入」 |
| `Empty(offline = false)` | 「暫時無法載入資料，稍後會自動重試」 |
| `message = Offline` | Snackbar「目前離線，顯示先前的資料」 |
| `message = PartialFailure` | Snackbar「部分資料未載入，稍後會自動重試」 |

| 決定 | 理由 | 考慮過的替代 |
| --- | --- | --- |
| 三條資料流在 VM 合併 | `Loading`／`Empty` 是畫面概念，不屬於 domain；用 VM 單元測試守 | `ObserveHome` use case：只是把 combine 搬進 domain |
| Snackbar 訊息放進 state，UI 顯示後回報 | 旋轉、背景都不會掉或重複；測試只需斷言 state，不需 Turbine。VM 記 `lastShownRound`：收到 round 比它大的 `Finished` 時立即推進到該 round（`Success` 也推進），不是 `Success` 才產生 message；較新一輪的結果取代尚未顯示的舊 message，`Success` 則清成 `null`；「顯示過了沒」是畫面的事，留在 VM。旋轉會觸發新的一輪，離線時 Snackbar 會再出現一次（那一輪確實又是離線），接受 | `SharedFlow(replay=0)` 會掉；`replay=1` 會重播；`Channel` 取出後被取消會掉 |
| Evolves from 前一階段未有 detail（`imageUrl == null`）時不可點 | 詳細頁只會收到已有 detail 的 id，`Loading` 不會卡住 | 可點＋詳細頁處理「還沒資料」：多一個狀態和等待規則 |
| sync 由 Activity 範圍的 `MainViewModel` 觸發 | 分層規則沒有例外；前景事件不屬於任何畫面 | Activity 直接注入 use case：繞過 VM；用 `HomeViewModel`：範圍不同會建出第二個實例；`ProcessLifecycleOwner`：新依賴 |

### 第 4a 輪：DB、pendingIds、transaction、mapper（2026-11-26 凍結）

```
① pokemon          id PK, name                         ← list API；name 存原始 slug
② pokemon_detail   pokemon_id PK → ①, image_url NULL   ← /pokemon/{id}
③ pokemon_type     (pokemon_id → ②, slot) PK, type_name
④ pokemon_species  pokemon_id PK → ①, evolves_from_id NULL（不設 FK）, description
⑤ capture          id PK (UUID), pokemon_id → ① RESTRICT（有 index）, captured_at
```

不加 `fetched_at`（A 用不到，加 B 時再加）；entity 不加 `@Serializable`（seed 已取消）。

```kotlin
// :domain
object PokemonScope { const val MAX_ID = 151 }   // 只做第一代；list API 的 limit 也用它

// :data
data class PendingWork(
    val needList: Boolean,       // ① 是空的
    val detailIds: List<Int>,    // 在 ① 但沒有 ② 的 id，升冪
    val speciesIds: List<Int>,   // 在 ① 但沒有 ④ 的 id，升冪
)
suspend fun pendingIds(): PendingWork   // 「哪些要抓」的唯一判斷點
```

| crash 時點 | 重開後 |
| --- | --- |
| list 抓取或寫入中 | list 在一個 transaction 寫入，① 仍是空的，重抓 list |
| 第 12 隻 detail 寫入中 | ② 與 ③ 同一個 transaction，兩張表都有或都沒有；重抓第 12 隻 |
| species 寫入中 | ④ 只寫一筆，不會寫一半 |
| 有 detail、沒有 species | 首頁正常顯示；詳細頁 `species == null`，顯示佔位 |

| 欄位 | Mapper 規則 |
| --- | --- |
| id | 取 `url` 最後一段的數字 |
| 顯示名稱 | DB 存 slug；轉 domain 時套對照表：29 Nidoran♀、32 Nidoran♂、83 Farfetch’d、122 Mr. Mime；其他首字母大寫 |
| 圖片 | `sprites.other.official-artwork.front_default`，可為 null |
| type | 依 `slot` 排序 |
| evolves\_from | 從 URL 取 id；轉 domain 時 `takeIf { it.id <= PokemonScope.MAX_ID }` |
| 描述 | 第一筆英文 `flavor_text`；`\f` 的處理對照 mock 實測；沒有英文存 `""`，UI 隱藏 |
| 必要欄位缺少或 type 為空 | 永久性失敗 |

| 決定 | 理由 | 考慮過的替代 |
| --- | --- | --- |
| 範圍規則放 mapper，SQL 只做 LEFT JOIN | 規則看得見；純 JVM 單元測試可測，不依賴 DB 內容 | 只靠 JOIN（① 沒有 >151）：規則隱含；SQL 加 `:maxId`：要跑 Room 才能測；`Int?` 多一條沒人走的分支 |
| DB 存 slug，轉 domain 時才換名稱 | 保留 API 原始值，改規則不必重抓 | 寫入時就轉換 |
| 沒有英文描述存 `""` | 只缺描述，evolves\_from 仍可用 | 當永久性失敗：species 不寫入，Evolves from 消失，詳細頁永遠佔位 |

### 第 4b 輪：sync 執行期與 interceptor（2026-11-26 凍結）

1. `requestSync()`：已有一輪在跑就忽略；否則先把 `status` 設為 `Running`（同步完成，回傳前），再在 application scope 啟動一輪。「檢查是否在跑」與「設為 Running」必須是一個原子操作（`MutableStateFlow.compareAndSet` 或等效），兩個呼叫同時進來也只會啟動一輪。
2. `ConnectivityChecker.isOnline()` 為 false → `status = Finished(round, Offline)`，結束。
3. `pendingIds()`；`needList` 時先抓 list 並寫入，再重算一次。
4. 佇列：先 `detailIds`、再 `speciesIds`，各自依 id 升冪；5 個 worker 取 id → 抓 → 轉換 → transaction 寫入。
5. 全部做完後 `status = Finished(round, result)`，result 優先序 `Offline` > `PartialFailure` > `Success`。

| 失敗類型 | 判斷 | 處理 |
| --- | --- | --- |
| 沒有網路 | `IOException` 且 `isOnline()` 為 false | 取消本輪剩下的請求，結果 `Offline` |
| 暫時性 | 重試用完仍是 `IOException`（有網路）、5xx、429 | 記下來，繼續其他 id，結果 `PartialFailure` |
| 永久性 | 其他 4xx、`SerializationException`、缺必要欄位 | 記 log，從 OkHttp Cache 移除該 URL，繼續；不影響結果 |
| DB 寫入失敗 | `SyncStore.writeXxx` 丟例外 | 記下來，繼續其他 id，不取消本輪；結果 `PartialFailure`；沒寫入的 id 留在 `pendingIds()`，下次回前景重抓 |
| 其他未列出的例外 | 單一 id 處理時丟出上面沒列的例外（例如 NPE、`IllegalStateException`） | 視為暫時性：記 log，繼續其他 id，結果 `PartialFailure` |
| 整輪層級的例外 | `pendingIds()`、`isOnline()` 等丟出未預期例外 | 記 log，本輪結束，結果 `PartialFailure`；`status` 不可停在 `Running` |
| 回報失敗 | `PermanentFailureReporter` 丟例外 | 記 log，不影響結果，繼續 |

取消會經 Retrofit suspend 函式傳到 OkHttp call。process 被 kill 不需額外處理，transaction 保證不留半筆資料。

**Retry interceptor**（共用 OkHttpClient 的 application interceptor，Retrofit 與 Coil 都經過）

- 重試：`IOException`（call 已取消除外）、5xx、429；其他 4xx 不重試。
- 最多 4 次嘗試，等待 1、2、4 秒各 ±50% 抖動。
- 429 照 `Retry-After`，上限 `MAX_RETRY_AFTER = 5` 秒；超過就不等，直接回傳 429，留到下次回前景補抓。
- `Retry-After` 缺少、不是整數或是負數時，照一般退避。
- 重試前必須 `response.close()`。
- 等待抽成可注入的 `Sleeper`；每次嘗試前檢查 `chain.call().isCanceled()`。
- 不判斷網路狀態，沒有外部依賴。
- 保留 OkHttp 預設 `retryOnConnectionFailure = true`；「4 次」是我們的嘗試次數，每次可能含 OkHttp 內建的一次。

| 決定 | 理由 | 考慮過的替代 |
| --- | --- | --- |
| 只有 sync 用 `ConnectivityChecker`（包 `ConnectivityManager`） | interceptor 只管重試、可獨立測試；一開始就離線由 sync 預檢擋下 | interceptor 也注入 checker：多一個依賴；View 塞網路狀態：data 依賴 UI，背景時會過期；以 `UnknownHostException` 判斷：DNS 偶發失敗會誤判 |
| 代價：中途斷網時提示晚約 7 秒 | 每個 worker 重試用完才判定離線 | — |
| `Retry-After` 上限 5 秒 | 與自己最長退避（約 6 秒）一致；避免 worker 被伺服器卡住；無實測依據，寫成常數 | 照等：可能卡很久；忽略：無視伺服器要求 |
| 429 不全域暫停 | 以單一請求為單位處理；與不做 circuit breaker 一致；PokéAPI 限流單位未知，NOTES 寫成假設 | 全域暫停整輪 |

### 第 5 輪：為測試新增的契約（2026-11-26 凍結）

sync 不直接碰 Room，而是經由 `:data` 內部的 `SyncStore`；Room 實作是 `RoomSyncStore`，測試用 in-memory fake。

```kotlin
interface SyncStore {
    suspend fun pendingIds(): PendingWork
    suspend fun writeList(pokemon: List<PokemonEntity>)                                // 一個 transaction
    suspend fun writeDetail(detail: PokemonDetailEntity, types: List<PokemonTypeEntity>) // ②③ 同一個 transaction
    suspend fun writeSpecies(species: PokemonSpeciesEntity)
}

internal fun interface Sleeper { fun sleep(millis: Long) }   // interceptor 的等待；測試只記錄不真的等
```

| 決定 | 理由 | 考慮過的替代 |
| --- | --- | --- |
| 不引入 MockWebServer | interceptor 用真的 `OkHttpClient` 接「腳本回應 interceptor」測，還能追蹤 `ResponseBody` 有沒有 close；sync 用 `FakePokeApi` 記錄收到的 id | MockWebServer：多一個依賴，只多出整條 HTTP 路徑的保證，改由手動看 log 補 |
| 不引入 Robolectric | sync 經 `SyncStore` 跑純 JVM；只有 DB 測試跑 instrumented，且只在 DAO／SQL 改動時跑 | Robolectric：多一個依賴且 SDK 支援會落後；全部 instrumented：每次 Do/Judge 都要先開模擬器 |

## 測試策略

第一批做 ★ 的 10 個，其餘依時間補，沒做的寫進 NOTES。每個測試都針對一個具體會壞的情況，不寫「驗證有呼叫到」的測試。新增依賴只有 JUnit 4、`kotlinx-coroutines-test`、`androidx.test` runner。

| # | 層 | 怎麼跑 | 情境 | 斷言 |
| --- | --- | --- | --- | --- |
| ★D1 | domain | JVM | `groupByType`：Charizard 是 fire/flying | 出現在兩列；type 依字母、列內依 id |
| ★M1 | mapper | JVM | Pikachu evolves\_from = 172；Charmeleon = 4 | 前者 `evolvesFrom == null`；後者是 Charmander |
| M2 | mapper | JVM | 特殊名稱 | 29 → Nidoran♀、83 → Farfetch’d、bulbasaur → Bulbasaur |
| M3 | mapper | JVM | 沒有英文描述 | `description == ""` |
| M4 | mapper | JVM | types 為空 | 丟出永久性失敗 |
| ★B1 | DB | instrumented | 已寫 detail 1–11 | `pendingIds().detailIds` 從 12 開始 |
| ★B2 | DB | instrumented | `writeDetail` 插入 type 時丟例外 | ② 沒有那一筆 |
| ★B3 | DB | instrumented | 已捕捉 Pikachu，再 upsert ① 的 Pikachu | capture 還在（upsert 而非 REPLACE 的證據） |
| ★B4 | DB | instrumented | 兩次捕捉 `captured_at` 相同 | 後插入的在前 |
| B5 | DB | instrumented | 同一 `captureId` 釋放兩次 | 不丟例外 |
| R1 | DB | instrumented | 只有部分寶可夢有 detail | catalog 只含有 detail 的寶可夢 |
| R2 | DB | instrumented | 一隻有 detail 沒 species；一隻沒有 detail | 前者 detail 非 null、species 為 null；後者整筆為 null |
| R3 | DB | instrumented | 前一階段已有 detail | `evolvesFrom` 帶前一階段的名字和圖片 |
| R4 | DB | instrumented | 同一隻捕捉兩次 | 2 筆、id 不同、`captured_at` 等於注入的時間 |
| R5 | DB | instrumented | types 依 slot 2、slot 1 的順序寫入 | catalog 與 detail 的 types 都依 slot 排序（讀回順序由主鍵 `(pokemon_id, slot)` 決定；mapper 漏排序由 `MappersTest` 抓，R5 抓不到） |
| R6 | DB | instrumented | 前一階段在 ① 但還沒有 detail | `evolvesFrom` 有名字、`imageUrl == null`（evolves-from 的 detail 必須是 LEFT JOIN） |
| R7 | DB | instrumented | 同一 `captureId` 經 repository 釋放兩次 | 不丟例外，captures 為空 |
| ★S1 | sync | JVM fake | 第一輪在 11 隻後中斷，再跑第二輪 | `FakePokeApi` 收到的 detail 請求從 12 開始，沒有 1–11 |
| ★S2 | sync | JVM fake | 離線時開始一輪 | `Offline`，0 個請求 |
| S3 | sync | JVM fake | id 7 一直 503 | `PartialFailure`；下一輪只請求 7 |
| S4 | sync | JVM fake | id 7 回 404 | `Success`，其他都寫入 |
| S5 | sync | JVM fake | 連續呼叫兩次 `requestSync()` | list 只被請求一次；結束後 `round == 1` |
| S6 | sync | JVM fake | 呼叫 `requestSync()` 後立刻讀 `status`（不推進 dispatcher） | 已是 `Running`；該輪結束後是 `Finished(1, …)`，再跑一輪是 `Finished(2, …)` |
| ★I1 | interceptor | JVM | 腳本回 503、503、200 | 成功，3 次請求，`Sleeper` 記錄落在 1、2 秒抖動範圍，前兩個 body 都已 close |
| I2 | interceptor | JVM | 連續 4 次 503 | 回傳 503，4 次請求 |
| I3 | interceptor | JVM | 404 | 1 次請求 |
| I4 | interceptor | JVM | 429 配 `Retry-After` 3 與 30 | 3：等 3 秒重試；30：直接回傳 429 |
| ★V1 | VM | JVM fake | catalog 空、status `Finished(1, Offline)` | `Empty(offline = true)`，`message == Offline` |
| V2 | VM | JVM fake | status `Finished(1, Offline)` 產生 message 後送出 `MessageShown`，再讓 catalog 發一次新值 | `message == null` |
| V3 | VM | JVM fake | VM 建立時 status 已是 `Finished(1, Offline)`（VM 晚於該輪結束才建立） | `message == Offline`，不會漏 |
| V5 | VM | JVM fake | 依序 `Finished(1, Offline)`、`Finished(2, Success)`，中間沒送 `MessageShown` | `message == null`，`Empty(offline = false)` |
| V4 | VM | JVM fake | catalog 空；status 依序 `Running` → `Finished(1, Offline)` → `Running` | `Loading` → `Empty(offline = true)` → 仍是 `Empty(offline = true)` |

R1–R7 是 RD-data 在 repository 階段補的（2026-11-27 列入），守 repository 讀取 SQL 的 JOIN 與排序。DB 測試只在 DAO／SQL 改動時跑；其餘一律 `./gradlew test`，Do／Judge 的 subagent 可自行執行。

**手動驗證**（在模擬器上做，結果記進 devlog，作為 NOTES 第 3 題素材）

- sync 進行中 `adb shell am kill`，重開後看 OkHttp log：第一個 detail 請求不是 1 號。
- 首次開 app 時開飛航模式：空狀態文字與 Snackbar；關掉飛航模式、切回 app 後自動載入。
- 列表捲在中段時上方插入新的 type 列：畫面錯點的實際表現。
- sync 過程的 log 中沒有 OkHttp 的連線洩漏警告。

## 已排除的做法與 backlog

| 做法 | 狀態 | 理由 |
| --- | --- | --- |
| 內建資料（seed） | 排除 | 會議：面試方不要，輕微跳動可接受 |
| 打 `/type` 或寫死 type 清單 | 排除 | 規格限三支 API；前 151 隻實際 17 種 type，`/type` 回 21 種 |
| 預建 type 列／佔位 | 排除 | 沒有 detail 就不知道有哪些 type |
| 游標式續抓 | 排除 | 並發 5 時完成的不是連續區段 |
| 中斷後整輪重來 | 排除 | 第一次開 app 沒有快取，會重抓已完成的 |
| 手動重試按鈕 | 排除 | 回前景觸發已涵蓋 |
| `stickyHeader` 放 My Pocket | 排除 | 放在 LazyColumn 外較簡單，不影響 key 錨定 |
| circuit breaker／429 全域暫停 | 排除 | 以單一請求為單位；一輪只有 303 個請求 |
| MockWebServer、Robolectric | 排除 | fake 與拆分測試可達同樣覆蓋 |
| 永久性失敗提示使用者回報 | 排除 | MVP 只記 log；正式產品改接自動回報（Crashlytics non-fatal） |
| 每日更新（B） | backlog | 排在 Bonus 後；切入點 `pendingIds()` 已留。屆時 `writeDetail` 要在同一個 transaction 先刪該 id 的舊 type 再寫（補缺語意下只寫一次，目前不刪） |
| 停在 app 內時網路恢復立刻補抓 | backlog | 需監聽網路或 WorkManager |
| 點不可點的 Evolves from 時提示「資料還在載入，稍後再試」 | backlog | 只改 UI（Snackbar），不動 VM 與契約；前一階段的 detail 抓到後 Flow 更新會自動變成可點。做的時候要改上面的狀態表，Judge mutation 改成「點了不導頁、跳 Snackbar」 |
| list 永久性失敗時提示使用者 | backlog | 目前照「永久性失敗不影響結果」，本輪為 `Success`，首頁空白且沒有提示；PokéAPI 靜態託管、URL 固定，機率低。要提示需在 `SyncResult` 加值（改契約） |

## 待實測

- [ ] 列表捲在中段時，上方插入新的 type 列（含 index 0），key 錨定的實際表現
- [ ] `flavor_text` 的 `\f`、`\n` 要怎麼轉換才符合 mock（對照 Charizard、Charmeleon）
- [ ] sync 進行中 `adb shell am kill`，重開後確認不重抓已完成的
- [ ] 首次開 app 開飛航模式，再關掉並切回 app
- [ ] 離線時按返回離開、立刻重開 app：不應出現上一次的舊 Snackbar（驗證「HomeViewModel 晚於 onStart 建立」的前提）
- [ ] 離線、DB 空時旋轉：不閃 `Loading`；Snackbar 再出現一次屬預期
- [ ] sync log 中沒有 OkHttp 連線洩漏警告
