from __future__ import annotations

from ingestion_worker.admin_api_client import AdminApiClient


class FakeResponse:
    def __init__(self, status_code: int, body: dict):
        self.status_code = status_code
        self._body = body

    def json(self) -> dict:
        return self._body

    def raise_for_status(self) -> None:
        if self.status_code >= 400:
            raise RuntimeError(f"http {self.status_code}")


class FakeClient:
    def __init__(self, *_args, **kwargs):
        self.headers = dict(kwargs.get("headers", {}))
        self.login_called = 0
        self.queued_called = 0

    def post(self, url: str, json: dict | None = None, **_kwargs) -> FakeResponse:
        if url == "/admin/auth/login":
            self.login_called += 1
            assert json == {"email": "ops@jcg.com", "password": "pass1234"}
            return FakeResponse(200, {"session": {"token": "new-token"}})
        return FakeResponse(200, {})

    def get(self, url: str, **_kwargs) -> FakeResponse:
        if url == "/admin/ingestion-jobs":
            self.queued_called += 1
            if self.queued_called == 1:
                return FakeResponse(401, {})
            return FakeResponse(
                200,
                {
                    "items": [
                        {
                            "id": "ing_job_test",
                            "crawlSourceId": "crawl_src_test",
                            "organizationId": "org_acc",
                            "serviceId": "svc_acc_chatbot",
                            "jobType": "crawl",
                            "status": "queued",
                            "jobStage": "fetch",
                            "triggerType": "manual",
                            "attemptCount": 1,
                            "errorCode": None,
                        }
                    ]
                },
            )
        return FakeResponse(200, {})

    def close(self) -> None:
        return None


def test_list_queued_jobs_relogin_on_401(monkeypatch):
    monkeypatch.setattr("ingestion_worker.admin_api_client.httpx.Client", FakeClient)

    client = AdminApiClient(
        base_url="http://localhost:8081",
        username="ops@jcg.com",
        password="pass1234",
    )
    jobs = client.list_queued_jobs(page_size=10)

    assert len(jobs) == 1
    assert jobs[0].id == "ing_job_test"
    assert client.session_token == "new-token"
    assert client.client.headers.get("X-Admin-Session-Id") == "new-token"

