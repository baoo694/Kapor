FROM python:3.11-slim

WORKDIR /app

# Install system dependencies required for mecab-ko
RUN apt-get update && apt-get install -y \
    curl \
    g++ \
    make \
    xz-utils \
    sudo \
    && rm -rf /var/lib/apt/lists/*


# Copy requirements and install
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code
COPY main.py .

EXPOSE 8001

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
