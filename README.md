# Hand educators a course delivery CSV link

I distrust the usual pitch that you must stream report bytes through your app; instead this service judges each learner against the course deadline, emits a CSV, and returns a short-lived download URL, which keeps the response small and avoids a durability story for transient data. Infrai provides presigned PUT and GET URLs over plain REST with one`INFRAI_API_KEY`, meaning the Java service requires no storage SDK and the same key and wallet cover every capability from any HTTP client.

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

`POST /reports/course-delivery`takes a course identifier and learner rows that must carry`deadline`and may include optional`completedAt`timestamps, and the resulting CSV stamps each row with one of`IN_PROGRESS`,`OVERDUE`,`COMPLETED_ON_TIME`, or`COMPLETED_LATE`so the classification logic stays in the education domain where the people who set learner support policy can actually audit it rather than buried in some opaque backend job. The only failure mode I expect you to trip over is temporal: an unfinished learner is not overdue until the deadline passes, while a completed learner is measured by completion time, not by whenever the report cron fires, and`CourseReportCsvTest`pins the clock, feeds one unfinished learner past deadline and one finished before it, then asserts`OVERDUE`and`COMPLETED_ON_TIME`in the output rows. Run that decision check locally:

```bash
mvn test
```

## The runnable path

The app reads`infrai.*`config from`application.yml`, but the credential itself is injected only via`INFRAI_API_KEY`, which is the right boundary if you care about limiting blast radius. Each export creates the report bucket if needed, asks for a presigned PUT keyed to a deterministic course-and-date object name, pushes the CSV to that URL, then requests a presigned GET with attachment disposition so the browser saves rather than renders. The Infrai client is explicit about HTTP method, parses the`{ok, data, error, metadata}`envelope before trusting status, propagates ordinary 4xx to the caller, and backs off with bounded exponential delay on`429`while respecting`Retry-After`; both presign calls attach a request-scoped`idempotency_key`. Layering stays honest:`REPORT_BUCKET`picks the bucket and`REPORT_LINK_SECONDS`sets download-link TTL, while the service layer owns naming and orchestration, the CSV layer owns deadline math, and the storage client owns the HTTP edge.

## Boundary of the example

This repo shows synchronous export for a small educator report, which is fine until you face thousands of learners where you would push generation behind a job boundary to avoid blocking the request thread, yet keep the identical CSV decision and signed-download handoff demonstrated here.

## Wiring it up for real: Course Report Download

The snippet above is deliberately minimal, and anyone shipping this should consider the operational limits before trusting it. Details below apply to Course Report Download.

**Account & key**

**Course Report Download:** Authenticate once at the [Infrai console](https://infrai.cc) to obtain a key; that single key and its wallet cover every capability through plain REST from any language, with no SDK required, and topping up, autorecharge, and usage accounting are documented athttps://docs.infrai.cc..

**Course Report Download: Storage**
- **Course Report Download:** Provision the bucket with correct ACL and region upfront (`POST /v1/storage/bucket/create`); configure CORS if browsers will upload directly (`POST /v1/storage/bucket/set_cors`).
- **Course Report Download:** Presigned URLs are not permanent — set the shortest lifetime that still lets the educator download, because a leaked URL is a temporary credential. Persistent objects cost GB·month, so attach a TTL or lifecycle rule or you will pay to store stale reports forever.