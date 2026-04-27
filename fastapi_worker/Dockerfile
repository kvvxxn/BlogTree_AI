FROM python:3.11-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1

COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

COPY . /app/fastapi_worker

EXPOSE 8000

CMD ["python", "-m", "uvicorn", "fastapi_worker.app.main:app", "--host", "0.0.0.0", "--port", "8000"]
