#!/usr/bin/env bash
set -euo pipefail

curl --fail-with-body --request POST http://localhost:8080/reports/course-delivery \
  --header 'Content-Type: application/json' \
  --data '{
    "courseId": "writing-201",
    "courseTitle": "Writing Strong Explanations",
    "learners": [
      {
        "learnerId": "learner-17",
        "learnerName": "Amina Cole",
        "deadline": "2026-08-20T16:00:00Z",
        "completedAt": null
      },
      {
        "learnerId": "learner-24",
        "learnerName": "Luis Park",
        "deadline": "2026-08-24T16:00:00Z",
        "completedAt": "2026-08-21T09:30:00Z"
      }
    ]
  }'
