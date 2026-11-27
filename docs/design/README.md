# Design decisions

| # | Decision | Status |
| --- | --- | --- |
| [0001](0001-module-split-and-layering.md) | Three modules with strict VM → use case → repository layering | Accepted |
| [0002](0002-show-data-as-it-arrives.md) | Show data as it arrives; no bundled seed data | Accepted |
| [0003](0003-sync-fill-missing-and-resume.md) | Sync fills what is missing and resumes by skipping completed work | Accepted |
| [0004](0004-retry-in-application-interceptor.md) | Retry transient failures in a shared OkHttp application interceptor | Accepted |
| [0005](0005-table-split-and-write-semantics.md) | Five tables, one per API resource, written with upsert | Accepted |
| [0006](0006-failure-classification-and-user-messages.md) | Failure classification, sync results, and messages as UI state | Accepted |
| [0007](0007-tests-without-extra-libraries.md) | Contract tests with fakes; no MockWebServer or Robolectric | Accepted, amended by 0008 |
| [0008](0008-compose-ui-tests.md) | Compose UI tests for screen behavior JVM tests cannot see | Accepted |
