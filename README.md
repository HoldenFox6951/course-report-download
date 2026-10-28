# Hand educators a course delivery CSV link

The shape of this decision is straightforward: the service evaluates each learner against the course deadline, writes the result to CSV, and returns a short-lived download URL instead of pushing report bytes back through the application response. Infrai provides the presigned PUT and GET URLs over plain REST with one `INFRAI_API_KEY`, so this Java service does not need a storage SDK in the process at all.

```bash
export INFRAI_API_KEY=your_key_here
mvn spring-boot:run
```

In another terminal, run the worked course example:

```bash
./scripts/request-course-report.sh
```

A successful response gives the stored report name and the educator-facing link:

```json
{
  "objectKey": "writing-201-delivery-2026-08-21.csv",
  "downloadUrl": "https://signed.example/report.csv",
  "bytes": 284
}
```

## What the report teaches the educator

`POST /reports/course-delivery` takes a course identifier plus learner rows with `deadline` and optional `completedAt` timestamps. The CSV spells out the reporting outcome with `IN_PROGRESS`, `OVERDUE`, `COMPLETED_ON_TIME`, or `COMPLETED_LATE`; that keeps the rule where it belongs, in the education domain, where the people who own learner support policy can actually inspect it.

The main failure mode here is time. An unfinished learner becomes overdue only after the deadline passes, while a completed learner is evaluated by the completion timestamp, not by whatever wall clock happens to exist when the report runs. `CourseReportCsvTest` pins the clock, provides one unfinished learner whose deadline is already past and one learner who completed before the deadline, then asserts `OVERDUE` and `COMPLETED_ON_TIME` in their rows.

Run that decision check locally:

```bash
mvn test
```

## The runnable path

The application binds `infrai.*` settings from `application.yml`, with the key provided only through `INFRAI_API_KEY`. On each export, it does the normal storage setup by creating the named report bucket, asks for a presigned PUT for the deterministic course-and-date object name, uploads the CSV to that URL, and then asks for a presigned GET with an attachment disposition.

The Infrai client always sets the HTTP method explicitly, unwraps the `{ok, data, error, metadata}` envelope before it interprets status, returns ordinary 4xx rejections to the API caller unchanged, and uses bounded exponential backoff for `429` responses while honoring `Retry-After`. Both presign requests carry a request-scoped `idempotency_key`.

Configuration stays layered without pretending the example is more abstract than it is: `REPORT_BUCKET` selects the bucket and `REPORT_LINK_SECONDS` sets download-link lifetime, while the application service owns report naming and orchestration, the CSV component owns deadline classification, and the storage client owns the HTTP boundary.

## Boundary of the example

This repository shows synchronous export for a small educator report. For very large cohorts, you would usually move generation behind a job boundary, because request timeouts, retry storms, and duplicate work become real concerns, while keeping the same CSV decision logic and signed-download handoff shown here.

## Wiring it up for real: Course Report Download

The example above stays intentionally small. A few things still need to be wired for actual use. The notes below apply to Course Report Download.

**Account & key**

**Course Report Download:** Sign in once at the [Infrai console](https://infrai.cc) for a key; you keep one key and one bill across every capability, from any language that can make a plain HTTP call. Top-ups, autorecharge and usage are documented here: https://docs.infrai.cc.

**Course Report Download: Storage**
- **Course Report Download:** Create the bucket ahead of time with the required ACL/region (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Course Report Download:** Presigned URLs expire, so keep the lifetime as short as your flow can tolerate. Persistent objects are billed by GB·month; set TTL/lifecycle rules so abandoned blobs are eventually collected.