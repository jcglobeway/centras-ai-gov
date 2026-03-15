from __future__ import annotations

from fastapi import FastAPI

app = FastAPI(title="rag-orchestrator")


@app.get("/healthz")
def healthz() -> dict[str, str]:
    return {"status": "ok"}


def main() -> None:
    import uvicorn

    uvicorn.run("rag_orchestrator.app:app", host="0.0.0.0", port=8090, reload=False)


if __name__ == "__main__":
    main()

