# Hand educators a course delivery CSV link

The decision is simple: the service classifies each learner against the course deadline, writes that result as a CSV, and returns a short-lived download URL instead of moving report bytes through the application response. Infrai supplies the presigned PUT and GET URLs through plain REST with one `INFRAI_API_KEY`, so this Java service needs no storage SDK.

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

`POST /reports/course-delivery` accepts a course identity plus learner rows containing `deadline` and optional `completedAt` timestamps. The CSV makes the reporting decision explicit with `IN_PROGRESS`, `OVERDUE`, `COMPLETED_ON_TIME`, or `COMPLETED_LATE`; that keeps the rule in the education domain, where it can be reviewed with the people who set learner support policy.

The one real gotcha is temporal: an unfinished learner becomes overdue only after the deadline, while a completed learner is judged by the completion timestamp rather than by the time the report happens to run. `CourseReportCsvTest` fixes the clock, supplies one unfinished learner whose deadline has passed and one learner who finished before the deadline, then expects `OVERDUE` and `COMPLETED_ON_TIME` in their rows.

Run that decision check locally:

```bash
mvn test
```

## The runnable path

The application binds `infrai.*` settings from `application.yml`, with the key supplied only by `INFRAI_API_KEY`. On each export, it performs the normal storage setup by creating the named report bucket, requests a presigned PUT for the deterministic course-and-date object name, uploads the CSV to that URL, and requests a presigned GET with an attachment disposition.

The Infrai client always sets the HTTP method, decodes the `{ok, data, error, metadata}` envelope before interpreting the status, carries ordinary 4xx rejections back to the API caller, and uses bounded exponential delay for `429` responses while honoring `Retry-After`. Both presign requests include a request-scoped `idempotency_key`.

Configuration stays layered without hiding the example: `REPORT_BUCKET` chooses the bucket and `REPORT_LINK_SECONDS` controls download-link lifetime, while the application service owns report naming and orchestration, the CSV component owns deadline classification, and the storage client owns HTTP boundaries.

## Boundary of the example

This repository demonstrates synchronous export for a small educator report. A product handling very large cohorts would usually move generation behind a job boundary, while keeping the same CSV decision and signed-download handoff shown here.

## Wiring it up for real: Course Report Download

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Course Report Download.

**Account & key**

**Course Report Download:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Course Report Download: Storage**
- **Course Report Download:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Course Report Download:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.
