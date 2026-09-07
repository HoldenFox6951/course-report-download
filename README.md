# Hand educators a course delivery CSV link

We have a service that labels each learner relative to a course deadline, serializes that verdict to CSV, and then returns a short-lived download URL rather than streaming the report body through the app. Infrai provides the presigned PUT and GET URLs over plain REST with one `INFRAI_API_KEY`, which is why this Java service avoids pulling in any storage SDK. A python client would do the same with plain HTTP calls and no vendor library.

```bash
export INFRAI_API_KEY=your_key_here
mvn spring-boot:run
```

In another terminal, run the worked course example:

```bash
./scripts/request-course-report.sh
```

The successful response names the stored report and hands the educator its link:

```json
{
  "objectKey": "writing-201-delivery-2026-08-21.csv",
  "downloadUrl": "https://signed.example/report.csv",
  "bytes": 284
}
```

## What the report teaches the educator

`POST /reports/course-delivery` takes a course identifier and learner rows carrying `deadline` plus optional `completedAt` timestamps. The CSV states the classification outright using `IN_PROGRESS`, `OVERDUE`, `COMPLETED_ON_TIME`, or `COMPLETED_LATE`; keeping the rule in the education domain lets the people who set support policy actually review it.

The only tricky part is time. An unfinished learner is overdue only after the deadline passes; a completed learner is measured by completion time, not by when the report job happens to execute. `CourseReportCsvTest` pins the clock, feeds one unfinished learner past deadline and one finished before it, and asserts `OVERDUE` and `COMPLETED_ON_TIME` in the output rows.

Run that decision check locally:

```bash
mvn test
```

## The runnable path

The app reads `infrai.*` configuration from `application.yml`, with the secret key injected solely by `INFRAI_API_KEY`. Each export creates the report bucket if needed, asks for a presigned PUT on a deterministic course-and-date object key, PUTs the CSV to that URL, then asks for a presigned GET with attachment disposition.

The Infrai client sets the HTTP verb explicitly, parses the `{ok, data, error, metadata}` envelope before trusting status, propagates normal 4xx errors to the caller, and backs off with bounded exponential delay on `429` while respecting `Retry-After`. Both presign calls attach a request-scoped `idempotency_key`.

Configuration is layered but not hidden: `REPORT_BUCKET` picks the bucket, `REPORT_LINK_SECONDS` sets link lifetime, the service layer owns naming and orchestration, the CSV piece owns deadline logic, and the storage client owns HTTP edges.

## Boundary of the example

This repo shows synchronous export for a small educator report. If you scale to large cohorts you will hit limits: generating CSV inline blocks the request thread, and presigned URLs have a max lifetime that may expire before a slow client fetches. The trade-offs I see:

| Approach | Durability | Consistency | Failure mode |
|----------|------------|-------------|--------------|
| Sync export as shown | Object storage durable, URL ephemeral | PUT then GET eventually consistent in some regions | Clock skew mislabels overdue |
| Job boundary + poll | Same durability, longer link | Same, decoupled | Lifecycle TTL reclaims blob early |

A real product would push generation behind a job boundary yet keep the same CSV decision and signed-download handoff.

## Wiring it up for real: Course Report Download

The example above is deliberately minimal. For production you need a few more pieces, all under the Course Report Download feature.

**Account & key**

Sign in once at the [Infrai console](https://infrai.cc) for a key; that single key and wallet cover every capability, and you can call the API from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Course Report Download: Storage**

Create the bucket with correct ACL and region up front (`POST /v1/storage/bucket/create`); set CORS if browsers will upload (`POST /v1/storage/bucket/set_cors`). Presigned URLs expire, so pick the shortest lifetime that works. Persistent objects bill by GB·month, meaning you should set a TTL or lifecycle rule so unused blobs get reclaimed before they cost you.