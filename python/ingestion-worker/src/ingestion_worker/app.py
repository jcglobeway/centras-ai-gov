from __future__ import annotations

import typer

app = typer.Typer(help="Ingestion worker entrypoint.")


@app.command()
def run() -> None:
    print("ingestion worker scaffold")


def main() -> None:
    app()


if __name__ == "__main__":
    main()

